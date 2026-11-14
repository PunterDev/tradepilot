"""Regression checks for the credential-free GitHub security bootstrap."""

from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
WORKFLOW = ROOT / ".github/workflows/security-scans.yml"


class SecurityScansWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_only_prs_and_master_pushes_trigger_security_scans(self) -> None:
        self.assertRegex(self.workflow, r"(?m)^  pull_request:\s*$")
        self.assertRegex(self.workflow, r"(?m)^      - master\s*$")
        self.assertNotIn('"codex/**"', self.workflow)
        self.assertNotIn("- main", self.workflow)
        self.assertNotIn("workflow_dispatch", self.workflow)

    def test_pr_scans_are_concurrent_and_cancel_obsolete_runs(self) -> None:
        self.assertIn(
            "group: security-scans-${{ github.event.pull_request.number || github.ref }}",
            self.workflow,
        )
        self.assertIn("cancel-in-progress: true", self.workflow)

    def test_security_job_names_are_stable(self) -> None:
        for name in ("secret-scan", "dependency-scan", "frontend-audit", "sast"):
            self.assertRegex(self.workflow, rf"(?m)^  {name}:\n    name: {name}\s*$")

    def test_frontend_audit_is_independent_and_blocking(self) -> None:
        frontend_job = re.search(
            r"(?ms)^  frontend-audit:\n(?P<body>.*?)(?=^  [a-z][\w-]*:\n|\Z)",
            self.workflow,
        )
        self.assertIsNotNone(frontend_job)
        body = frontend_job.group("body")
        self.assertNotIn("needs:", body)
        self.assertIn("contents: read", body)
        self.assertIn('node-version: "22"', body)
        self.assertIn("cache-dependency-path: frontend/package-lock.json", body)
        self.assertIn("npm audit --audit-level=high", body)
        self.assertNotIn("continue-on-error", body)

    def test_backend_scan_remains_fail_closed_and_credential_free(self) -> None:
        self.assertIn("org.owasp:dependency-check-maven:12.2.2:check", self.workflow)
        self.assertIn("-DautoUpdate=false", self.workflow)
        self.assertIn("tradepilot-verified-nvd-data", self.workflow)
        self.assertIn("-DfailBuildOnCVSS=8", self.workflow)
        self.assertIn("-DfailOnError=true", self.workflow)
        self.assertNotIn("NVD_API_KEY", self.workflow)
        self.assertNotIn("nvdApiKey", self.workflow)
        self.assertNotIn("-DnvdApiKeyEnvironmentVariable", self.workflow)
        self.assertNotIn("continue-on-error", self.workflow)

    def test_gitleaks_and_semgrep_remain_independent_blocking_jobs(self) -> None:
        secret_job = re.search(
            r"(?ms)^  secret-scan:\n(?P<body>.*?)(?=^  [a-z][\w-]*:\n|\Z)",
            self.workflow,
        )
        sast_job = re.search(
            r"(?ms)^  sast:\n(?P<body>.*?)(?=^  [a-z][\w-]*:\n|\Z)",
            self.workflow,
        )
        self.assertIsNotNone(secret_job)
        self.assertIsNotNone(sast_job)
        self.assertIn("gitleaks/gitleaks-action@v2", secret_job.group("body"))
        self.assertIn("fetch-depth: 0", secret_job.group("body"))
        self.assertIn("semgrep scan", sast_job.group("body"))
        self.assertIn("--error", sast_job.group("body"))
        self.assertNotIn("needs:", secret_job.group("body"))
        self.assertNotIn("needs:", sast_job.group("body"))

    def test_draft_prs_are_not_skipped(self) -> None:
        self.assertNotIn("github.event.pull_request.draft", self.workflow)


if __name__ == "__main__":
    unittest.main()
