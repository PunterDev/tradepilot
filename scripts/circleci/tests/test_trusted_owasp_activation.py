from __future__ import annotations

import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DISPATCH = ROOT / "scripts/circleci/prepare_trusted_owasp_continuation.py"
SETUP = ROOT / ".circleci/trusted-owasp-control.yml"
CONTINUATION = ROOT / ".circleci/trusted-owasp-continuation.yml"


class TrustedActivationTests(unittest.TestCase):
    def env(self) -> dict[str, str]:
        head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
        return os.environ | {
            "TP_INTEGRATION_SOURCE": "github_app",
            "TP_CONFIG_REPOSITORY": "PunterDev/tradepilot",
            "TP_CONFIG_PATH": ".circleci/trusted-owasp-control.yml",
            "TP_CONFIG_REF": "refs/tags/trusted-owasp-control-v1",
            "TP_CONFIG_SHA": head,
            "TP_REPO_OWNER": "PunterDev",
            "TP_REPO_NAME": "tradepilot",
            "TP_CHECKOUT_BRANCH": "master",
            "TP_CHECKOUT_SHA": head,
            "TP_TRIGGER_TYPE": "github_app",
            "TP_EVENT_NAME": "push",
            "TP_GITHUB_PUSH_REF": "refs/heads/master",
            "TP_GITHUB_PUSH_AFTER": head,
        }

    def invoke(self, intent: dict, env: dict[str, str] | None = None):
        environment = env or self.env()
        with tempfile.TemporaryDirectory() as directory:
            intent_path, output = Path(directory) / "intent.json", Path(directory) / "parameters.json"
            intent_path.write_text(json.dumps(intent))
            result = subprocess.run(
                ["python3", str(DISPATCH), "--intent", str(intent_path), "--parameters", str(output), "--control-sha", environment.get("TP_CONFIG_SHA", ""), "--activation-sha", environment.get("TP_CHECKOUT_SHA", "")],
                cwd=ROOT, env=environment, text=True, capture_output=True, check=False,
            )
            return result, json.loads(output.read_text()) if output.exists() else None

    def test_native_github_app_push_accepts_an_inactive_intent(self):
        result, parameters = self.invoke({"status": "INACTIVE"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(parameters, {"run_protected": False})

    def test_rejects_wrong_or_missing_trigger_metadata(self):
        cases = (
            ("TP_TRIGGER_TYPE", "webhook"),
            ("TP_TRIGGER_TYPE", "api"),
            ("TP_EVENT_NAME", "pull_request"),
            ("TP_EVENT_NAME", "schedule"),
            ("TP_GITHUB_PUSH_REF", "refs/heads/untrusted"),
            ("TP_GITHUB_PUSH_AFTER", "a" * 40),
            ("TP_CONFIG_PATH", ".circleci/config.yml"),
            ("TP_CONFIG_SHA", "b" * 40),
            ("TP_CONFIG_REPOSITORY", "attacker/tradepilot"),
        )
        for key, value in cases:
            with self.subTest(key=key, value=value):
                env = self.env(); env[key] = value
                result, _ = self.invoke({"status": "INACTIVE"}, env)
                self.assertNotEqual(result.returncode, 0)
        env = self.env(); env.pop("TP_CONFIG_REF")
        result, _ = self.invoke({"status": "INACTIVE"}, env)
        self.assertNotEqual(result.returncode, 0)

    def test_rejects_ssh_rerun(self):
        env = self.env() | {"CIRCLE_SSH_CONNECTION": "synthetic"}
        result, _ = self.invoke({"status": "INACTIVE"}, env)
        self.assertNotEqual(result.returncode, 0)

    def test_ready_intent_cannot_replay_or_include_unrelated_changes(self):
        parent = self.env()["TP_CONFIG_SHA"]
        intent = {
            "status": "READY", "scope": "expanded-sixteen-dec15", "repository": "PunterDev/tradepilot",
            "controlCommit": parent, "candidate": {"sha256": "b" * 64},
            "approval": {"targetSha": "c" * 40, "pullRequest": 7, "expiresAt": "2026-12-15T23:59:59Z"},
        }
        result, _ = self.invoke(intent)
        self.assertNotEqual(result.returncode, 0)

    def test_ready_intent_requires_the_pinned_control_parent_and_exact_scope(self):
        control = self.env()["TP_CONFIG_SHA"]
        valid_shape = {
            "status": "READY", "scope": "expanded-sixteen-dec15", "repository": "PunterDev/tradepilot",
            "controlCommit": control, "candidate": {"sha256": "b" * 64},
            "approval": {"targetSha": "c" * 40, "pullRequest": 7, "expiresAt": "2026-12-15T23:59:59Z"},
        }
        for field, value in (("controlCommit", "d" * 40), ("scope", "historical-eight")):
            with self.subTest(field=field):
                intent = dict(valid_shape)
                intent[field] = value
                result, _ = self.invoke(intent)
                self.assertNotEqual(result.returncode, 0)

    def test_inactive_intent_never_enables_protected_continuation(self):
        inactive = {
            "status": "INACTIVE", "scope": "expanded-sixteen-dec15", "repository": "PunterDev/tradepilot",
            "controlCommit": "not-used", "candidate": {"sha256": "not-used"},
            "approval": {"targetSha": "not-used", "pullRequest": 0, "expiresAt": "not-used"},
        }
        result, parameters = self.invoke(inactive)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(parameters, {"run_protected": False})

    def test_setup_is_secretless_and_continuation_is_conditional(self):
        setup, continuation = SETUP.read_text(), CONTINUATION.read_text()
        self.assertIn("setup: true", setup)
        self.assertNotIn("context:", setup)
        self.assertIn("continuation/continue", setup)
        self.assertIn("when: << pipeline.parameters.run_protected >>", continuation)
        self.assertIn("context: tradepilot-approval-read", continuation)
        self.assertIn("context: tradepilot-nvd-refresh", continuation)
        analysis = continuation.split("secretless-dependency-check:", 1)[1].split("trusted-final-approval-validation:", 1)[0]
        self.assertNotIn("context:", analysis)

    def test_policy_b_documents_all_pre_job_requirements(self):
        document = (ROOT / "docs/security/CIRCLECI_TRUSTED_OWASP_ARCHITECTURE.md").read_text()
        for term in ("pipeline.trigger.type == \"github_app\"", "pipeline.event.github.ref == \"refs/heads/master\"", "pipeline.event.github.after == pipeline.git.revision", "not job.ssh.enabled", "CONTROL_SHA"):
            self.assertIn(term, document)

    def test_control_scripts_and_ready_data_are_loaded_from_their_separate_revisions(self):
        setup, continuation = SETUP.read_text(), CONTINUATION.read_text()
        self.assertIn('git archive "$TP_CONFIG_SHA" scripts/circleci', setup)
        self.assertIn('git show "$TP_CHECKOUT_SHA:docs/security/trusted-owasp-activation-intent.json"', setup)
        self.assertIn('git show "$TP_CONFIG_SHA:.circleci/trusted-owasp-continuation.yml"', setup)
        self.assertIn('git archive "$TP_CONFIG_SHA" scripts/circleci', continuation)
        self.assertIn('git show "$TP_CHECKOUT_SHA:docs/security/trusted-owasp-activation-intent.json"', continuation)
