import json
import os
import stat
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.parse import urlencode, urlparse
from urllib.request import urlopen

try:
    from zoneinfo import ZoneInfo
except (ImportError, ModuleNotFoundError):
    try:
        from backports.zoneinfo import ZoneInfo
    except (ImportError, ModuleNotFoundError):
        ZoneInfo = None


CONFIG_FILE = str(Path(__file__).parent / "config.json")


def validate_moodle_url(url):
    parsed = urlparse(url)
    if not parsed.scheme:
        raise ValueError("URL missing scheme (use https://)")
    if parsed.scheme != "https":
        raise ValueError("Moodle URL must use HTTPS")
    if not parsed.netloc:
        raise ValueError("Invalid Moodle URL")
    return url.rstrip("/")


def load_config():
    try:
        with open(CONFIG_FILE) as f:
            return json.load(f)
    except FileNotFoundError:
        raise FileNotFoundError(f"{CONFIG_FILE} not found")
    except json.JSONDecodeError as e:
        raise ValueError(f"config.json is not valid JSON: {e}")


def save_config(config):
    config.setdefault("save_password", False)
    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=4)
        f.write("\n")
    os.chmod(CONFIG_FILE, stat.S_IRUSR | stat.S_IWUSR)


def moodle_api(config, endpoint, **params):
    validate_moodle_url(config["moodle_url"])
    params["wstoken"] = config["token"]
    params["moodlewsrestformat"] = "json"
    url = f"{config['moodle_url']}/webservice/rest/server.php?{urlencode(params)}"
    try:
        with urlopen(url, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except URLError as e:
        raise ConnectionError(f"Network error calling {endpoint}: {e}")
    except json.JSONDecodeError:
        raise ValueError(f"Invalid JSON from {endpoint}")


def login(moodle_url, username, password):
    moodle_url = validate_moodle_url(moodle_url)
    url = f"{moodle_url}/login/token.php?{urlencode({'username': username, 'password': password, 'service': 'moodle_mobile_app'})}"
    try:
        with urlopen(url, timeout=30) as resp:
            data = json.loads(resp.read().decode())
    except URLError as e:
        raise ConnectionError(f"Could not reach Moodle: {e}")
    except json.JSONDecodeError:
        raise ValueError("Invalid response from Moodle login server")

    if "error" in data:
        raise PermissionError(f"Login failed: {data['error']}")
    if "token" not in data:
        raise ValueError("Unexpected response from Moodle login (no token)")

    return data["token"]


def strip_html(text):
    import re
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    text = text.replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
    return text.strip()


def system_timezone():
    if ZoneInfo is None:
        return timezone.utc
    try:
        with open("/etc/timezone") as f:
            return ZoneInfo(f.read().strip())
    except Exception:
        pass
    try:
        import os
        link = os.path.realpath("/etc/localtime")
        if "zoneinfo" in link:
            name = link.split("zoneinfo/", 1)[1]
            return ZoneInfo(name)
    except Exception:
        pass
    return timezone.utc


def resolve_tz(config):
    if config.get("timezone"):
        try:
            if ZoneInfo is not None:
                return ZoneInfo(config["timezone"])
        except Exception:
            pass
    return system_timezone()


def fmt_ics_dt(ts, tz):
    return datetime.fromtimestamp(ts, tz).strftime("%Y%m%dT%H%M%S")


def escape_ics(text):
    return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")


def fetch_events(config):
    days_back = config.get("fetch_days_back", 7)
    limitnum = config.get("fetch_limit", 100)
    timesort = int((datetime.now(timezone.utc) - timedelta(days=days_back)).timestamp())
    data = moodle_api(
        config,
        "core_calendar_get_action_events_by_timesort",
        timesortfrom=timesort,
        limitnum=limitnum,
    )
    events = []
    if data and "events" in data:
        for ev in data["events"]:
            events.append({
                "id": ev.get("id"),
                "name": ev.get("name", "Untitled"),
                "description": strip_html(ev.get("description", {}).get("text", "")),
                "timestart": ev.get("timestart", 0),
                "timeduration": ev.get("timeduration", 0),
                "eventtype": ev.get("eventtype", ""),
                "url": ev.get("url", ""),
                "course": ev.get("course", {}).get("shortname", ""),
                "modname": ev.get("modulename", ""),
            })

    assign_data = moodle_api(config, "mod_assign_get_assignments")
    if assign_data and "courses" in assign_data:
        for course in assign_data["courses"]:
            shortname = course.get("shortname", "")
            for assign in course.get("assignments", []):
                duedate = assign.get("duedate", 0)
                if duedate <= timesort:
                    continue
                all_ids = {e["id"] for e in events}
                ev = {
                    "id": f"assign_{assign['cmid']}",
                    "name": assign.get("name", "Untitled"),
                    "description": strip_html(assign.get("intro", "")),
                    "timestart": duedate,
                    "timeduration": 0,
                    "eventtype": "assign",
                    "url": assign.get("url", ""),
                    "course": shortname,
                    "modname": "assign",
                }
                if ev["id"] not in all_ids:
                    events.append(ev)

    events.sort(key=lambda e: e["timestart"])
    return events


def generate_ics(config, events):
    tz = resolve_tz(config)
    lines = [
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Moodle Calendar Bridge//EN",
        "X-WR-CALNAME:Moodle Calendar",
    ]
    for ev in events:
        uid = f"{ev['id']}@moodle-calendar-bridge"
        dtstart = fmt_ics_dt(ev["timestart"], tz)
        duration_sec = ev.get("timeduration", 0)
        if duration_sec > 0:
            end_ts = ev["timestart"] + duration_sec
        else:
            end_ts = ev["timestart"] + 3600
        dtend = fmt_ics_dt(end_ts, tz)
        summary = escape_ics(ev["name"])
        desc = escape_ics(ev.get("description", ""))
        if ev.get("course"):
            desc = f"{desc}\\n\\nCourse: {ev['course']}" if desc else f"Course: {ev['course']}"
        if ev.get("url"):
            desc = f"{desc}\\nURL: {ev['url']}"
        lines.append("BEGIN:VEVENT")
        lines.append(f"UID:{uid}")
        lines.append(f"DTSTART:{dtstart}")
        lines.append(f"DTEND:{dtend}")
        lines.append(f"SUMMARY:{summary}")
        lines.append(f"DESCRIPTION:{desc}")
        lines.append("END:VEVENT")
    lines.append("END:VCALENDAR")
    return "\r\n".join(lines) + "\r\n"


def generate_logseq(config, events):
    tz = resolve_tz(config)
    out_dir = Path(config.get("paths", {}).get("logseq", "output/logseq/"))
    out_dir.mkdir(parents=True, exist_ok=True)
    for ev in events:
        dt = datetime.fromtimestamp(ev["timestart"], tz)
        title = ev["name"]
        safe_name = "".join(c if c.isalnum() or c in " -_" else "_" for c in title).strip()
        if not safe_name:
            safe_name = f"event-{ev['id']}"
        deadline_str = dt.strftime("%Y-%m-%d %a %H:%M")
        content = f"- {title}\n  DEADLINE: <{deadline_str}>\n"
        if ev.get("course"):
            content += f"  course:: {ev['course']}\n"
        if ev.get("url"):
            content += f"  url:: {ev['url']}\n"
        if ev.get("description"):
            content += f"  description:: {ev['description']}\n"
        (out_dir / f"{safe_name}.md").write_text(content)


def generate_obsidian(config, events):
    tz = resolve_tz(config)
    out_dir = Path(config.get("paths", {}).get("obsidian", "output/obsidian/"))
    out_dir.mkdir(parents=True, exist_ok=True)
    for ev in events:
        dt = datetime.fromtimestamp(ev["timestart"], tz)
        date_str = dt.strftime("%Y-%m-%d")
        due_str = dt.strftime("%Y-%m-%d %H:%M")
        frontmatter = {"due": due_str, "title": ev["name"]}
        if ev.get("course"):
            frontmatter["course"] = ev["course"]
        if ev.get("url"):
            frontmatter["url"] = ev["url"]
        yaml_lines = ["---"]
        for k, v in frontmatter.items():
            yaml_lines.append(f"{k}: {v}")
        yaml_lines.append("---")
        yaml_lines.append("")
        if ev.get("description"):
            yaml_lines.append(ev["description"])
        yaml_lines.append("")
        (out_dir / f"{date_str}.md").write_text("\n".join(yaml_lines))


CACHE_DIR = "output"
CACHE_FILE = ".event_cache.json"


def read_cache():
    try:
        with open(Path(CACHE_DIR) / CACHE_FILE) as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def write_cache(cache):
    Path(CACHE_DIR).mkdir(parents=True, exist_ok=True)
    with open(Path(CACHE_DIR) / CACHE_FILE, "w") as f:
        json.dump(cache, f)
        f.write("\n")


def diff_events(events, cache):
    new_ids = set()
    changed_ids = set()
    event_map = {}
    for ev in events:
        eid = str(ev["id"])
        event_map[eid] = ev
        if eid not in cache:
            new_ids.add(eid)
        elif cache[eid] != ev["timestart"]:
            changed_ids.add(eid)

    new_events = [event_map[eid] for eid in new_ids]
    changed_events = [event_map[eid] for eid in changed_ids]
    new_or_changed = new_events + changed_events
    new_or_changed.sort(key=lambda e: e["timestart"])
    return new_events, changed_events, new_or_changed


def build_cache(events):
    return {str(ev["id"]): ev["timestart"] for ev in events}


def run_with_config(config):
    logs = []
    def log(msg):
        logs.append(msg)
        print(msg)

    log("Fetching events from Moodle...")
    events = fetch_events(config)
    if not events:
        log("No events found.")
        return logs

    log(f"Found {len(events)} event(s).")

    cache = read_cache()
    new_ev, changed_ev, new_or_changed = diff_events(events, cache)
    if new_ev:
        log(f"  {len(new_ev)} new, {len(changed_ev)} changed, {len(events) - len(new_or_changed)} unchanged")

    paths = config.get("paths", {})
    output = config.get("output", {})

    if output.get("ics", True):
        ics_dir = Path(paths.get("ics", "output/calendar.ics")).parent
        ics_dir.mkdir(parents=True, exist_ok=True)

        all_ics = generate_ics(config, events)
        (ics_dir / "calendar.ics").write_text(all_ics)
        log(f"  ICS (all)     -> {ics_dir / 'calendar.ics'}")

        if new_or_changed:
            new_ics = generate_ics(config, new_or_changed)
            (ics_dir / "calendar_new.ics").write_text(new_ics)
            log(f"  ICS (new)     -> {ics_dir / 'calendar_new.ics'}  ({len(new_or_changed)} events)")

    if output.get("logseq", True):
        generate_logseq(config, events)
        log(f"  Logseq        -> {paths.get('logseq', 'output/logseq/')}")

    if output.get("obsidian", True):
        generate_obsidian(config, events)
        log(f"  Obsidian      -> {paths.get('obsidian', 'output/obsidian/')}")

    write_cache(build_cache(events))
    return logs


def cli_login(config=None):
    import getpass
    if config is None:
        try:
            config = load_config()
        except FileNotFoundError:
            config = {"moodle_url": input("Moodle URL: ").strip()}
        except ValueError as e:
            print(f"Error: {e}")
            sys.exit(1)

    url = config.get("moodle_url", "").rstrip("/")
    while True:
        try:
            if not url:
                url = input("Moodle URL: ").strip().rstrip("/")
            validate_moodle_url(url)
            break
        except ValueError:
            url = input("Moodle URL: ").strip().rstrip("/")

    print(f"Logging into {url} ...")
    username = input("Username: ").strip()
    password = getpass.getpass("Password: ")

    token = login(url, username, password)

    config["moodle_url"] = url
    config["token"] = token
    config["username"] = username
    save = input("Save password to config.json? (y/N): ").strip().lower()
    if save == "y":
        config["password"] = password
        config["save_password"] = True
    else:
        config.pop("password", None)
        config["save_password"] = False
    save_config(config)
    print("Token saved to config.json")
    return config


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Moodle Calendar Bridge")
    parser.add_argument("--moodle-url", help="Moodle instance URL (overrides config.json)")
    parser.add_argument("--login", action="store_true", help="Authenticate with Moodle username/password")
    args = parser.parse_args()

    if args.moodle_url:
        try:
            validate_moodle_url(args.moodle_url)
        except ValueError as e:
            print(f"Error: {e}")
            sys.exit(1)

    if args.login:
        if args.moodle_url:
            config = {"moodle_url": args.moodle_url.rstrip("/")}
            cli_login(config)
        else:
            cli_login()
        return

    try:
        config = load_config()
        if args.moodle_url:
            config["moodle_url"] = args.moodle_url.rstrip("/")
        if not config.get("token"):
            print("Not authenticated. Run with --login to log in.")
            print(f"  python3 moodle_cal.py --login")
            sys.exit(1)
        run_with_config(config)
    except (FileNotFoundError, ValueError, ConnectionError, PermissionError) as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
