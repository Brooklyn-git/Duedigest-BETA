#!/usr/bin/env python3
"""Moodle Calendar Bridge — Flet GUI.

Requires: pip install flet
Run with:  flet run moodle_cal_flet.py
Build APK: flet build apk --org "com.yourorg" --product "Moodle Calendar"
"""

import json
import threading
import zoneinfo
from pathlib import Path

import moodle_cal as core

import flet as ft

with open(Path(__file__).parent / "langs.json") as _f:
    LANG = json.load(_f)

THEME_MAP = {
    "system": ft.ThemeMode.SYSTEM,
    "light": ft.ThemeMode.LIGHT,
    "dark": ft.ThemeMode.DARK,
}


def main(page: ft.Page):
    page.window.width = 560
    page.window.height = 620
    page.window.min_width = 480
    page.window.min_height = 500
    page.padding = 20
    page.spacing = 0

    config = {}
    log_controls = []

    # translatable controls registry — Text controls only (have .value)
    _txt = {}

    def _tr(key):
        lang = config.get("language", "en")
        return LANG.get(lang, LANG["en"]).get(key, key)

    def apply_language():
        page.title = _tr("app_title")
        for key, ctrl in _txt.items():
            ctrl.value = _tr(key)
        tab_conn.label = _tr("connection")
        tab_out.label = _tr("output")
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
        fetch_btn.content = _tr("fetch")
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
            config["password"] = password_field.value
        else:
            config.pop("password", None)
        config["save_password"] = save_pw_check.value
        config["output"] = {
            "ics": ics_check.value,
            "logseq": logseq_check.value,
            "obsidian": obsidian_check.value,
        }
        config["paths"] = {
            "ics": ics_path_field.value.strip(),
            "logseq": logseq_path_field.value.strip(),
            "obsidian": obsidian_path_field.value.strip(),
        }

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

    # ── Load config ───────────────────────────────────────────────
    try:
        config = core.load_config()
    except (FileNotFoundError, ValueError):
        config = {
            "moodle_url": "",
            "token": "",
            "username": "",
            "password": "",
            "timezone": "",
            "language": "en",
            "theme_mode": "system",
            "save_password": True,
            "output": {"ics": True, "logseq": True, "obsidian": True},
            "paths": {
                "ics": "output/calendar.ics",
                "logseq": "output/logseq/",
                "obsidian": "output/obsidian/",
            },
        }
    config.setdefault("language", "en")
    config.setdefault("theme_mode", "system")

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
            width=300,
        )
        lang_dd = ft.Dropdown(
            options=[
                ft.DropdownOption(key="en", text="English"),
                ft.DropdownOption(key="es", text="Espa\u00f1ol"),
            ],
            value=lang_val,
            width=300,
        )
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
        width=400,
    )
    username_field = ft.TextField(
        label=_tr("username"),
        value=config.get("username", ""),
        width=400,
    )
    password_field = ft.TextField(
        label=_tr("password"),
        value=config.get("password", "") if config.get("save_password") else "",
        password=True,
        width=400,
    )

    tz_list = sorted(zoneinfo.available_timezones())
    tz_options = [ft.DropdownOption(key=z, text=z) for z in tz_list]

    timezone_field = ft.Dropdown(
        label=_tr("timezone"),
        options=tz_options,
        value=config.get("timezone", "") or None,
        width=400,
        enable_search=True,
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
        width=400,
    )
    logseq_path_field = ft.TextField(
        label=_tr("logseq_dir"),
        value=paths.get("logseq", "output/logseq/"),
        width=400,
    )
    obsidian_path_field = ft.TextField(
        label=_tr("obsidian_dir"),
        value=paths.get("obsidian", "output/obsidian/"),
        width=400,
    )

    fmt_heading = ft.Text(_tr("formats"), weight=ft.FontWeight.BOLD, size=13)
    _txt["formats"] = fmt_heading
    paths_heading = ft.Text(_tr("paths"), weight=ft.FontWeight.BOLD, size=13)
    _txt["paths"] = paths_heading

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
                        ics_path_field,
                        logseq_path_field,
                        obsidian_path_field,
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
                ft.TabBar(
                    tabs=[tab_conn, tab_out],
                ),
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
        value=config.get("save_password", True),
    )

    # ── Fetch button ─────────────────────────────────────────────
    def do_fetch(retried=False):
        ui_to_config()
        core.save_config(config)

        if not config.get("token"):
            username = config.get("username", "")
            password = config.get("password", "")
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
                if not save_pw_check.value:
                    config.pop("password", None)
                    password_field.value = ""
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
        fetch_btn.content = _tr("working")
        clear_log()
        log(_tr("fetching_events"))
        page.update()

        def worker():
            try:
                logs = core.run_with_config(config)
                page.run_thread(lambda: on_fetch_done(logs, None))
            except PermissionError as e:
                config.pop("token", None)
                core.save_config(config)
                if retried:
                    page.run_thread(lambda: on_fetch_done([], _tr("login_failed_again").format(e)))
                else:
                    log(_tr("token_expired"))
                    page.run_thread(lambda: do_fetch(True))
            except Exception as e:
                page.run_thread(lambda: on_fetch_done([], str(e)))

        threading.Thread(target=worker, daemon=True).start()

    def on_fetch_done(logs, error):
        fetch_btn.disabled = False
        fetch_btn.content = _tr("fetch")
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
        width=300,
        height=40,
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
                ft.Container(
                    content=fetch_btn,
                    alignment=ft.Alignment.CENTER,
                ),
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
