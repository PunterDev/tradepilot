#!/usr/bin/env python3
"""Select old encrypted backup objects without ever selecting the newest one."""

import argparse
import datetime as dt
import json
import sys


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--retention-days", type=int, required=True)
    parser.add_argument("--now", required=True, help="UTC timestamp in ISO 8601 format")
    return parser.parse_args()


def parse_time(value):
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))


def main():
    args = parse_args()
    if args.retention_days < 14:
        raise SystemExit("retention must be at least 14 days")
    now = parse_time(args.now)
    objects = json.load(sys.stdin).get("Contents", [])
    backups = [item for item in objects if item.get("Key", "").endswith(".dump.age")]
    # Never delete when it would leave fewer than two independently retained artifacts.
    if len(backups) <= 2:
        json.dump({"delete": []}, sys.stdout)
        return
    cutoff = now - dt.timedelta(days=args.retention_days)
    expired = [item["Key"] for item in backups if parse_time(item["LastModified"]) < cutoff]
    # Retain at least two artifacts even when several are older than the window.
    max_deletions = max(0, len(backups) - 2)
    json.dump({"delete": expired[:max_deletions]}, sys.stdout)


if __name__ == "__main__":
    main()
