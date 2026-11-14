#!/usr/bin/env python3
"""Regression tests for the trusted NVD archive format and extraction guard."""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import tarfile
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / "scripts/security/trusted_nvd_archive.py"
REPOSITORY = "PunterDev/tradepilot"
WORKFLOW = ".github/workflows/publish-verified-nvd.yml"
REVISION = "a" * 40
NOW = "2026-11-13T12:00:00Z"


class TrustedNvdArchiveTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.data = self.root / "data"
        self.data.mkdir()
        (self.data / "odc.mv.db").write_bytes(b"x" * (1024 * 1024 + 1))
        self.archive = self.root / "nvd.tar.gz"
        self.manifest = self.root / "nvd.json"
        self.build()

    def tearDown(self) -> None:
        self.temp.cleanup()

    def build(self) -> None:
        self.invoke(
            "build", "--data-dir", self.data, "--archive", self.archive, "--manifest", self.manifest,
            "--repository", REPOSITORY, "--publisher-workflow", WORKFLOW,
            "--source-revision", REVISION, "--workflow-run-id", "123", "--max-age-hours", "24",
        )

    def invoke(self, *arguments: object, success: bool = True) -> subprocess.CompletedProcess[str]:
        result = subprocess.run([sys.executable, str(SCRIPT), *map(str, arguments)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0 if success else 1, result.stderr)
        return result

    def verify(self, success: bool = True, **overrides: str) -> subprocess.CompletedProcess[str]:
        verification_time = overrides.get("now")
        if verification_time is None:
            if self.manifest.exists():
                manifest = json.loads(self.manifest.read_text())
                verification_time = (
                    datetime.fromisoformat(manifest["createdAt"].replace("Z", "+00:00"))
                    + timedelta(minutes=1)
                ).isoformat().replace("+00:00", "Z")
            else:
                verification_time = NOW
        args = [
            "verify", "--archive", self.archive, "--manifest", self.manifest,
            "--destination", self.root / "out", "--repository", overrides.get("repository", REPOSITORY),
            "--publisher-workflow", overrides.get("workflow", WORKFLOW), "--max-age-hours", "24", "--now", verification_time,
        ]
        return self.invoke(*args, success=success)

    def rewrite_manifest(self, **changes: object) -> None:
        manifest = json.loads(self.manifest.read_text())
        manifest.update(changes)
        self.manifest.write_text(json.dumps(manifest))

    def test_valid_archive_extracts_before_untrusted_checkout(self) -> None:
        self.verify()
        self.assertTrue((self.root / "out/data/odc.mv.db").is_file())

    def test_wrong_repository_fails_closed(self) -> None:
        self.verify(success=False, repository="PunterDev/other")

    def test_wrong_workflow_identity_fails_closed(self) -> None:
        self.verify(success=False, workflow=".github/workflows/other.yml")

    def test_tampered_archive_fails_closed(self) -> None:
        self.archive.write_bytes(self.archive.read_bytes() + b"tamper")
        self.verify(success=False)

    def test_expired_archive_fails_closed(self) -> None:
        self.rewrite_manifest(createdAt="2026-11-10T00:00:00Z", expiresAt="2026-11-11T00:00:00Z")
        self.verify(success=False, now=NOW)

    def test_unsupported_scanner_version_fails_closed(self) -> None:
        self.rewrite_manifest(scannerVersion="13.0.0")
        self.verify(success=False)

    def test_missing_manifest_fails_closed(self) -> None:
        self.manifest.unlink()
        self.verify(success=False)

    def test_unsafe_archive_path_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            payload = self.root / "payload"
            payload.write_text("bad")
            archive.add(payload, arcname="../outside")
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_symlink_archive_entry_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            info = tarfile.TarInfo("data/odc.mv.db")
            info.type = tarfile.SYMTYPE
            info.linkname = "/etc/passwd"
            archive.addfile(info)
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_hardlink_archive_entry_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            info = tarfile.TarInfo("data/odc.mv.db")
            info.type = tarfile.LNKTYPE
            info.linkname = "data/other"
            archive.addfile(info)
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_oversized_manifest_fails_closed(self) -> None:
        self.rewrite_manifest(archiveSize=2 * 1024 * 1024 * 1024 + 1)
        self.verify(success=False)

    def test_invalid_source_revision_fails_closed(self) -> None:
        self.rewrite_manifest(sourceRevision="not-a-sha")
        self.verify(success=False)

    def test_publisher_run_identity_mismatch_fails_closed(self) -> None:
        self.invoke(
            "verify", "--archive", self.archive, "--manifest", self.manifest,
            "--destination", self.root / "out", "--repository", REPOSITORY,
            "--publisher-workflow", WORKFLOW, "--max-age-hours", "24", "--now", NOW,
            "--expected-source-revision", REVISION, "--expected-workflow-run-id", "999", success=False,
        )

    def test_publisher_source_revision_mismatch_fails_closed(self) -> None:
        self.invoke(
            "verify", "--archive", self.archive, "--manifest", self.manifest,
            "--destination", self.root / "out", "--repository", REPOSITORY,
            "--publisher-workflow", WORKFLOW, "--max-age-hours", "24",
            "--expected-source-revision", "b" * 40, "--expected-workflow-run-id", "123", success=False,
        )

    def test_refresh_without_nvd_key_fails_closed(self) -> None:
        result = subprocess.run(
            [
                sys.executable, str(SCRIPT), "refresh-and-build", "--data-dir", self.root / "refresh-data",
                "--archive", self.root / "refresh.tar.gz", "--manifest", self.root / "refresh.json",
                "--repository", REPOSITORY, "--publisher-workflow", WORKFLOW,
                "--source-revision", REVISION, "--workflow-run-id", "123",
            ],
            capture_output=True, text=True, env={"PATH": os.environ["PATH"]},
        )
        self.assertEqual(result.returncode, 1)
        self.assertNotIn("NVD_API_KEY=", result.stderr)


class WorkflowBoundaryTests(unittest.TestCase):
    def test_publisher_and_consumer_boundaries_are_explicit(self) -> None:
        publisher = (ROOT / ".github/workflows/publish-verified-nvd.yml").read_text()
        consumer = (ROOT / ".github/workflows/trusted-pr-dependency-check.yml").read_text()
        self.assertIn("environment: trusted-nvd-publisher", publisher)
        self.assertIn("refresh-and-build", publisher)
        self.assertNotIn("pull_request:", publisher)
        self.assertIn("pull_request_target:", consumer)
        self.assertIn("Check out protected verifier source", consumer)
        self.assertIn("persist-credentials: false", consumer)
        self.assertIn("NVD_API_KEY: ''", consumer)
        self.assertIn("-DautoUpdate=false", consumer)
        self.assertIn("-DfailOnError=true", consumer)
        self.assertIn("-DfailBuildOnCVSS=8", consumer)
        self.assertIn("-DfailBuildOnUnusedSuppressionRule=true", consumer)
        self.assertIn("HTML,JSON,SARIF", consumer)
        self.assertIn("dependency-check-report.html", consumer)
        self.assertIn("dependency-check-report.sarif", consumer)
        self.assertNotIn("docs/security/candidates", consumer)
        self.assertNotIn("actions/cache", publisher + consumer)
        self.assertNotIn("tar -C \"$RUNNER_TEMP\" -xzf", consumer)
        self.assertLess(consumer.index("Download and verify trusted NVD package"), consumer.index("Check out exact pull request source"))
        scanner = consumer.split("Run credential-free Dependency-Check", 1)[1]
        self.assertNotIn("${{ github.token }}", scanner)


if __name__ == "__main__":
    unittest.main()
