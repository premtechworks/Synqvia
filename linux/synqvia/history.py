"""SQLite history store with configurable cap."""
import sqlite3
import threading
import time
import os

SCHEMA = """
CREATE TABLE IF NOT EXISTS clips (
  id TEXT PRIMARY KEY,
  text TEXT NOT NULL,
  ts INTEGER NOT NULL,
  src TEXT NOT NULL,
  direction TEXT NOT NULL,
  conflict_loser INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_clips_ts ON clips(ts DESC);
"""


class History:
    def __init__(self, db_path: str, cap: int = 0):
        """cap: 0 = unlimited, else max rows (oldest pruned)."""
        os.makedirs(os.path.dirname(os.path.abspath(db_path)), exist_ok=True)
        self.db_path = db_path
        self.cap = cap
        self._lock = threading.Lock()
        self._db = sqlite3.connect(db_path, check_same_thread=False)
        self._db.execute("PRAGMA journal_mode=WAL;")
        self._db.executescript(SCHEMA)

    def set_cap(self, cap: int):
        with self._lock:
            self.cap = cap
            self._prune_locked()
            self._db.commit()

    def insert(self, msg_id: str, text: str, ts: int, src: str,
               direction: str, conflict_loser: bool = False) -> bool:
        """Returns False if id already present (dedup)."""
        with self._lock:
            try:
                self._db.execute(
                    "INSERT INTO clips(id,text,ts,src,direction,conflict_loser) VALUES(?,?,?,?,?,?)",
                    (msg_id, text, ts, src, direction, 1 if conflict_loser else 0))
                self._prune_locked()
                self._db.commit()
                return True
            except sqlite3.IntegrityError:
                return False

    def _prune_locked(self):
        if self.cap and self.cap > 0:
            self._db.execute(
                "DELETE FROM clips WHERE id NOT IN "
                "(SELECT id FROM clips ORDER BY ts DESC, rowid DESC LIMIT ?)", (self.cap,))

    def exists(self, msg_id: str) -> bool:
        with self._lock:
            cur = self._db.execute("SELECT 1 FROM clips WHERE id=? LIMIT 1", (msg_id,))
            return cur.fetchone() is not None

    def mark_loser(self, msg_id: str):
        with self._lock:
            self._db.execute("UPDATE clips SET conflict_loser=1 WHERE id=?", (msg_id,))
            self._db.commit()

    @staticmethod
    def _escape_like(q: str) -> str:
        return q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    def recent(self, limit: int = 50, query: str = ""):
        with self._lock:
            if query:
                q = f"%{self._escape_like(query)}%"
                cur = self._db.execute(
                    "SELECT id,text,ts,src,direction,conflict_loser FROM clips "
                    "WHERE text LIKE ? ESCAPE '\\' "
                    "ORDER BY ts DESC, rowid DESC LIMIT ?", (q, limit))
            else:
                cur = self._db.execute(
                    "SELECT id,text,ts,src,direction,conflict_loser FROM clips "
                    "ORDER BY ts DESC, rowid DESC LIMIT ?",
                    (limit,))
            return cur.fetchall()

    def clear(self):
        with self._lock:
            self._db.execute("DELETE FROM clips")
            self._db.commit()

    def count(self) -> int:
        with self._lock:
            return self._db.execute("SELECT COUNT(*) FROM clips").fetchone()[0]
