#!/usr/bin/env python3
"""Fail-closed CircleCI adapter for the existing GitHub approval validator.

CircleCI does not provide the GitHub Actions token used by the repository's
existing validator. This adapter requires a separately provisioned, read-only
GitHub API token and rejects an absent token, missing PR metadata, draft PR,
wrong base branch, or build SHA that does not match the current PR head.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / "scripts/security/validate-owasp-exception-candidates.py"
SUPPRESSION = ROOT / "docs/security/candidates/dependency-check-final-eight-candidate.xml"
APPROVAL = ROOT / "docs/security/candidates/owasp-final-eight-approval.json"
EXPECTED_REPOSITORY = "PunterDev/tradepilot"


class GateError(RuntimeError):
    pass


def request_json(url: str, token: str) -> dict:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.load(response)
    except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError) as error:
        raise GateError(f"cannot verify pull request state through GitHub API: {error}") from error


def pull_request_number(value: str) -> int:
    match = re.search(r"/pull/(\d+)(?:$|[/?#])", value)
    if not match:
        raise GateError("CIRCLE_PULL_REQUEST must contain the current GitHub pull-request URL")
    return int(match.group(1))


def main() -> int:
    token = os.environ.get("TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN")
    pr_url = os.environ.get("CIRCLE_PULL_REQUEST", "")
    build_sha = os.environ.get("CIRCLE_SHA1", "")
    repository = os.environ.get("CIRCLE_PROJECT_USERNAME", "") + "/" + os.environ.get("CIRCLE_PROJECT_REPONAME", "")

    if not token:
        raise GateError("TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN is required for activation")
    if repository != EXPECTED_REPOSITORY:
        raise GateError(f"CircleCI project must be {EXPECTED_REPOSITORY}; received {repository or '<missing>'}")
    if not re.fullmatch(r"[0-9a-f]{40}", build_sha):
        raise GateError("CIRCLE_SHA1 must be the immutable 40-character commit SHA")

    number = pull_request_number(pr_url)
    pr = request_json(f"https://api.github.com/repos/{EXPECTED_REPOSITORY}/pulls/{number}", token)
    if pr.get("draft") is not False:
        raise GateError("activation requires a GitHub pull request that is not a draft")
    if pr.get("base", {}).get("ref") != "master":
        raise GateError("activation requires a pull request targeting master")
    if pr.get("head", {}).get("sha") != build_sha:
        raise GateError("CircleCI build SHA is not the current pull-request head")

    environment = os.environ.copy()
    environment["GITHUB_TOKEN"] = token
    result = subprocess.run(
        [
            sys.executable,
            str(VALIDATOR),
            "--suppression",
            str(SUPPRESSION),
            "--approval-record",
            str(APPROVAL),
            "--mode",
            "activation",
            "--verify-github-approvals",
        ],
        cwd=ROOT,
        env=environment,
        check=False,
    )
    return result.returncode


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"security activation gate failed: {error}", file=sys.stderr)
        raise SystemExit(1)
