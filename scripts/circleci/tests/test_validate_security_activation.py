from __future__ import annotations

import importlib.util
import os
import subprocess
import unittest
from pathlib import Path
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[3]
MODULE_PATH = ROOT / "scripts/circleci/validate_security_activation.py"
SPEC = importlib.util.spec_from_file_location("circleci_gate", MODULE_PATH)
assert SPEC and SPEC.loader
GATE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATE)


class CircleCiActivationGateTests(unittest.TestCase):
    def test_extracts_pull_request_number(self) -> None:
        self.assertEqual(7, GATE.pull_request_number("https://github.com/PunterDev/tradepilot/pull/7"))

    def test_rejects_missing_or_invalid_pull_request_url(self) -> None:
        with self.assertRaises(GATE.GateError):
            GATE.pull_request_number("")
        with self.assertRaises(GATE.GateError):
            GATE.pull_request_number("https://github.com/PunterDev/tradepilot/issues/7")

    def test_rejects_missing_read_only_github_token_before_network(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(GATE.GateError, "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN"):
                GATE.main()

    def test_rejects_missing_pull_request_metadata(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_SHA1": "a" * 40,
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True):
            with self.assertRaisesRegex(GATE.GateError, "CIRCLE_PULL_REQUEST"):
                GATE.main()

    def test_rejects_missing_or_nonimmutable_build_sha(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_PULL_REQUEST": "https://github.com/PunterDev/tradepilot/pull/7",
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True):
            with self.assertRaisesRegex(GATE.GateError, "CIRCLE_SHA1"):
                GATE.main()

    def test_rejects_draft_pull_request(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_PULL_REQUEST": "https://github.com/PunterDev/tradepilot/pull/7",
            "CIRCLE_SHA1": "a" * 40,
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True), patch.object(
            GATE, "request_json", return_value={"draft": True, "base": {"ref": "master"}, "head": {"sha": "a" * 40}}
        ):
            with self.assertRaisesRegex(GATE.GateError, "not a draft"):
                GATE.main()

    def test_rejects_pr_head_that_does_not_match_build(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_PULL_REQUEST": "https://github.com/PunterDev/tradepilot/pull/7",
            "CIRCLE_SHA1": "a" * 40,
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True), patch.object(
            GATE, "request_json", return_value={"draft": False, "base": {"ref": "master"}, "head": {"sha": "b" * 40}}
        ):
            with self.assertRaisesRegex(GATE.GateError, "current pull-request head"):
                GATE.main()

    def test_rejects_non_master_base_before_running_approval_validator(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_PULL_REQUEST": "https://github.com/PunterDev/tradepilot/pull/7",
            "CIRCLE_SHA1": "a" * 40,
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True), patch.object(
            GATE, "request_json", return_value={"draft": False, "base": {"ref": "feature"}, "head": {"sha": "a" * 40}}
        ):
            with self.assertRaisesRegex(GATE.GateError, "targeting master"):
                GATE.main()

    def test_ready_master_pr_invokes_existing_github_approval_validator(self) -> None:
        environment = {
            "TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN": "masked-test-token",
            "CIRCLE_PULL_REQUEST": "https://github.com/PunterDev/tradepilot/pull/7",
            "CIRCLE_SHA1": "a" * 40,
            "CIRCLE_PROJECT_USERNAME": "PunterDev",
            "CIRCLE_PROJECT_REPONAME": "tradepilot",
        }
        with patch.dict(os.environ, environment, clear=True), patch.object(
            GATE, "request_json", return_value={"draft": False, "base": {"ref": "master"}, "head": {"sha": "a" * 40}}
        ), patch.object(
            GATE.subprocess, "run", return_value=subprocess.CompletedProcess([], 0)
        ) as run:
            self.assertEqual(0, GATE.main())
        self.assertIn("--verify-github-approvals", run.call_args.args[0])
        self.assertEqual("masked-test-token", run.call_args.kwargs["env"]["GITHUB_TOKEN"])
