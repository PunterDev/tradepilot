#!/usr/bin/env python3
"""Validate the public-schema scope of a disposable TradePilot recovery."""

from __future__ import annotations

import argparse
import csv
import json
import re
from pathlib import Path


IDENTIFIER = r'(?:(?:"(?:[^"]|"")+")|(?:[A-Za-z_][A-Za-z0-9_$]*))'


def count(pattern: str, value: str) -> int:
    return len(re.findall(pattern, value, flags=re.IGNORECASE | re.MULTILINE))


def normalized_identifier(value: str) -> str:
    if value.startswith('"') and value.endswith('"'):
        return value[1:-1].replace('""', '"')
    return value.lower()


def build_manifest(ddl: str) -> dict[str, object]:
    lowered = ddl.lower()
    forbidden_schema_reference = re.compile(
        r"\b(?:auth|extensions|graphql|graphql_public|pgbouncer|realtime|storage|supabase_migrations|vault)\s*\."
    )
    forbidden_extension_reference = re.compile(
        r"\b(?:supabase_vault|pgcrypto|uuid-ossp|pg_stat_statements)\b"
    )
    extension_function_reference = re.compile(
        r"\b(?:crypt|digest|gen_random_uuid|hmac|pgp_[a-z0-9_]*|uuid_generate_[a-z0-9_]*)\s*\("
    )
    if (
        forbidden_schema_reference.search(lowered)
        or forbidden_extension_reference.search(lowered)
        or extension_function_reference.search(lowered)
    ):
        raise ValueError(
            "application-scope archive depends on an excluded Supabase-managed schema or extension"
        )

    sequence_inventory: dict[str, dict[str, str | None]] = {}
    for match in re.finditer(
        rf"^CREATE SEQUENCE public\.(?P<sequence>{IDENTIFIER})", ddl, re.IGNORECASE | re.MULTILINE
    ):
        name = normalized_identifier(match.group("sequence"))
        sequence_inventory[name] = {
            "name": name,
            "owner_table": None,
            "owner_column": None,
            "dependency_type": None,
            "identity_type": None,
        }

    for match in re.finditer(
        rf"^ALTER SEQUENCE public\.(?P<sequence>{IDENTIFIER}) OWNED BY public\.(?P<table>{IDENTIFIER})\.(?P<column>{IDENTIFIER});",
        ddl,
        re.IGNORECASE | re.MULTILINE,
    ):
        name = normalized_identifier(match.group("sequence"))
        if name not in sequence_inventory:
            raise ValueError("application-scope archive has an unrecognized sequence ownership definition")
        sequence_inventory[name].update(
            {
                "owner_table": normalized_identifier(match.group("table")),
                "owner_column": normalized_identifier(match.group("column")),
                "dependency_type": "a",
            }
        )

    identity_pattern = re.compile(
        rf"ALTER TABLE(?: ONLY)? public\.(?P<table>{IDENTIFIER})\s+ALTER COLUMN (?P<column>{IDENTIFIER})\s+ADD GENERATED (?P<mode>ALWAYS|BY DEFAULT) AS IDENTITY\s*\(\s*SEQUENCE NAME public\.(?P<sequence>{IDENTIFIER})",
        re.IGNORECASE | re.DOTALL,
    )
    for match in identity_pattern.finditer(ddl):
        name = normalized_identifier(match.group("sequence"))
        if name in sequence_inventory:
            raise ValueError("application-scope archive represents an identity sequence as an explicit sequence")
        sequence_inventory[name] = {
            "name": name,
            "owner_table": normalized_identifier(match.group("table")),
            "owner_column": normalized_identifier(match.group("column")),
            "dependency_type": "i",
            "identity_type": "a" if match.group("mode").upper() == "ALWAYS" else "d",
        }

    manifest: dict[str, object] = {
        "tables": count(r"^CREATE TABLE(?: ONLY)? public\.", ddl),
        "sequences": sorted(sequence_inventory.values(), key=lambda item: str(item["name"])),
        "functions": count(r"^CREATE FUNCTION public\.", ddl),
        "rls_tables": count(r"\bENABLE ROW LEVEL SECURITY\b", ddl),
        "policies": count(r"^CREATE POLICY ", ddl),
        "triggers": count(r"^CREATE TRIGGER ", ddl),
        "explicit_indexes": count(r"^CREATE (?:UNIQUE )?INDEX .* ON public\.", ddl),
    }
    if manifest["tables"] == 0:
        raise ValueError("application-scope archive contains no public tables")
    return manifest


