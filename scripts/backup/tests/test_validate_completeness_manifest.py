#!/usr/bin/env python3
"""Regression tests for encrypted-backup completeness manifest validation."""

import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "validate-completeness-manifest.py"
CHECKSUM = "a" * 64


class CompletenessManifestTests(unittest.TestCase):
    def run_tool(self, *args):
        return subprocess.run(
            ["python3", str(SCRIPT), *args],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )

    def create_manifest(self, directory: Path) -> Path:
        counts = directory / "counts.tsv"
        counts.write_text("identity_ledger\t2\nserial_ledger\t1\n", encoding="utf-8")
        manifest = directory / "manifest.json"
        result = self.run_tool(
            "create",
            "--counts", str(counts),
            "--output", str(manifest),
            "--artifact", "tradepilot-postgres-test.dump.age",
            "--encrypted-artifact-sha256", CHECKSUM,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        return manifest

    def test_manifest_requires_exact_inventory_and_counts(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            manifest = self.create_manifest(directory)
            actual = directory / "actual.tsv"
            actual.write_text("identity_ledger\t2\nserial_ledger\t1\n", encoding="utf-8")
            self.assertEqual(self.run_tool("validate", "--manifest", str(manifest), "--counts", str(actual)).returncode, 0)

            actual.write_text("identity_ledger\t2\nserial_ledger\t0\n", encoding="utf-8")
            self.assertNotEqual(self.run_tool("validate", "--manifest", str(manifest), "--counts", str(actual)).returncode, 0)

            actual.write_text("identity_ledger\t2\n", encoding="utf-8")
            self.assertNotEqual(self.run_tool("validate", "--manifest", str(manifest), "--counts", str(actual)).returncode, 0)

    def test_count_queries_require_the_exact_encrypted_archive_binding(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = self.create_manifest(Path(tmp))
            valid = self.run_tool(
                "emit-count-queries", "--manifest", str(manifest),
                "--artifact", "tradepilot-postgres-test.dump.age",
                "--encrypted-artifact-sha256", CHECKSUM,
            )
            self.assertEqual(valid.returncode, 0, valid.stderr)
            self.assertIn('only public."identity_ledger"', valid.stdout)

            changed_archive = self.run_tool(
                "emit-count-queries", "--manifest", str(manifest),
                "--artifact", "tradepilot-postgres-changed.dump.age",
                "--encrypted-artifact-sha256", CHECKSUM,
            )
            self.assertNotEqual(changed_archive.returncode, 0)

            changed_checksum = self.run_tool(
                "emit-count-queries", "--manifest", str(manifest),
                "--artifact", "tradepilot-postgres-test.dump.age",
                "--encrypted-artifact-sha256", "b" * 64,
            )
            self.assertNotEqual(changed_checksum.returncode, 0)


if __name__ == "__main__":
    unittest.main()
