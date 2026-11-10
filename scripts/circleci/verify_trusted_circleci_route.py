#!/usr/bin/env python3
"""Fail-closed verifier for the master-controlled CircleCI context route."""

from __future__ import annotations

import argparse
import re
from dataclasses import dataclass


SHA = re.compile(r"[0-9a-f]{40}")


class RouteRejected(ValueError):
    pass


@dataclass(frozen=True)
class RouteMetadata:
    integration_source: str
    trigger_type: str
    event_name: str
    repository: str
    config_repository: str
    config_path: str
    config_ref: str
    config_sha: str
    checkout_branch: str
    checkout_sha: str
    checkout_head: str
    ssh_enabled: bool


@dataclass(frozen=True)
class RoutePolicy:
    """A fixed, master-controlled route that is eligible to use one context."""

    config_path: str
    allow_manual_api: bool
    requires_approval_binding: bool


CANARY_POLICY = RoutePolicy(
    config_path=".circleci/trusted-context-isolation-canary.yml",
    allow_manual_api=True,
    requires_approval_binding=False,
)

PRIVILEGED_POLICY = RoutePolicy(
    config_path=".circleci/trusted-owasp-control.yml",
    allow_manual_api=False,
    requires_approval_binding=True,
)


def validate(
    metadata: RouteMetadata,
    expected_control_sha: str,
    approval_control_sha: str | None,
    policy: RoutePolicy = PRIVILEGED_POLICY,
) -> None:
    """Require a documented master control route before a context job may proceed."""
    values = vars(metadata) | {"expected_control_sha": expected_control_sha}
    if policy.requires_approval_binding:
        values["approval_control_sha"] = approval_control_sha
    if any(not value for value in values.values() if isinstance(value, str)):
        raise RouteRejected("required metadata is missing")
    if metadata.integration_source != "github_app":
        raise RouteRejected("integration is not the GitHub App")
    if policy.allow_manual_api:
        if metadata.trigger_type != "api" or metadata.event_name != "api":
            raise RouteRejected("canary must use the known manual API route")
    elif metadata.trigger_type != "github_app" or metadata.event_name != "push":
        raise RouteRejected("privileged execution must use a GitHub App push event")
    if metadata.repository != "PunterDev/tradepilot":
        raise RouteRejected("wrong CircleCI project")
    if metadata.config_repository != "PunterDev/tradepilot":
        raise RouteRejected("wrong configuration repository")
    if metadata.config_path != policy.config_path:
        raise RouteRejected("wrong configuration path")
    if metadata.config_ref != "refs/heads/master":
        raise RouteRejected("configuration is not sourced from master")
    if metadata.checkout_branch != "master":
        raise RouteRejected("checkout is not master")
    if metadata.ssh_enabled:
        raise RouteRejected("SSH reruns are not trusted")
    for name, value in {
        "configuration SHA": metadata.config_sha,
        "checkout SHA": metadata.checkout_sha,
        "checked-out HEAD": metadata.checkout_head,
        "expected control SHA": expected_control_sha,
    }.items():
        if not SHA.fullmatch(value):
            raise RouteRejected(f"{name} is invalid")
    commits = {metadata.config_sha, metadata.checkout_sha, metadata.checkout_head, expected_control_sha}
    if policy.requires_approval_binding:
        if approval_control_sha is None or not SHA.fullmatch(approval_control_sha):
            raise RouteRejected("approval control SHA is invalid")
        commits.add(approval_control_sha)
    if len(commits) != 1:
        raise RouteRejected("control and checkout commits do not match")


def main() -> None:
    parser = argparse.ArgumentParser()
    for name in (
        "integration-source", "trigger-type", "event-name", "repository", "config-repository",
        "config-path", "config-ref", "config-sha", "checkout-branch", "checkout-sha",
        "checkout-head", "expected-control-sha",
    ):
        parser.add_argument(f"--{name}", required=True)
    parser.add_argument("--approval-control-sha")
    parser.add_argument("--policy", choices=("canary", "privileged"), default="privileged")
    parser.add_argument("--ssh-enabled", action="store_true")
    args = parser.parse_args()
    metadata = RouteMetadata(
        integration_source=args.integration_source,
        trigger_type=args.trigger_type,
        event_name=args.event_name,
        repository=args.repository,
        config_repository=args.config_repository,
        config_path=args.config_path,
        config_ref=args.config_ref,
        config_sha=args.config_sha,
        checkout_branch=args.checkout_branch,
        checkout_sha=args.checkout_sha,
        checkout_head=args.checkout_head,
        ssh_enabled=args.ssh_enabled,
    )
    policy = CANARY_POLICY if args.policy == "canary" else PRIVILEGED_POLICY
    try:
        validate(metadata, args.expected_control_sha, args.approval_control_sha, policy)
    except RouteRejected as error:
        raise SystemExit(f"trusted CircleCI route rejected: {error}") from error
    print("trusted CircleCI route verified")


if __name__ == "__main__":
    main()
