#!/usr/bin/env python3
"""Opt-in end-to-end test for a published trusted NVD package.

This test deliberately requires explicit local inputs. It is not suitable for
ordinary unit-test execution because it verifies a real GitHub attestation and
scans against a real, short-lived NVD archive.
"""

from __future__ import annotations

import hashlib
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
ARCHIVE_TOOL = ROOT / "scripts/security/trusted_nvd_archive.py"
REPOSITORY = "PunterDev/tradepilot"
WORKFLOW = ".github/workflows/publish-verified-nvd.yml"
SIGNER = "PunterDev/tradepilot/.github/workflows/publish-verified-nvd.yml"


@unittest.skipUnless(os.environ.get("TRADEPILOT_NVD_PUBLISHER_PACKAGE"), "real publisher package not provided")
class TrustedNvdWorkflowIntegrationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.package = Path(os.environ["TRADEPILOT_NVD_PUBLISHER_PACKAGE"])
        self.source_revision = os.environ["TRADEPILOT_NVD_SOURCE_REVISION"]
        self.run_id = os.environ["TRADEPILOT_NVD_WORKFLOW_RUN_ID"]
        self.package_sha256 = os.environ["TRADEPILOT_NVD_PACKAGE_SHA256"]
        self.expected_scan_status = int(os.environ.get("TRADEPILOT_NVD_EXPECT_SCAN_STATUS", "0"))
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def invoke(self, *arguments: str, success: bool = True) -> subprocess.CompletedProcess[str]:
        result = subprocess.run([sys.executable, str(ARCHIVE_TOOL), *arguments], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0 if success else 1, result.stderr)
        return result

    def test_published_artifact_reaches_credential_free_scanner(self) -> None:
        self.assertTrue(self.package.is_file())
        self.assertEqual(hashlib.sha256(self.package.read_bytes()).hexdigest(), self.package_sha256)
        extracted = self.root / "publisher"
        self.invoke("extract-publisher-package", "--archive", str(self.package), "--destination", str(extracted))
        archive = extracted / "dependency-check-nvd.tar.gz"
        manifest = extracted / "dependency-check-nvd.manifest.json"
        attestation = subprocess.run(
            [
                "gh", "attestation", "verify", str(archive), "--repo", REPOSITORY,
                "--signer-workflow", SIGNER, "--source-ref", "refs/heads/master",
                "--source-digest", self.source_revision, "--deny-self-hosted-runners",
            ],
            capture_output=True,
            text=True,
        )
        self.assertEqual(attestation.returncode, 0, attestation.stderr)
        verified = self.root / "verified-nvd-input"
        self.invoke(
            "verify", "--archive", str(archive), "--manifest", str(manifest), "--destination", str(verified),
            "--repository", REPOSITORY, "--publisher-workflow", WORKFLOW, "--max-age-hours", "24",
            "--expected-source-revision", self.source_revision, "--expected-workflow-run-id", self.run_id,
        )
        self.assertGreater((verified / "data/odc.mv.db").stat().st_size, 1024 * 1024)

        reports = self.root / "reports"
        environment = os.environ | {"NVD_API_KEY": "", "GITHUB_TOKEN": "", "GH_TOKEN": ""}
        scan = subprocess.run(
            [
                "mvn", "-B", "org.owasp:dependency-check-maven:12.2.2:check", "-DautoUpdate=false",
                f"-DdataDirectory={verified / 'data'}", "-Dformats=HTML,JSON,SARIF",
                f"-Dodc.outputDirectory={reports}", "-DfailOnError=true", "-DfailBuildOnCVSS=8",
                "-DfailBuildOnUnusedSuppressionRule=true",
            ],
            cwd=ROOT / "backend",
            env=environment,
            capture_output=True,
            text=True,
        )
        self.assertEqual(scan.returncode, self.expected_scan_status, scan.stderr)
        for suffix in ("html", "json", "sarif"):
            self.assertTrue((reports / f"dependency-check-report.{suffix}").is_file())


if __name__ == "__main__":
    unittest.main()
