#!/usr/bin/env python3
"""Reject a public-schema restore that depends on Supabase-managed objects."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


def count(pattern: str, value: str) -> int:
    return len(re.findall(pattern, value, flags=re.IGNORECASE | re.MULTILINE))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ddl", required=True)
    parser.add_argument("--manifest", required=True)
    args = parser.parse_args()

    ddl = Path(args.ddl).read_text(encoding="utf-8")
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
        raise SystemExit(
            "application-scope archive depends on an excluded Supabase-managed schema or extension"
        )

    manifest = {
        "tables": count(r"^CREATE TABLE(?: ONLY)? public\.", ddl),
        "sequences": count(r"^CREATE SEQUENCE public\.", ddl),
        "functions": count(r"^CREATE FUNCTION public\.", ddl),
        "rls_tables": count(r"\bENABLE ROW LEVEL SECURITY\b", ddl),
        "policies": count(r"^CREATE POLICY ", ddl),
        "triggers": count(r"^CREATE TRIGGER ", ddl),
        "explicit_indexes": count(r"^CREATE (?:UNIQUE )?INDEX .* ON public\.", ddl),
    }
    if manifest["tables"] == 0:
        raise SystemExit("application-scope archive contains no public tables")

    Path(args.manifest).write_text(json.dumps(manifest), encoding="utf-8")
    print(
        "Application-scope archive validation passed: "
        f"{manifest['tables']} public tables, {manifest['sequences']} sequences, "
        f"{manifest['explicit_indexes']} explicit indexes."
    )


if __name__ == "__main__":
    main()
