#!/usr/bin/env python3
"""Fail CI when common local/runtime secrets or artifacts are tracked.

This is intentionally small and dependency-free. It does not replace a full
secret scanner or history rewrite, but it prevents the mistakes that previously
occurred in this repository from being reintroduced on the current branch.
"""

from __future__ import annotations

import re
import subprocess
from pathlib import Path


BANNED_PREFIXES = (".claude/", "uploads/")
BANNED_SUFFIXES = (".exe",)

SENSITIVE_PROPERTY_KEYS = {
    "spring.datasource.password",
    "spring.datasource.username",
    "spring.mail.username",
    "spring.mail.password",
    "jwt.secret",
    "admin.invitation-code",
    "amap.api.key",
    "medical.ai.api-key",
}

LOCAL_ONLY_URL_KEYS = {"spring.datasource.url"}
PLACEHOLDER_PREFIXES = ("${", "YOUR_", "CHANGE_ME", "REPLACE_ME")
CREDENTIAL_URL = re.compile(r"https?://[^/\s:@]+:[^@\s/]+@", re.IGNORECASE)


def tracked_files() -> list[str]:
    output = subprocess.check_output(["git", "ls-files"], text=True)
    return [line.strip() for line in output.splitlines() if line.strip()]


def placeholder(value: str) -> bool:
    stripped = value.strip()
    return not stripped or stripped.startswith(PLACEHOLDER_PREFIXES)


def check_properties(path: Path, issues: list[str]) -> None:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return

    for line_number, raw in enumerate(text.splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = (part.strip() for part in line.split("=", 1))

        if key in SENSITIVE_PROPERTY_KEYS and not placeholder(value):
            issues.append(f"{path}:{line_number}: hard-coded sensitive property '{key}'")

        if key in LOCAL_ONLY_URL_KEYS and not placeholder(value):
            lower = value.lower()
            if "localhost" not in lower and "127.0.0.1" not in lower:
                issues.append(f"{path}:{line_number}: non-local datasource URL is committed")


def check_credential_urls(path: Path, issues: list[str]) -> None:
    try:
        if path.stat().st_size > 1_000_000:
            return
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return

    if CREDENTIAL_URL.search(text):
        issues.append(f"{path}: credential-bearing URL detected")


def main() -> int:
    issues: list[str] = []
    tracked = tracked_files()

    for name in tracked:
        lower = name.lower()
        if name.startswith(BANNED_PREFIXES):
            issues.append(f"{name}: local/runtime path must not be tracked")
        if lower.endswith(BANNED_SUFFIXES):
            issues.append(f"{name}: executable artifact must not be tracked")

        path = Path(name)
        if path.suffix == ".properties":
            check_properties(path, issues)
        check_credential_urls(path, issues)

    if issues:
        print("Security guard failed:")
        for issue in issues:
            print(f"  - {issue}")
        return 1

    print(f"Security guard passed ({len(tracked)} tracked files checked).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
