#!/usr/bin/env python3
"""Create and fail-closed validate encrypted-backup public table manifests."""

import argparse
import json
import re
import sys
from pathlib import Path


SHA256 = re.compile(r"^[0-9a-f]{64}$")


def read_counts(path: Path) -> dict[str, int]:
    counts: dict[str, int] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line:
            continue
        try:
            name, value = line.split("\t", 1)
        except ValueError as exc:
            raise SystemExit("table-count inventory is malformed") from exc
        if not name or name in counts or not value.isdigit():
            raise SystemExit("table-count inventory is malformed")
        counts[name] = int(value)
    if not counts:
        raise SystemExit("table-count inventory is empty")
    return counts


def read_manifest(path: Path) -> dict:
    try:
        manifest = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise SystemExit("completeness manifest is unreadable") from exc
    if manifest.get("format") != "tradepilot-public-table-counts-v1":
        raise SystemExit("completeness manifest has an unsupported format")
    if manifest.get("snapshotConsistency") != "postgres-exported-snapshot":
        raise SystemExit("completeness manifest is not snapshot-bound")
    if not isinstance(manifest.get("artifact"), str) or not SHA256.fullmatch(str(manifest.get("encryptedArtifactSha256", ""))):
        raise SystemExit("completeness manifest is not bound to a valid encrypted artifact")
    tables = manifest.get("publicTableRowCounts")
    if not isinstance(tables, dict) or not tables:
        raise SystemExit("completeness manifest has no public table inventory")
    for name, value in tables.items():
        if not isinstance(name, str) or not name or not isinstance(value, int) or value < 0:
            raise SystemExit("completeness manifest has an invalid public table inventory")
    return manifest


def sql_identifier(name: str) -> str:
    return '"' + name.replace('"', '""') + '"'


def create(args: argparse.Namespace) -> None:
    if not SHA256.fullmatch(args.encrypted_artifact_sha256):
        raise SystemExit("encrypted artifact checksum must be a lowercase SHA-256 digest")
    manifest = {
        "format": "tradepilot-public-table-counts-v1",
        "artifact": args.artifact,
        "encryptedArtifactSha256": args.encrypted_artifact_sha256,
        "snapshotConsistency": "postgres-exported-snapshot",
        "publicTableRowCounts": read_counts(Path(args.counts)),
    }
    Path(args.output).write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def emit_queries(args: argparse.Namespace) -> None:
    manifest = read_manifest(Path(args.manifest))
    if manifest["artifact"] != args.artifact or manifest["encryptedArtifactSha256"] != args.encrypted_artifact_sha256:
        raise SystemExit("completeness manifest is not bound to this encrypted archive")
    for table in sorted(manifest["publicTableRowCounts"]):
        literal = table.replace("'", "''")
        print(f"select '{literal}' || chr(9) || count(*) from only public.{sql_identifier(table)}")


def validate(args: argparse.Namespace) -> None:
    expected = read_manifest(Path(args.manifest))["publicTableRowCounts"]
    actual = read_counts(Path(args.counts))
    if expected.keys() != actual.keys():
        raise SystemExit(
            "restored public table inventory does not match the encrypted completeness manifest "
            f"(expected {len(expected)} tables, found {len(actual)})"
        )
    if any(expected[name] != actual[name] for name in expected):
        raise SystemExit("restored public table counts do not match the encrypted completeness manifest")


def main() -> None:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)

    create_parser = commands.add_parser("create")
    create_parser.add_argument("--counts", required=True)
    create_parser.add_argument("--output", required=True)
    create_parser.add_argument("--artifact", required=True)
    create_parser.add_argument("--encrypted-artifact-sha256", required=True)
    create_parser.set_defaults(handler=create)

    queries_parser = commands.add_parser("emit-count-queries")
    queries_parser.add_argument("--manifest", required=True)
    queries_parser.add_argument("--artifact", required=True)
    queries_parser.add_argument("--encrypted-artifact-sha256", required=True)
    queries_parser.set_defaults(handler=emit_queries)

    validate_parser = commands.add_parser("validate")
    validate_parser.add_argument("--manifest", required=True)
    validate_parser.add_argument("--counts", required=True)
    validate_parser.set_defaults(handler=validate)

    args = parser.parse_args()
    args.handler(args)


if __name__ == "__main__":
    main()
