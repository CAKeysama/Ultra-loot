"""Shared output helper for the Ultra Loot generators (data, lang).

Generated files are written only when their content changes, so running a generator twice produces no Git diff and no
timestamp churn. Content is compared with line endings normalised; an existing file keeps its line-ending style
(a Windows checkout with core.autocrlf=true has CRLF files) and new files are written with LF, which Git stores
unchanged. Pass --check to a generator to list the files that would change without writing anything (exit code 1
when something is out of date).
"""
import json
import os
import sys

CHECK = "--check" in sys.argv


class Output:
    def __init__(self):
        self.changed = []
        self.unchanged = 0

    def text(self, path, text):
        new = text.replace("\r\n", "\n")
        crlf = False
        if os.path.exists(path):
            with open(path, "rb") as f:
                raw = f.read()
            if raw.decode("utf-8").replace("\r\n", "\n") == new:
                self.unchanged += 1
                return False
            crlf = b"\r\n" in raw
        self.changed.append(path)
        if not CHECK:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            data = new.replace("\n", "\r\n") if crlf else new
            with open(path, "wb") as f:
                f.write(data.encode("utf-8"))
        return True

    def json(self, path, obj, ensure_ascii=True):
        return self.text(path, json.dumps(obj, indent=2, ensure_ascii=ensure_ascii) + "\n")

    def report(self, label, root):
        verb = "would change" if CHECK else "written"
        print(f"{label}: {len(self.changed)} {verb}, {self.unchanged} unchanged")
        for p in self.changed:
            print("  " + os.path.relpath(p, root))
        if CHECK and self.changed:
            sys.exit(1)
