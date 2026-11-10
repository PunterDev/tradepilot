from __future__ import annotations

import unittest

from scripts.circleci.verify_trusted_circleci_route import (
    CANARY_POLICY,
    PRIVILEGED_POLICY,
    RouteMetadata,
    RouteRejected,
    validate,
)


SHA = "a" * 40


def trusted_route(**overrides: object) -> RouteMetadata:
    values: dict[str, object] = {
        "integration_source": "github_app",
        "trigger_type": "github_app",
        "event_name": "push",
        "repository": "PunterDev/tradepilot",
        "config_repository": "PunterDev/tradepilot",
        "config_path": ".circleci/trusted-owasp-control.yml",
        "config_ref": "refs/heads/master",
        "config_sha": SHA,
        "checkout_branch": "master",
        "checkout_sha": SHA,
        "checkout_head": SHA,
        "ssh_enabled": False,
    }
    values.update(overrides)
    return RouteMetadata(**values)  # type: ignore[arg-type]


class TrustedCircleCiRouteTests(unittest.TestCase):
    def test_accepts_only_the_complete_trusted_route(self) -> None:
        validate(trusted_route(), SHA, SHA, PRIVILEGED_POLICY)

    def test_canary_allows_only_the_known_manual_api_route(self) -> None:
        validate(
            trusted_route(
                trigger_type="api",
                event_name="api",
                config_path=CANARY_POLICY.config_path,
            ),
            SHA,
            None,
            CANARY_POLICY,
        )

    def test_canary_rejects_non_api_execution(self) -> None:
        with self.assertRaises(RouteRejected):
            validate(
                trusted_route(config_path=CANARY_POLICY.config_path),
                SHA,
                None,
                CANARY_POLICY,
            )

    def test_canary_rejects_context_isolation_negative_controls(self) -> None:
        cases: tuple[dict[str, object], ...] = (
            ({"repository": "other/project"},),
            ({"config_path": ".circleci/config.yml"},),
            ({"config_sha": "b" * 40},),
            ({"checkout_branch": "feature"},),
            ({"ssh_enabled": True},),
            ({"config_repository": ""},),
        )
        for (overrides,) in cases:
            with self.subTest(overrides=overrides):
                with self.assertRaises(RouteRejected):
                    canary_overrides = {
                        "trigger_type": "api",
                        "event_name": "api",
                        "config_path": CANARY_POLICY.config_path,
                    }
                    canary_overrides.update(overrides)
                    validate(
                        trusted_route(**canary_overrides),
                        SHA,
                        None,
                        CANARY_POLICY,
                    )

    def test_rejects_every_required_negative_control(self) -> None:
        cases: tuple[dict[str, object], str, str] = (
            ({"repository": "other/project"}, SHA, SHA),
            ({"config_repository": "other/project"}, SHA, SHA),
            ({"config_path": ".circleci/config.yml"}, SHA, SHA),
            ({"config_ref": "refs/heads/feature"}, SHA, SHA),
            ({"config_sha": "b" * 40}, SHA, SHA),
            ({"checkout_branch": "feature"}, SHA, SHA),
            ({"checkout_sha": "b" * 40}, SHA, SHA),
            ({"checkout_head": "b" * 40}, SHA, SHA),
            ({"trigger_type": "api", "event_name": "api"}, SHA, SHA),
            ({"event_name": "pull_request"}, SHA, SHA),
            ({"trigger_type": "schedule", "event_name": "schedule"}, SHA, SHA),
            ({"ssh_enabled": True}, SHA, SHA),
            ({"repository": ""}, SHA, SHA),
            ({"config_path": ""}, SHA, SHA),
            ({}, "b" * 40, SHA),
        )
        for overrides, expected_control_sha, approval_control_sha in cases:
            with self.subTest(overrides=overrides):
                with self.assertRaises(RouteRejected):
                    validate(trusted_route(**overrides), expected_control_sha, approval_control_sha, PRIVILEGED_POLICY)

    def test_rejects_an_approval_for_an_outdated_control_commit(self) -> None:
        with self.assertRaises(RouteRejected):
            validate(trusted_route(), SHA, "b" * 40, PRIVILEGED_POLICY)

    def test_privileged_policy_requires_approval_binding(self) -> None:
        with self.assertRaises(RouteRejected):
            validate(trusted_route(), SHA, None, PRIVILEGED_POLICY)
