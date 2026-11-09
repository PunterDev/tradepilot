#!/usr/bin/env python3
"""Fail closed on an unverified Dependency-Check database bundle."""

from __future__ import annotations

import argparse
import hashlib
import json
import tarfile
from datetime import UTC, datetime, timedelta
from pathlib import Path


SCANNER_VERSION = "12.2.2"
MAX_AGE = timedelta(hours=24)
REPOSITORY = "PunterDev/tradepilot"


def fail(message: str) -> None:
    raise SystemExit(f"trusted NVD bundle rejected: {message}")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--as-of", default=None)
    args = parser.parse_args()
    if not args.manifest.is_file() or not args.archive.is_file():
        fail("manifest or archive is missing")
    try:
        manifest = json.loads(args.manifest.read_text())
        created = datetime.fromisoformat(manifest["createdAt"].replace("Z", "+00:00"))
    except (KeyError, ValueError, json.JSONDecodeError):
        fail("manifest is malformed")
    if manifest.get("scannerVersion") != SCANNER_VERSION:
        fail("scanner version does not match the pinned version")
    if manifest.get("repository") != REPOSITORY:
        fail("bundle provenance repository does not match")
    control_commit = manifest.get("controlCommit")
    if not isinstance(control_commit, str) or len(control_commit) != 40 or any(
        character not in "0123456789abcdef" for character in control_commit.lower()
    ):
        fail("bundle provenance control commit is invalid")
    if manifest.get("archiveSha256") != sha256(args.archive):
        fail("archive checksum mismatch")
    now = datetime.fromisoformat(args.as_of.replace("Z", "+00:00")) if args.as_of else datetime.now(UTC)
    if created.tzinfo is None or created > now or now - created > MAX_AGE:
        fail("archive is stale or has an invalid creation timestamp")
    try:
        with tarfile.open(args.archive, "r:gz") as archive:
            members = archive.getmembers()
            if any(member.name.startswith("/") or ".." in Path(member.name).parts for member in members):
                fail("archive contains an unsafe path")
            if not any(Path(member.name).name == "odc.mv.db" for member in members):
                fail("archive does not contain the Dependency-Check database")
    except tarfile.TarError:
        fail("archive is corrupt")


if __name__ == "__main__":
    main()
