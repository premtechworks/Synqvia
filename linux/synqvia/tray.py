"""Tray icon + menu. Prefers Ayatana AppIndicator; falls back to StatusIcon.

Both paths open the main window (window.py) — left-click or menu item.
"""
import logging

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib

log = logging.getLogger("synqvia.tray")

try:
    gi.require_version("AyatanaAppIndicator3", "0.1")
    from gi.repository import AyatanaAppIndicator3
    HAVE_INDICATOR = True
except Exception:
    HAVE_INDICATOR = False


class TrayApp:
    def __init__(self, engine, history, config, save_config, on_quit, show_window=None, *args, **kwargs):
        self.engine = engine
        self.history = history
        self.config = config
        self.save_config = save_config
        self.on_quit = on_quit
        self.show_window = show_window or (lambda: None)
        self._connected = False
        self._peer = ""
        self.gtk_app = None
        try:
            engine.add_status_listener(self._on_status)
        except Exception:
            engine.status_cb = self._on_status

        if HAVE_INDICATOR:
            self._ind = AyatanaAppIndicator3.Indicator.new(
                "synqvia", "edit-paste",
                AyatanaAppIndicator3.IndicatorCategory.APPLICATION_STATUS)
            self._ind.set_status(AyatanaAppIndicator3.IndicatorStatus.ACTIVE)
            self._ind.set_menu(self._build_menu())
            self._icon = None
            log.info("tray: using AyatanaAppIndicator")
        else:
            self._ind = None
            self._icon = Gtk.StatusIcon.new_from_icon_name("edit-paste")
            self._icon.set_tooltip_text("Synqvia: starting…")
            try:
                self._icon.connect("activate", lambda *a: self.show_window())
            except Exception:
                pass
            self._icon.connect("popup-menu", self._on_menu_legacy)
            log.info("tray: using legacy StatusIcon")

    # ---------- status ----------
    def _on_status(self, connected: bool, peer: str):
        self._connected = connected
        self._peer = peer or ""
        GLib.idle_add(self._refresh)

    def _state_text(self):
        if self._connected:
            return f"Connected ({self._peer})" if self._peer else "Connected"
        return f"Offline ({self._peer})" if self._peer else "Offline (listening)"

    def _refresh(self):
        tip = f"Synqvia: {self._state_text()}"
        try:
            if self._ind is not None:
                self._ind.set_menu(self._build_menu())
            elif self._icon is not None:
                self._icon.set_tooltip_text(tip)
        except Exception as e:
            log.debug("tray refresh failed: %s", e)
        return False

    # ---------- menus ----------
    def _build_menu(self):
        menu = Gtk.Menu()
        hdr = Gtk.MenuItem(label=f"Synqvia: {self._state_text()}")
        hdr.set_sensitive(False)
        menu.append(hdr)
        menu.append(Gtk.SeparatorMenuItem())

        show = Gtk.MenuItem(label="Show window")
        show.connect("activate", lambda *a: self.show_window())
        menu.append(show)

        for row in self.history.recent(limit=10):
            text = row[1]
            direction = row[4]
            label = (text[:60].replace("\n", " ⏎ ") or "(empty)")
            item = Gtk.MenuItem(label=f"{'◀' if direction == 'remote' else '▶'} {label}")
            item.connect("activate", self._repaste, text)
            menu.append(item)
        menu.append(Gtk.SeparatorMenuItem())

        for label, cb in [
            ("Clear history", self._clear),
            ("Quit", self._quit),
        ]:
            it = Gtk.MenuItem(label=label)
            it.connect("activate", cb)
            menu.append(it)
        menu.show_all()
        return menu

    def _on_menu_legacy(self, icon, button, t):
        menu = self._build_menu()
        try:
            menu.popup_at_pointer(None)
        except Exception:
            menu.popup(None, None, None, None, button, t)

    def _repaste(self, _w, text):
        try:
            self.engine.repaste(text)
        except Exception as e:
            log.warning("repaste failed: %s", e)

    def _clear(self, _w):
        try:
            self.history.clear()
        except Exception as e:
            log.warning("clear failed: %s", e)

    def _quit(self, _w):
        try:
            self.on_quit()
        finally:
            try:
                if self.gtk_app is not None:
                    self.gtk_app.quit()
                    return
            except Exception:
                pass
            try:
                Gtk.main_quit()
            except Exception:
                pass

    def run(self):
        Gtk.main()
