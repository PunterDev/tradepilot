#!/usr/bin/env python3
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SELECTOR = ROOT / "retention.py"


def select(contents, retention=21):
    result = subprocess.run(
        [sys.executable, str(SELECTOR), "--retention-days", str(retention), "--now", "2026-11-01T00:00:00Z"],
        input=json.dumps({"Contents": contents}), text=True, capture_output=True, check=True,
    )
    return json.loads(result.stdout)["delete"]


def item(day):
    return {"Key": f"tradepilot/production/postgres/tradepilot-postgres-2026{day}.dump.age", "LastModified": f"2026-{day[:2]}-{day[2:]}T00:00:00Z"}


assert select([item("1001"), item("1010")]) == []
assert select([item("0901"), item("0910"), item("1001"), item("1020")]) == [item("0901")["Key"], item("0910")["Key"]]
try:
    select([], 13)
except subprocess.CalledProcessError:
    pass
else:
    raise AssertionError("retention below fourteen days must fail")

print("retention selector tests passed")
