#!/usr/bin/env python3
"""Moodle Calendar Bridge — Flet GUI.

Requires: pip install flet
Run with:  flet run moodle_cal_flet.py
Build APK: flet build apk --org "com.yourorg" --product "Moodle Calendar"
"""

import asyncio
import json
import subprocess
import threading
import zoneinfo
from pathlib import Path

import moodle_cal as core

import flet as ft

# Placeholder shown in the password field when the password is stored
# in the OS keychain but not visible in the text field.
_SAVED = "••••••••"


def _native_save_file():
    try:
        r = subprocess.run(
            ["zenity", "--file-selection", "--save", "--filename=calendar.ics",
             "--title=Save ICS file"],
            capture_output=True, text=True, timeout=60,
        )
        if r.returncode == 0:
            return r.stdout.strip()
    except FileNotFoundError:
        pass
    return None


def _native_pick_directory(title="Select directory"):
    try:
        r = subprocess.run(
            ["zenity", "--file-selection", "--directory", f"--title={title}"],
            capture_output=True, text=True, timeout=60,
        )
        if r.returncode == 0:
            return r.stdout.strip()
    except FileNotFoundError:
        pass
    return None

with open(Path(__file__).parent / "langs.json") as _f:
    LANG = json.load(_f)

THEME_MAP = {
    "system": ft.ThemeMode.SYSTEM,
    "light": ft.ThemeMode.LIGHT,
    "dark": ft.ThemeMode.DARK,
}


