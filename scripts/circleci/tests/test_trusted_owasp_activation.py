from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "scripts/circleci/validate_trusted_owasp_activation.py"
SHA = "a" * 40


class TrustedOwaspActivationTests(unittest.TestCase):
    def fixture_files(self, directory: Path) -> tuple[Path, Path, Path]:
        candidate = directory / "candidate.xml"
        candidate.write_text("<synthetic-candidate />\n")
        candidate_hash = hashlib.sha256(candidate.read_bytes()).hexdigest()
        approval = {
            "status": "APPROVED", "scope": "expanded-sixteen-dec15", "reviewedCommit": SHA,
            "expiry": "2026-12-15T23:59:59Z",
            "approvalEvidence": {
                "pullRequest": 42, "reviewedPullRequestHeadSha": SHA,
                "candidateSha256": candidate_hash,
            },
        }
        approval_path = directory / "approval.json"
        approval_path.write_text(json.dumps(approval))
        pr_path = directory / "pr.json"
        pr_path.write_text(json.dumps({
            "draft": False, "base": {"ref": "master"},
            "head": {"ref": "codex/security-consolidated", "sha": SHA},
        }))
        return approval_path, pr_path, candidate

    def run_validator(self, approval: Path, pr: Path, candidate: Path, *, sha: str = SHA, candidate_hash: str | None = None, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
        environment = os.environ | {"CIRCLE_BRANCH": "master", "CIRCLE_PROJECT_USERNAME": "PunterDev", "CIRCLE_PROJECT_REPONAME": "tradepilot"}
        candidate_hash = candidate_hash or hashlib.sha256(candidate.read_bytes()).hexdigest()
        if env:
            environment.update(env)
        return subprocess.run([
            "python3", str(VALIDATOR), "--target-pr", "42", "--target-sha", sha,
            "--candidate-sha256", candidate_hash, "--candidate", str(candidate),
            "--approval-record", str(approval), "--github-fixture", str(pr),
            "--as-of", "2026-12-01T00:00:00Z", "--output", str(approval.parent / "target.json"),
        ], env=environment, text=True, capture_output=True, check=False)

    def test_accepts_exact_approved_non_draft_target(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            approval, pr, candidate = self.fixture_files(Path(temporary))
            result = self.run_validator(approval, pr, candidate)
            self.assertEqual(0, result.returncode, result.stderr)

    def test_rejects_pr_and_approval_binding_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            approval, pr, candidate = self.fixture_files(directory)
            cases = (
                ("draft", lambda record: record.update(draft=True)),
                ("wrong-base", lambda record: record["base"].update(ref="develop")),
                ("wrong-source", lambda record: record["head"].update(ref="untrusted")),
                ("changed-head", lambda record: record["head"].update(sha="b" * 40)),
            )
            for _name, mutate in cases:
                record = json.loads(pr.read_text())
                mutate(record)
                pr.write_text(json.dumps(record))
                self.assertNotEqual(0, self.run_validator(approval, pr, candidate).returncode)
                self.fixture_files(directory)
            self.assertNotEqual(0, self.run_validator(approval, pr, candidate, candidate_hash="b" * 64).returncode)

    def test_rejects_expired_or_untrusted_control_environment(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            approval, pr, candidate = self.fixture_files(Path(temporary))
            self.assertNotEqual(0, self.run_validator(approval, pr, candidate, env={"CIRCLE_SSH_CONNECTION": "1"}).returncode)
            self.assertNotEqual(0, self.run_validator(approval, pr, candidate, env={"CIRCLE_CONFIG_SOURCE": "api"}).returncode)
            approval_data = json.loads(approval.read_text())
            approval_data["expiry"] = "2026-11-30T23:59:59Z"
            approval.write_text(json.dumps(approval_data))
            self.assertNotEqual(0, self.run_validator(approval, pr, candidate).returncode)

    def test_rejects_unapproved_records_and_wrong_repository(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            approval, pr, candidate = self.fixture_files(Path(temporary))
            approval_data = json.loads(approval.read_text())
            approval_data["status"] = "PENDING"
            approval.write_text(json.dumps(approval_data))
            self.assertNotEqual(0, self.run_validator(approval, pr, candidate).returncode)
            self.fixture_files(approval.parent)
            self.assertNotEqual(0, self.run_validator(
                approval, pr, candidate, env={"CIRCLE_PROJECT_USERNAME": "untrusted"},
            ).returncode)
