#!/usr/bin/env python3
"""Moodle Calendar Bridge — GUI launcher.

Requires: pip install customtkinter
Run with:  .venv/bin/python3 moodle_cal_gui.py
"""

import sys
import threading
from pathlib import Path

import moodle_cal as core

try:
    import customtkinter as ctk
    from tkinter import messagebox
except ImportError:
    print("customtkinter not found. Install it:")
    print("  pip install customtkinter")
    print("Then re-run this script.")
    sys.exit(1)

ctk.set_appearance_mode("system")
ctk.set_default_color_theme("blue")


class App:
    def __init__(self):
        self.root = ctk.CTk()
        self.root.title("Moodle Calendar Bridge")
        self.root.geometry("560x620")
        self.root.minsize(500, 500)

        self.config = {}

        self._build_ui()
        self._load_config()

    def _build_ui(self):
        outer = ctk.CTkFrame(self.root, fg_color="transparent")
        outer.pack(fill="both", expand=True, padx=20, pady=16)

        # ── Header ────────────────────────────────────────────────
        header = ctk.CTkFrame(outer, fg_color="transparent")
        header.pack(fill="x", pady=(0, 14))
        ctk.CTkLabel(header, text="Moodle Calendar Bridge",
                     font=("Segoe UI", 18, "bold")).pack(anchor="w")
        ctk.CTkLabel(header, text="Sync deadlines to ICS, Logseq, Obsidian",
                     font=("Segoe UI", 11),
                     text_color="gray").pack(anchor="w")

        # ── Tabview ───────────────────────────────────────────────
        self.tabview = ctk.CTkTabview(outer)
        self.tabview.pack(fill="x", pady=(0, 12))

        self._build_connection_tab()
        self._build_output_tab()

        # ── Checkbox ──────────────────────────────────────────────
        self.save_pw_var = ctk.BooleanVar(value=True)
        ctk.CTkCheckBox(outer, text="Store password",
                        variable=self.save_pw_var).pack(anchor="w", pady=(0, 10))

        # ── Fetch button ──────────────────────────────────────────
        self.fetch_btn = ctk.CTkButton(
            outer, text="Fetch & Generate", command=self._fetch,
            fg_color="#2563eb", hover_color="#1d4ed8",
            font=("Segoe UI", 12, "bold"), height=36
        )
        self.fetch_btn.pack(pady=(0, 12))

        # ── Log area ──────────────────────────────────────────────
        ctk.CTkLabel(outer, text="Log", anchor="w",
                     font=("Segoe UI", 10)).pack(fill="x", pady=(0, 4))
        self.log_area = ctk.CTkTextbox(outer, font=("Consolas", 10), wrap="word")
        self.log_area.pack(fill="both", expand=True)
        self.log_area.configure(state="disabled")

        # ── Status bar ────────────────────────────────────────────
        self.status_var = ctk.StringVar(value="Ready")
        ctk.CTkLabel(outer, textvariable=self.status_var,
                     font=("Segoe UI", 9),
                     text_color="gray").pack(anchor="w", pady=(8, 0))

    # ── Tabs ─────────────────────────────────────────────────────

    def _build_connection_tab(self):
        conn = self.tabview.add("Connection")

        group = ctk.CTkFrame(conn)
        group.pack(fill="x", pady=(0, 4), padx=2, ipady=4)

        ctk.CTkLabel(group, text="Moodle URL",
                     font=("Segoe UI", 10)).pack(anchor="w", pady=(10, 2))
        self.url_var = ctk.StringVar()
        ctk.CTkEntry(group, textvariable=self.url_var, width=380).pack(pady=(0, 10))

        ctk.CTkLabel(group, text="Username",
                     font=("Segoe UI", 10)).pack(anchor="w", pady=(0, 2))
        self.username_var = ctk.StringVar()
        ctk.CTkEntry(group, textvariable=self.username_var, width=380).pack(pady=(0, 10))

        ctk.CTkLabel(group, text="Password",
                     font=("Segoe UI", 10)).pack(anchor="w", pady=(0, 2))
        self.password_var = ctk.StringVar()
        ctk.CTkEntry(group, textvariable=self.password_var, width=380,
                     show="*").pack(pady=(0, 6))

        ctk.CTkLabel(conn,
                     text="Password is not stored by default.\n"
                           "config.json is locked to your user (chmod 600).",
                     font=("Segoe UI", 9),
                     text_color="gray").pack(anchor="w", pady=(6, 0))

    def _build_output_tab(self):
        out = self.tabview.add("Output")

        fmt_frame = ctk.CTkFrame(out)
        fmt_frame.pack(fill="x", pady=(0, 12), padx=2)

        ctk.CTkLabel(fmt_frame, text="Formats",
                     font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(8, 6))

        self.ics_var = ctk.BooleanVar(value=True)
        self.logseq_var = ctk.BooleanVar(value=True)
        self.obsidian_var = ctk.BooleanVar(value=True)

        ctk.CTkCheckBox(fmt_frame, text="ICS (calendar.ics)",
                        variable=self.ics_var).pack(anchor="w", pady=2)
        ctk.CTkCheckBox(fmt_frame, text="Logseq Markdown",
                        variable=self.logseq_var).pack(anchor="w", pady=2)
        ctk.CTkCheckBox(fmt_frame, text="Obsidian Markdown",
                        variable=self.obsidian_var).pack(anchor="w", pady=2)

        path_frame = ctk.CTkFrame(out)
        path_frame.pack(fill="x", padx=2)

        ctk.CTkLabel(path_frame, text="Paths",
                     font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(8, 6))

        self.ics_path_var = ctk.StringVar(value="output/calendar.ics")
        ctk.CTkLabel(path_frame, text="ICS file:",
                     font=("Segoe UI", 10)).pack(anchor="w")
        ctk.CTkEntry(path_frame, textvariable=self.ics_path_var,
                     width=380).pack(pady=(0, 8))

        self.logseq_path_var = ctk.StringVar(value="output/logseq/")
        ctk.CTkLabel(path_frame, text="Logseq dir:",
                     font=("Segoe UI", 10)).pack(anchor="w")
        ctk.CTkEntry(path_frame, textvariable=self.logseq_path_var,
                     width=380).pack(pady=(0, 8))

        self.obsidian_path_var = ctk.StringVar(value="output/obsidian/")
        ctk.CTkLabel(path_frame, text="Obsidian dir:",
                     font=("Segoe UI", 10)).pack(anchor="w")
        ctk.CTkEntry(path_frame, textvariable=self.obsidian_path_var,
                     width=380).pack(pady=(0, 8))

    # ── Config IO ──────────────────────────────────────────────────

    def _load_config(self):
        try:
            self.config = core.load_config()
        except (FileNotFoundError, ValueError):
            self.config = {
                "moodle_url": "",
                "token": "",
                "username": "",
                "password": "",
                "save_password": True,
                "output": {"ics": True, "logseq": True, "obsidian": True},
                "paths": {
                    "ics": "output/calendar.ics",
                    "logseq": "output/logseq/",
                    "obsidian": "output/obsidian/",
                },
            }
        self._config_to_ui()

    def _config_to_ui(self):
        self.url_var.set(self.config.get("moodle_url", ""))
        self.username_var.set(self.config.get("username", ""))
        self.password_var.set(self.config.get("password", "") if self.config.get("save_password") else "")
        self.save_pw_var.set(self.config.get("save_password", True))
        out = self.config.get("output", {})
        self.ics_var.set(out.get("ics", True))
        self.logseq_var.set(out.get("logseq", True))
        self.obsidian_var.set(out.get("obsidian", True))
        paths = self.config.get("paths", {})
        self.ics_path_var.set(paths.get("ics", "output/calendar.ics"))
        self.logseq_path_var.set(paths.get("logseq", "output/logseq/"))
        self.obsidian_path_var.set(paths.get("obsidian", "output/obsidian/"))

    def _ui_to_config(self):
        self.config["moodle_url"] = self.url_var.get().strip()
        self.config["username"] = self.username_var.get().strip()
        if self.save_pw_var.get():
            self.config["password"] = self.password_var.get()
        else:
            self.config.pop("password", None)
        self.config["save_password"] = self.save_pw_var.get()
        self.config["output"] = {
            "ics": self.ics_var.get(),
            "logseq": self.logseq_var.get(),
            "obsidian": self.obsidian_var.get(),
        }
        self.config["paths"] = {
            "ics": self.ics_path_var.get().strip(),
            "logseq": self.logseq_path_var.get().strip(),
            "obsidian": self.obsidian_path_var.get().strip(),
        }

    # ── Fetch & Generate ──────────────────────────────────────────

    def _fetch(self):
        self._ui_to_config()
        core.save_config(self.config)
        self._do_fetch()

    def _do_fetch(self, retried=False):
        if not self.config.get("token"):
            username = self.config.get("username", "")
            password = self.config.get("password", "")
            if not username or not password:
                self.status_var.set("Need credentials")
                messagebox.showerror("Error", "Enter your Moodle username and password in the Connection tab.")
                return
            self._log("No cached token \u2014 logging in\u2026")
            try:
                token = core.login(self.config["moodle_url"], username, password)
                self.config["token"] = token
                if not self.save_pw_var.get():
                    self.config.pop("password", None)
                    self.password_var.set("")
                core.save_config(self.config)
                self._log("Login successful, token saved.")
            except Exception as e:
                self.status_var.set("Login failed")
                messagebox.showerror("Login failed", str(e))
                return

        self.status_var.set("Fetching\u2026")
        self.fetch_btn.configure(state="disabled", text="Working\u2026")
        self._clear_log()
        self._log("Fetching events from Moodle\u2026")

        def worker():
            try:
                logs = core.run_with_config(self.config)
                self.root.after(0, self._on_fetch_done, logs, None)
            except PermissionError as e:
                self.config.pop("token", None)
                core.save_config(self.config)
                if retried:
                    self.root.after(0, self._on_fetch_done, [], f"Login failed again: {e}")
                else:
                    self._log("Token expired \u2014 re-logging in\u2026")
                    self.root.after(0, self._do_fetch, True)
            except Exception as e:
                self.root.after(0, self._on_fetch_done, [], str(e))

        threading.Thread(target=worker, daemon=True).start()

    def _on_fetch_done(self, logs, error):
        self.fetch_btn.configure(state="normal", text="Fetch & Generate")
        if error:
            self.status_var.set("Error")
            self._log(f"Error: {error}")
            messagebox.showerror("Error", error)
        else:
            self.status_var.set("Done")
            self._log("Done.")

    # ── Logging ────────────────────────────────────────────────────

    def _log(self, msg):
        self.log_area.configure(state="normal")
        self.log_area.insert("end", msg + "\n")
        self.log_area.see("end")
        self.log_area.configure(state="disabled")

    def _clear_log(self):
        self.log_area.configure(state="normal")
        self.log_area.delete("1.0", "end")
        self.log_area.configure(state="disabled")

    # ── Run ────────────────────────────────────────────────────────

    def run(self):
        self.root.mainloop()


def main():
    App().run()


if __name__ == "__main__":
    main()