async def main(page: ft.Page):
    page.padding = 20
    page.spacing = 0

    config = {}
    log_controls = []
    _txt = {}

    def _tr(key):
        lang = config.get("language", "en")
        return LANG.get(lang, LANG["en"]).get(key, key)

    def apply_language():
        page.title = _tr("app_title")
        for key, ctrl in _txt.items():
            ctrl.value = _tr(key)
        url_field.label = _tr("moodle_url")
        username_field.label = _tr("username")
        password_field.label = _tr("password")
        timezone_field.label = _tr("timezone")
        ics_path_field.label = _tr("ics_file")
        logseq_path_field.label = _tr("logseq_dir")
        obsidian_path_field.label = _tr("obsidian_dir")
        save_pw_check.label = _tr("store_pw")
        ics_check.label = _tr("ics_label")
        logseq_check.label = _tr("logseq_label")
        obsidian_check.label = _tr("obsidian_label")
        days_back_field.label = _tr("fetch_days")
        limit_field.label = _tr("fetch_limit")
        fetch_btn.text = _tr("fetch")
        share_btn.text = _tr("share_ics")
        tab_conn.label = _tr("connection")
        tab_out.label = _tr("output")
        status_text.value = _tr("ready")
        page.update()

    def apply_theme():
        key = config.get("theme_mode", "system")
        page.theme_mode = THEME_MAP.get(key, ft.ThemeMode.SYSTEM)
        page.update()

    def ui_to_config():
        config["moodle_url"] = url_field.value.strip()
        config["username"] = username_field.value.strip()
        config["timezone"] = (timezone_field.value or "").strip()
        if save_pw_check.value:
            val = password_field.value
            if val and val != _SAVED:
                config["password"] = val
            else:
                config.pop("password", None)
        else:
            config.pop("password", None)
        config["save_password"] = save_pw_check.value
        config["output"] = {
            "ics": ics_check.value,
            "logseq": logseq_check.value,
            "obsidian": obsidian_check.value,
        }
        config["paths"] = {
            "ics": ics_path_field.value.strip() or "output/calendar.ics",
            "logseq": logseq_path_field.value.strip() or "output/logseq/",
            "obsidian": obsidian_path_field.value.strip() or "output/obsidian/",
        }
        try:
            config["fetch_days_back"] = int(days_back_field.value.strip())
        except (ValueError, AttributeError):
            config.pop("fetch_days_back", None)
        try:
            config["fetch_limit"] = int(limit_field.value.strip())
        except (ValueError, AttributeError):
            config.pop("fetch_limit", None)

    def _border():
        return ft.border.Border(
            left=ft.border.BorderSide(1, ft.Colors.OUTLINE),
            top=ft.border.BorderSide(1, ft.Colors.OUTLINE),
            right=ft.border.BorderSide(1, ft.Colors.OUTLINE),
            bottom=ft.border.BorderSide(1, ft.Colors.OUTLINE),
        )

    def log(msg):
        log_controls.append(ft.Text(msg, size=11, font_family="monospace"))
        log_view.controls = log_controls
        if hasattr(page, "update"):
            page.update()

    def clear_log():
        log_controls.clear()
        log_view.controls = log_controls
        page.update()

    def validate_url(url):
        try:
            core.validate_moodle_url(url)
            return None
        except ValueError as e:
            return str(e)

    def _url_blur(e):
        url_field.error_text = validate_url(url_field.value.strip())
        page.update()

    # ── Load config ───────────────────────────────────────────────
    try:
        config = core.load_config()
    except (FileNotFoundError, ValueError):
        config = {
            "moodle_url": "https://",
            "token": "",
            "username": "",
            "password": "",
            "timezone": "",
            "language": "en",
            "theme_mode": "system",
            "save_password": False,
            "output": {"ics": True, "logseq": True, "obsidian": True},
            "paths": {
                "ics": "output/calendar.ics",
                "logseq": "output/logseq/",
                "obsidian": "output/obsidian/",
            },
        }
    config.setdefault("language", "en")
    config.setdefault("theme_mode", "system")
    config.setdefault("save_password", False)

    # ── Platform detection ─────────────────────────────────────────
    is_mobile = page.platform in (ft.PagePlatform.ANDROID, ft.PagePlatform.IOS)

    # ── Settings dialog ───────────────────────────────────────────
    def open_settings(e):
        theme_val = config.get("theme_mode", "system")
        lang_val = config.get("language", "en")
        theme_dd = ft.Dropdown(
            options=[
                ft.DropdownOption(key="system", text=_tr("theme_system")),
                ft.DropdownOption(key="light", text=_tr("theme_light")),
                ft.DropdownOption(key="dark", text=_tr("theme_dark")),
            ],
            value=theme_val,
            expand=True,
        )
        lang_dd = ft.Dropdown(
            options=[
                ft.DropdownOption(key="en", text="English"),
                ft.DropdownOption(key="es", text="Espa\u00f1ol"),
            ],
            value=lang_val,
            expand=True,
        )

        def clear_creds(ev):
            config.pop("token", None)
            config.pop("password", None)
            password_field.value = ""
            core.delete_password(
                config.get("moodle_url", ""),
                config.get("username", ""),
            )
            core.save_config(config)
            dlg.open = False
            page.update()

        def save_and_close(ev):
            config["theme_mode"] = theme_dd.value
            config["language"] = lang_dd.value
            core.save_config(config)
            page.theme_mode = THEME_MAP.get(config["theme_mode"], ft.ThemeMode.SYSTEM)
            apply_language()
            dlg.open = False
            page.update()

        dlg = ft.AlertDialog(
            title=ft.Text(_tr("settings")),
            content=ft.Column(
                [
                    ft.Text(_tr("theme"), size=12),
                    theme_dd,
                    ft.Divider(height=8, color=ft.Colors.TRANSPARENT),
                    ft.Text(_tr("language"), size=12),
                    lang_dd,
                    ft.Divider(height=8, color=ft.Colors.TRANSPARENT),
                    ft.TextButton(
                        _tr("clear_creds"),
                        on_click=clear_creds,
                        style=ft.ButtonStyle(color=ft.Colors.RED),
                    ),
                ],
                width=320,
                spacing=4,
            ),
            actions=[ft.TextButton(_tr("accept"), on_click=save_and_close)],
        )
        page.show_dialog(dlg)

    # ── Connection tab ───────────────────────────────────────────
    url_field = ft.TextField(
        label=_tr("moodle_url"),
        value=config.get("moodle_url", ""),
        expand=True,
        on_blur=_url_blur,
    )
    username_field = ft.TextField(
        label=_tr("username"),
        value=config.get("username", ""),
        expand=True,
    )
    _pw_saved = (
        config.get("save_password")
        and not config.get("password")
        and core.get_password(config.get("moodle_url", ""), config.get("username", ""))
    )
    password_field = ft.TextField(
        label=_tr("password"),
        value=config.get("password") or (_SAVED if _pw_saved else ""),
        password=True,
        expand=True,
    )

    timezone_field = ft.TextField(
        label=_tr("timezone"),
        value=config.get("timezone", ""),
        expand=True,
    )

    conn_tip = ft.Text(
        _tr("pw_tip"),
        size=10,
        color=ft.Colors.GREY,
    )
    _txt["pw_tip"] = conn_tip

    conn_tab = ft.ListView(
        [
            ft.Container(
                content=ft.Column(
                    [
                        url_field,
                        username_field,
                        password_field,
                        ft.Divider(height=4, color=ft.Colors.TRANSPARENT),
                        timezone_field,
                    ],
                    spacing=8,
                ),
                border=_border(),
                border_radius=8,
                padding=16,
            ),
            conn_tip,
        ],
        spacing=8,
    )

    # ── Output tab ───────────────────────────────────────────────
    ics_check = ft.Checkbox(label=_tr("ics_label"), value=config.get("output", {}).get("ics", True))
    logseq_check = ft.Checkbox(label=_tr("logseq_label"), value=config.get("output", {}).get("logseq", True))
    obsidian_check = ft.Checkbox(label=_tr("obsidian_label"), value=config.get("output", {}).get("obsidian", True))

    paths = config.get("paths", {})

    ics_path_field = ft.TextField(
        label=_tr("ics_file"),
        value=paths.get("ics", "output/calendar.ics"),
        expand=True,
    )
    logseq_path_field = ft.TextField(
        label=_tr("logseq_dir"),
        value=paths.get("logseq", "output/logseq/"),
        expand=True,
    )
    obsidian_path_field = ft.TextField(
        label=_tr("obsidian_dir"),
        value=paths.get("obsidian", "output/obsidian/"),
        expand=True,
    )

    async def _browse_ics(e):
        path = await asyncio.to_thread(_native_save_file)
        if path:
            ics_path_field.value = path
            page.update()

    async def _browse_logseq(e):
        path = await asyncio.to_thread(_native_pick_directory, "Select Logseq directory")
        if path:
            logseq_path_field.value = path
            page.update()

    async def _browse_obsidian(e):
        path = await asyncio.to_thread(_native_pick_directory, "Select Obsidian directory")
        if path:
            obsidian_path_field.value = path
            page.update()

    def _path_row(field, browse_handler):
        if is_mobile:
            return field
        btn = ft.IconButton(
            icon=ft.Icons.FOLDER_OPEN,
            tooltip=_tr("browse"),
            on_click=browse_handler,
        )
        return ft.Row([field, btn], spacing=4, vertical_alignment=ft.CrossAxisAlignment.CENTER)

    fmt_heading = ft.Text(_tr("formats"), weight=ft.FontWeight.BOLD, size=13)
    _txt["formats"] = fmt_heading
    paths_heading = ft.Text(_tr("paths"), weight=ft.FontWeight.BOLD, size=13)
    _txt["paths"] = paths_heading

    # ── Fetch params ────────────────────────────────────────────
    fetch_heading = ft.Text(_tr("fetch_params"), weight=ft.FontWeight.BOLD, size=13)
    _txt["fetch_params"] = fetch_heading

    days_back_field = ft.TextField(
        label=_tr("fetch_days"),
        value=str(config.get("fetch_days_back", 7)),
        expand=True,
        keyboard_type=ft.KeyboardType.NUMBER,
    )
    limit_field = ft.TextField(
        label=_tr("fetch_limit"),
        value=str(config.get("fetch_limit", 100)),
        expand=True,
        keyboard_type=ft.KeyboardType.NUMBER,
    )

    out_tab = ft.ListView(
        [
            ft.Container(
                content=ft.Column(
                    [
                        fmt_heading,
                        ics_check,
                        logseq_check,
                        obsidian_check,
                    ],
                    spacing=4,
                ),
                border=_border(),
                border_radius=8,
                padding=16,
            ),
            ft.Container(
                content=ft.Column(
                    [
                        paths_heading,
                        _path_row(ics_path_field, _browse_ics),
                        _path_row(logseq_path_field, _browse_logseq),
                        _path_row(obsidian_path_field, _browse_obsidian),
                    ],
                    spacing=8,
                ),
                border=_border(),
                border_radius=8,
                padding=16,
            ),
            ft.Container(
                content=ft.Column(
                    [
                        fetch_heading,
                        days_back_field,
                        limit_field,
                    ],
                    spacing=8,
                ),
                border=_border(),
                border_radius=8,
                padding=16,
            ),
        ],
        spacing=12,
    )

    # ── Tabs ─────────────────────────────────────────────────────
    tab_conn = ft.Tab(label=_tr("connection"))
    tab_out = ft.Tab(label=_tr("output"))

    tabs = ft.Tabs(
        length=2,
        selected_index=0,
        expand=3,
        content=ft.Column(
            expand=True,
            controls=[
                ft.TabBar(tabs=[tab_conn, tab_out]),
                ft.TabBarView(
                    expand=True,
                    controls=[conn_tab, out_tab],
                ),
            ],
        ),
    )

    # ── Header with settings button ──────────────────────────────
    header_title = ft.Text(_tr("app_title"), size=18, weight=ft.FontWeight.BOLD)
    header_subtitle = ft.Text(_tr("subtitle"), size=11, color=ft.Colors.GREY)
    _txt["app_title"] = header_title
    _txt["subtitle"] = header_subtitle

    header_row = ft.Row(
        [
            ft.Column(
                [
                    header_title,
                    header_subtitle,
                ],
                expand=True,
                spacing=2,
            ),
            ft.IconButton(
                icon=ft.Icons.SETTINGS,
                tooltip=_tr("settings"),
                on_click=open_settings,
            ),
        ],
        spacing=8,
        vertical_alignment=ft.CrossAxisAlignment.START,
    )

    # ── Store password checkbox ──────────────────────────────────
    save_pw_check = ft.Checkbox(
        label=_tr("store_pw"),
        value=config.get("save_password", False),
    )

    # ── Fetch button ─────────────────────────────────────────────
    def do_fetch(retried=False):
        ui_to_config()
        if core.HAS_KEYRING:
            config.pop("password", None)
        core.save_config(config)

        if not config.get("token"):
            username = config.get("username", "")
            pw_field = password_field.value
            if pw_field == _SAVED:
                pw_field = ""
            password = pw_field or core.resolve_password(config)
            if not username or not password:
                status_text.value = _tr("need_creds")
                page.update()
                page.show_dialog(ft.AlertDialog(
                    title=ft.Text(_tr("error")),
                    content=ft.Text(_tr("cred_error")),
                ))
                return
            log(_tr("no_cached_token"))
            try:
                token = core.login(config["moodle_url"], username, password)
                config["token"] = token
                if save_pw_check.value:
                    core.store_password(
                        config["moodle_url"],
                        config["username"],
                        password,
                    )
                    config.pop("password", None)
                    password_field.value = _SAVED
                else:
                    config.pop("password", None)
                    password_field.value = ""
                    core.delete_password(
                        config.get("moodle_url", ""),
                        config.get("username", ""),
                    )
                ui_to_config()
                if core.HAS_KEYRING and config.get("save_password"):
                    config.pop("password", None)
                core.save_config(config)
                log(_tr("login_success"))
            except Exception as e:
                status_text.value = _tr("login_failed")
                page.update()
                page.show_dialog(ft.AlertDialog(
                    title=ft.Text(_tr("login_failed")),
                    content=ft.Text(str(e)),
                ))
                return

        status_text.value = _tr("fetching")
        fetch_btn.disabled = True
        clear_log()
        log(_tr("fetching_events"))
        page.update()

        def worker():
            try:
                logs = core.run_with_config(config)
                page.run_thread(lambda: on_fetch_done(logs, None))
            except PermissionError as exc:
                config.pop("token", None)
                core.save_config(config)
                if retried:
                    msg = _tr("login_failed_again").format(exc)
                    page.run_thread(lambda: on_fetch_done([], msg))
                else:
                    log(_tr("token_expired"))
                    page.run_thread(lambda: do_fetch(True))
            except Exception as exc:
                msg = str(exc)
                page.run_thread(lambda: on_fetch_done([], msg))

        threading.Thread(target=worker, daemon=True).start()

    def on_fetch_done(logs, error):
        fetch_btn.disabled = False
        if error:
            status_text.value = _tr("error")
            log(f"{_tr('error')}: {error}")
            page.update()
            page.show_dialog(ft.AlertDialog(
                title=ft.Text(_tr("error")),
                content=ft.Text(error),
            ))
        else:
            status_text.value = _tr("done")
            for msg in logs:
                log(msg)
            log(_tr("done"))
        page.update()

    def on_fetch(e):
        clear_log()
        do_fetch()

    fetch_btn = ft.FilledButton(
        _tr("fetch"),
        on_click=on_fetch,
        expand=True,
        height=40,
    )

    # ── Share ICS button ─────────────────────────────────────────
    def on_share(e):
        ics_path = config.get("paths", {}).get("ics", "output/calendar.ics")
        full = Path(ics_path).resolve()
        if not full.exists():
            page.show_dialog(ft.AlertDialog(
                title=ft.Text(_tr("error")),
                content=ft.Text(_tr("no_ics")),
            ))
            return
        if is_mobile:
            page.set_clipboard(str(full))
            page.launch_url(f"file://{full}")
            page.show_dialog(ft.AlertDialog(
                title=ft.Text(_tr("share_ics")),
                content=ft.Text(_tr("share_mobile_hint").format(str(full))),
            ))
        else:
            page.launch_url(f"file://{full}")
            page.show_dialog(ft.AlertDialog(
                title=ft.Text(_tr("share_ics")),
                content=ft.Text(_tr("share_desktop_hint").format(str(full))),
            ))

    share_btn = ft.OutlinedButton(
        _tr("share_ics"),
        icon=ft.Icons.SHARE,
        on_click=on_share,
        expand=True,
        height=40,
    )

    # ── Button row ───────────────────────────────────────────────
    btn_row = ft.Row(
        [fetch_btn, share_btn],
        spacing=10,
        alignment=ft.MainAxisAlignment.CENTER,
    )

    # ── Log area ─────────────────────────────────────────────────
    log_view = ft.ListView(
        controls=[],
        expand=True,
        spacing=2,
    )

    log_container = ft.Container(
        content=log_view,
        border=_border(),
        border_radius=8,
        padding=10,
        expand=2,
    )

    log_label = ft.Text(_tr("log"), size=12)
    _txt["log"] = log_label

    status_text = ft.Text(_tr("ready"), size=11, color=ft.Colors.GREY)
    _txt["ready"] = status_text

    # ── Assemble page ────────────────────────────────────────────
    page.title = _tr("app_title")

    page.add(
        ft.Column(
            [
                header_row,
                ft.Divider(height=8, color=ft.Colors.TRANSPARENT),
                tabs,
                ft.Divider(height=8, color=ft.Colors.TRANSPARENT),
                save_pw_check,
                btn_row,
                ft.Divider(height=4, color=ft.Colors.TRANSPARENT),
                log_label,
                log_container,
                status_text,
            ],
            spacing=0,
            expand=True,
        )
    )

    apply_theme()
    apply_language()


if __name__ == "__main__":
    import os
    import sys

    filt = ["gtk_window_get_position", "gtk_window_get_size",
            "FlutterEngineRemoveView", "FlBinaryMessenger",
            "Failed to cleanup", "Attempted to set message"]

    class _StderrFilter:
        def __init__(self, orig):
            self.orig = orig
        def write(self, text):
            if not any(m in text for m in filt):
                self.orig.write(text)
        def flush(self):
            self.orig.flush()

    sys.stderr = _StderrFilter(sys.stderr)
    ft.run(main)