def read_actual_sequences(path: Path) -> dict[str, dict[str, str | None]]:
    inventory: dict[str, dict[str, str | None]] = {}
    with path.open(encoding="utf-8", newline="") as stream:
        for row in csv.reader(stream, delimiter="\t"):
            if len(row) != 5:
                raise ValueError("restored sequence inventory has an invalid row")
            name, owner_table, owner_column, dependency_type, identity_type = row
            if not name or name in inventory:
                raise ValueError("restored sequence inventory is incomplete or ambiguous")
            inventory[name] = {
                "name": name,
                "owner_table": owner_table or None,
                "owner_column": owner_column or None,
                "dependency_type": dependency_type or None,
                "identity_type": identity_type or None,
            }
    return inventory


def validate_sequence_inventory(manifest: dict[str, object], actual_path: Path) -> None:
    expected = {str(item["name"]): item for item in manifest["sequences"]}  # type: ignore[index]
    actual = read_actual_sequences(actual_path)
    missing = sorted(set(expected) - set(actual))
    unexpected = sorted(set(actual) - set(expected))
    if missing or unexpected:
        details = []
        if missing:
            details.append("missing=" + ",".join(missing))
        if unexpected:
            details.append("unexpected=" + ",".join(unexpected))
        raise ValueError("restored sequence inventory mismatch: " + "; ".join(details))
    for name, expected_entry in expected.items():
        actual_entry = actual[name]
        for field in ("owner_table", "owner_column", "dependency_type", "identity_type"):
            if actual_entry[field] != expected_entry[field]:
                raise ValueError(f"restored sequence definition mismatch: {name}")


def parse_sequence_states(path: Path) -> dict[str, tuple[int, bool]]:
    contents = path.read_text(encoding="utf-8")
    states: dict[str, tuple[int, bool]] = {}
    pattern = re.compile(
        r"SELECT pg_catalog\.setval\('public\.(?P<sequence>(?:[^']|'')+)'(?:::regclass)?,\s*(?P<value>-?\d+),\s*(?P<called>true|false)\);",
        re.IGNORECASE,
    )
    for match in pattern.finditer(contents):
        name = match.group("sequence").replace("''", "'")
        if name in states:
            raise ValueError("application-scope archive contains duplicate sequence state")
        states[name] = (int(match.group("value")), match.group("called").lower() == "true")
    return states


def sequence_state_check(manifest: dict[str, object], path: Path) -> str:
    expected_names = {str(item["name"]) for item in manifest["sequences"]}  # type: ignore[index]
    states = parse_sequence_states(path)
    missing = sorted(expected_names - set(states))
    unexpected = sorted(set(states) - expected_names)
    if missing or unexpected:
        details = []
        if missing:
            details.append("missing=" + ",".join(missing))
        if unexpected:
            details.append("unexpected=" + ",".join(unexpected))
        raise ValueError("application-scope sequence state inventory mismatch: " + "; ".join(details))
    values = ",\n        ".join(
        "('" + name.replace("'", "''") + f"', {value}, {'true' if called else 'false'})"
        for name, (value, called) in sorted(states.items())
    )
    return f"""DO $sequence_state_check$
DECLARE
  expected record;
  actual_last_value bigint;
  actual_is_called boolean;
BEGIN
  FOR expected IN
    SELECT * FROM (VALUES
        {values}
    ) AS states(sequence_name, last_value, is_called)
  LOOP
    EXECUTE format('SELECT last_value, is_called FROM public.%I', expected.sequence_name)
      INTO actual_last_value, actual_is_called;
    IF actual_last_value IS DISTINCT FROM expected.last_value
       OR actual_is_called IS DISTINCT FROM expected.is_called THEN
      RAISE EXCEPTION 'restored sequence state mismatch: %', expected.sequence_name;
    END IF;
  END LOOP;
END
$sequence_state_check$;"""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ddl")
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--actual-sequences")
    parser.add_argument("--sequence-states")
    parser.add_argument("--emit-sequence-state-check", action="store_true")
    args = parser.parse_args()
    manifest_path = Path(args.manifest)

    try:
        if args.ddl:
            manifest = build_manifest(Path(args.ddl).read_text(encoding="utf-8"))
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            print(
                "Application-scope archive validation passed: "
                f"{manifest['tables']} public tables, {len(manifest['sequences'])} sequences, "
                f"{manifest['explicit_indexes']} explicit indexes."
            )
            return

        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if args.actual_sequences:
            validate_sequence_inventory(manifest, Path(args.actual_sequences))
            return
        if args.sequence_states and args.emit_sequence_state_check:
            print(sequence_state_check(manifest, Path(args.sequence_states)))
            return
        raise ValueError("select archive validation, sequence inventory validation, or state-check output")
    except ValueError as error:
        raise SystemExit(f"application-scope validation failed: {error}") from error


if __name__ == "__main__":
    main()
