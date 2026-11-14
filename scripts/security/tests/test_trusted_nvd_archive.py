#!/usr/bin/env python3
"""Regression tests for the trusted NVD archive format and extraction guard."""

from __future__ import annotations

import hashlib
import json
import os
import re
import subprocess
import sys
import tarfile
import tempfile
import unittest
import zipfile
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
        self.data = self.root / "nvd-data"
        self.data.mkdir()
        (self.data / "odc.mv.db").write_bytes(os.urandom(1024 * 1024 + 1))
        (self.data / "cache").mkdir()
        (self.data / "cache/CENTRAL.data").write_bytes(b"synthetic-cache")
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

    def test_custom_source_directory_uses_logical_data_prefix(self) -> None:
        manifest = json.loads(self.manifest.read_text())
        expected = {"data/odc.mv.db", "data/cache/CENTRAL.data"}
        self.assertEqual({item["path"] for item in manifest["files"]}, expected)
        with tarfile.open(self.archive, "r:gz") as archive:
            self.assertEqual(set(archive.getnames()), expected)
        self.verify()
        self.assertTrue((self.root / "out/data/cache/CENTRAL.data").is_file())

    def test_missing_archive_member_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            archive.add(self.data / "odc.mv.db", arcname="data/odc.mv.db")
        self.rewrite_manifest(
            archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(),
            archiveSize=self.archive.stat().st_size,
        )
        self.verify(success=False)

    def test_non_regular_source_file_fails_closed(self) -> None:
        cache = self.data / "cache/CENTRAL.data"
        cache.unlink()
        cache.symlink_to(self.data / "odc.mv.db")
        self.invoke(
            "build", "--data-dir", self.data, "--archive", self.root / "invalid.tar.gz",
            "--manifest", self.root / "invalid.json", "--repository", REPOSITORY,
            "--publisher-workflow", WORKFLOW, "--source-revision", REVISION,
            "--workflow-run-id", "123", "--max-age-hours", "24", success=False,
        )

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

    def test_absolute_tar_path_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            payload = self.root / "payload"
            payload.write_text("bad")
            archive.add(payload, arcname="/data/odc.mv.db")
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

    def test_duplicate_tar_entry_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            payload = self.root / "payload"
            payload.write_bytes(b"x" * (1024 * 1024 + 1))
            archive.add(payload, arcname="data/odc.mv.db")
            archive.add(payload, arcname="data/odc.mv.db")
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_special_tar_entry_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            info = tarfile.TarInfo("data/odc.mv.db")
            info.type = tarfile.CHRTYPE
            archive.addfile(info)
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_truncated_archive_fails_closed(self) -> None:
        self.archive.write_bytes(self.archive.read_bytes()[:20])
        self.verify(success=False)

    def test_incorrect_member_size_fails_closed(self) -> None:
        with tarfile.open(self.archive, "w:gz") as archive:
            payload = self.root / "payload"
            payload.write_bytes(b"short")
            archive.add(payload, arcname="data/odc.mv.db")
        self.rewrite_manifest(archiveSha256=hashlib.sha256(self.archive.read_bytes()).hexdigest(), archiveSize=self.archive.stat().st_size)
        self.verify(success=False)

    def test_duplicate_manifest_entry_fails_closed(self) -> None:
        manifest = json.loads(self.manifest.read_text())
        manifest["files"].append(dict(manifest["files"][0]))
        self.manifest.write_text(json.dumps(manifest))
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

    def publisher_package(self, entries: list[tuple[str, bytes]]) -> Path:
        package = self.root / "publisher.zip"
        with zipfile.ZipFile(package, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, content in entries:
                archive.writestr(name, content)
        return package

    def extract_publisher_package(self, package: Path, success: bool = True) -> None:
        self.invoke("extract-publisher-package", "--archive", package, "--destination", self.root / "publisher-out", success=success)

    def validate_pr(self, payload: dict[str, object], success: bool = True, **overrides: str) -> None:
        metadata = self.root / "pr.json"
        metadata.write_text(json.dumps(payload))
        arguments: list[object] = [
            "validate-pr", "--metadata", metadata, "--repository", REPOSITORY,
            "--expected-number", overrides.get("number", "17"),
            "--expected-head", overrides.get("head", REVISION),
        ]
        if "event_head" in overrides:
            arguments.extend(["--event-head", overrides["event_head"]])
        self.invoke(*arguments, success=success)

    @staticmethod
    def pr_payload() -> dict[str, object]:
        return {
            "number": 17, "state": "open",
            "base": {"ref": "master", "repo": {"full_name": REPOSITORY}},
            "head": {"sha": REVISION, "repo": {"full_name": REPOSITORY}},
        }

    def test_current_internal_master_pr_is_accepted(self) -> None:
        self.validate_pr(self.pr_payload(), event_head=REVISION)

    def test_event_payload_head_mismatch_fails_closed(self) -> None:
        self.validate_pr(self.pr_payload(), success=False, event_head="b" * 40)

    def test_pr_head_change_before_or_during_scan_fails_closed(self) -> None:
        payload = self.pr_payload()
        payload["head"] = {"sha": "b" * 40, "repo": {"full_name": REPOSITORY}}
        self.validate_pr(payload, success=False)

    def test_pr_head_change_after_scan_fails_closed(self) -> None:
        payload = self.pr_payload()
        payload["head"] = {"sha": "c" * 40, "repo": {"full_name": REPOSITORY}}
        self.validate_pr(payload, success=False)

    def test_manual_dispatch_unexpected_commit_fails_closed(self) -> None:
        self.validate_pr(self.pr_payload(), success=False, head="b" * 40)

    def test_closed_or_retargeted_pr_fails_closed(self) -> None:
        closed = self.pr_payload()
        closed["state"] = "closed"
        self.validate_pr(closed, success=False)
        retargeted = self.pr_payload()
        retargeted["base"] = {"ref": "main", "repo": {"full_name": REPOSITORY}}
        self.validate_pr(retargeted, success=False)

    def test_external_pr_repository_fails_closed(self) -> None:
        payload = self.pr_payload()
        payload["head"] = {"sha": REVISION, "repo": {"full_name": "other/tradepilot"}}
        self.validate_pr(payload, success=False)

    def test_valid_publisher_package_extracts(self) -> None:
        package = self.publisher_package([
            ("dependency-check-nvd.tar.gz", self.archive.read_bytes()),
            ("dependency-check-nvd.manifest.json", self.manifest.read_bytes()),
        ])
        self.extract_publisher_package(package)
        self.assertTrue((self.root / "publisher-out/dependency-check-nvd.tar.gz").is_file())

    def test_duplicate_zip_entry_fails_closed(self) -> None:
        package = self.publisher_package([
            ("dependency-check-nvd.tar.gz", b"one"),
            ("dependency-check-nvd.tar.gz", b"two"),
            ("dependency-check-nvd.manifest.json", b"{}"),
        ])
        self.extract_publisher_package(package, success=False)

    def test_unsafe_zip_path_fails_closed(self) -> None:
        package = self.publisher_package([
            ("../dependency-check-nvd.tar.gz", b"x"),
            ("dependency-check-nvd.manifest.json", b"{}"),
        ])
        self.extract_publisher_package(package, success=False)

    def test_symlink_zip_entry_fails_closed(self) -> None:
        package = self.root / "publisher.zip"
        link = zipfile.ZipInfo("dependency-check-nvd.tar.gz")
        link.external_attr = 0o120777 << 16
        with zipfile.ZipFile(package, "w") as archive:
            archive.writestr(link, "target")
            archive.writestr("dependency-check-nvd.manifest.json", "{}")
        self.extract_publisher_package(package, success=False)

    def test_unexpected_zip_file_fails_closed(self) -> None:
        package = self.publisher_package([
            ("dependency-check-nvd.tar.gz", b"x"),
            ("dependency-check-nvd.manifest.json", b"{}"),
            ("extra", b"x"),
        ])
        self.extract_publisher_package(package, success=False)


class WorkflowBoundaryTests(unittest.TestCase):
    WORKFLOW = ROOT / ".github/workflows/trusted-pr-dependency-check.yml"
    JQ_EXPRESSION = re.compile(r"(?:--jq|jq -r)\s+(?:'([^']+)'|(\.[A-Za-z0-9_.]+))")

    def workflow_jq_expressions(self) -> list[str]:
        return [quoted or bare for quoted, bare in self.JQ_EXPRESSION.findall(self.WORKFLOW.read_text())]

    @staticmethod
    def run_jq(expression: str, payload: object) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["jq", "-er", expression], input=json.dumps(payload), capture_output=True, text=True,
        )

    def test_all_workflow_jq_expressions_are_extracted_and_executable(self) -> None:
        expressions = self.workflow_jq_expressions()
        self.assertEqual(len(expressions), 18)
        self.assertEqual(sum("workflow_runs" in expression for expression in expressions), 1)
        self.assertEqual(sum("artifacts" in expression for expression in expressions), 1)
        self.assertGreaterEqual(sum(expression.startswith(".head") for expression in expressions), 3)
        self.assertGreaterEqual(sum(expression.startswith(".base") for expression in expressions), 2)
        payload = {
            "id": 2,
            "workflow_runs": [
                {"id": 1, "conclusion": "failure", "event": "workflow_dispatch"},
                {"id": 2, "conclusion": "success", "event": "workflow_dispatch"},
            ],
            "artifacts": [
                {"id": 3, "expired": True, "name": "dependency-check-nvd-expired"},
                {"id": 4, "expired": False, "name": "other-artifact"},
                {"id": 5, "expired": False, "name": "dependency-check-nvd-verified"},
            ],
            "repository": {"full_name": REPOSITORY},
            "workflow_id": 2,
            "status": "completed",
            "conclusion": "success",
            "head_branch": "master",
            "head_sha": REVISION,
            "digest": "sha256:" + "b" * 64,
            "workflow_run": {"id": 2},
            "state": "open",
            "base": {"ref": "master", "repo": {"full_name": REPOSITORY}},
            "head": {"sha": REVISION, "repo": {"full_name": REPOSITORY}},
        }
        for expression in expressions:
            result = self.run_jq(expression, payload)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotEqual(result.stdout.strip(), "")

    def test_artifact_filter_selects_only_a_current_trusted_package(self) -> None:
        expression = next(expression for expression in self.workflow_jq_expressions() if ".artifacts[]" in expression)
        result = self.run_jq(expression, {"artifacts": [
            {"id": 1, "expired": True, "name": "dependency-check-nvd-expired"},
            {"id": 2, "expired": False, "name": "not-nvd"},
            {"id": 3, "expired": False, "name": "dependency-check-nvd-verified"},
        ]})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout), {
            "id": 3, "expired": False, "name": "dependency-check-nvd-verified",
        })

    def test_artifact_filter_rejects_no_match_expired_and_malformed_payloads(self) -> None:
        expression = next(expression for expression in self.workflow_jq_expressions() if ".artifacts[]" in expression)
        for payload in (
            {"artifacts": []},
            {"artifacts": [{"id": 1, "expired": True, "name": "dependency-check-nvd-expired"}]},
            {},
        ):
            result = self.run_jq(expression, payload)
            self.assertNotEqual(result.returncode, 0)

    def test_publisher_and_consumer_boundaries_are_explicit(self) -> None:
        publisher = (ROOT / ".github/workflows/publish-verified-nvd.yml").read_text()
        consumer = (ROOT / ".github/workflows/trusted-pr-dependency-check.yml").read_text()
        archive_tool = SCRIPT.read_text()
        self.assertIn("environment: trusted-nvd-publisher", publisher)
        self.assertIn("refresh-and-build", publisher)
        self.assertNotIn("pull_request:", publisher)
        self.assertIn("workflow_dispatch:", publisher)
        self.assertNotIn("schedule:", publisher)
        self.assertIn("pull_request_target:", consumer)
        self.assertIn("Check out protected verifier source", consumer)
        self.assertIn("Bind the protected verifier source", consumer)
        self.assertIn("trusted-nvd-archive-verifier.py", consumer)
        self.assertIn("expected-workflow-run-id", consumer)
        self.assertIn("expected-source-revision", consumer)
        self.assertIn("pull_request_head", consumer)
        self.assertIn("Revalidate the exact PR head before checkout", consumer)
        self.assertIn("publish-commit-specific-check", consumer)
        self.assertIn("checks: write", consumer)
        self.assertIn("name=trusted-dependency-check", consumer)
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
        self.assertNotIn("extractall", consumer)
        self.assertNotIn("-DnvdApiKey=", archive_tool)
        self.assertIn("-DnvdApiKeyEnvironmentVariable=NVD_API_KEY", archive_tool)
        for pin in (
            "11d5960a326750d5838078e36cf38b85af677262",
            "cf277c60eb25467037889841efdb72551f06f6c3",
            "ea165f8d65b6e75b540449e92b4886f43607fa02",
            "d3f86a106a0bac45b974a628896c90dbdf5c8093",
            "e8998f949152b193b063cb0ec769d69d929409be",
        ):
            self.assertIn(pin, publisher + consumer)
        self.assertLess(consumer.index("Download and verify trusted NVD package"), consumer.index("Check out exact pull request source"))
        scanner = consumer.split("Run credential-free Dependency-Check", 1)[1].split("publish-commit-specific-check", 1)[0]
        self.assertNotIn("${{ github.token }}", scanner)
        self.assertNotIn("checks: write", scanner)
        self.assertIn("Preserve the real scan result", scanner)
        publisher_job = consumer.split("publish-commit-specific-check", 1)[1]
        self.assertNotIn("actions/checkout", publisher_job)
        self.assertIn("checks: write", publisher_job)
        self.assertIn("test \"$conclusion\" = success", publisher_job)


if __name__ == "__main__":
    unittest.main()
