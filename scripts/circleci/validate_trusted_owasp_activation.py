#!/usr/bin/env python3
"""Fail-closed validation for the protected CircleCI OWASP control pipeline."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
from datetime import UTC, datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPOSITORY = "PunterDev/tradepilot"
SOURCE_BRANCH = "codex/security-consolidated"
SCOPE = "expanded-sixteen-dec15"
EXPIRY = "2026-12-15T23:59:59Z"
DECEMBER_CANDIDATE = ROOT / "docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml"
DECEMBER_APPROVAL = ROOT / "docs/security/candidates/owasp-expanded-sixteen-dec15-approval.template.json"


def fail(reason: str) -> None:
    raise SystemExit(f"trusted OWASP activation rejected: {reason}")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def expected_environment() -> None:
    if os.environ.get("CIRCLE_PROJECT_USERNAME") not in (None, "PunterDev"):
        fail("wrong repository owner")
    if os.environ.get("CIRCLE_PROJECT_REPONAME") not in (None, "tradepilot"):
        fail("wrong repository name")
    if os.environ.get("CIRCLE_BRANCH") != "master":
        fail("control pipeline must run from master")
    if os.environ.get("CIRCLE_SSH_CONNECTION"):
        fail("SSH reruns are not trusted")
    if os.environ.get("CIRCLE_CONFIG_SOURCE", "").startswith("api"):
        fail("API-supplied configuration is not trusted")


def read_pr(pr_number: int, fixture: Path | None) -> dict[str, object]:
    if fixture:
        return json.loads(fixture.read_text())
    fail("a fixture is required for local synthetic validation")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-pr", type=int, required=True)
    parser.add_argument("--target-sha", required=True)
    parser.add_argument("--candidate-sha256", required=True)
    parser.add_argument("--github-fixture", type=Path)
    parser.add_argument("--approval-record", type=Path, default=DECEMBER_APPROVAL)
    parser.add_argument("--candidate", type=Path, default=DECEMBER_CANDIDATE)
    parser.add_argument("--as-of")
    parser.add_argument("--output", type=Path, default=Path("/tmp/approved-target.json"))
    args = parser.parse_args()
    expected_environment()
    if not re.fullmatch(r"[0-9a-f]{40}", args.target_sha):
        fail("target SHA is invalid")
    if not re.fullmatch(r"[0-9a-f]{64}", args.candidate_sha256):
        fail("candidate SHA-256 is invalid")
    if not args.candidate.is_file() or sha256(args.candidate) != args.candidate_sha256:
        fail("candidate file hash does not match requested scope")

    approval = json.loads(args.approval_record.read_text())
    if approval.get("status") != "APPROVED":
        fail("approval record is not approved")
    if approval.get("scope") != SCOPE:
        fail("approval does not bind the requested candidate")
    if approval.get("reviewedCommit") != args.target_sha or approval.get("expiry") != EXPIRY:
        fail("approval does not bind the requested commit and expiry")
    now = datetime.fromisoformat(args.as_of.replace("Z", "+00:00")) if args.as_of else datetime.now(UTC)
    if now >= datetime.fromisoformat(EXPIRY.replace("Z", "+00:00")):
        fail("approval is expired")

    evidence = approval.get("approvalEvidence")
    if not isinstance(evidence, dict) or evidence.get("pullRequest") != args.target_pr:
        fail("approval evidence is not bound to the target PR")
    if evidence.get("reviewedPullRequestHeadSha") != args.target_sha:
        fail("approval evidence head is not bound to the target SHA")
    if evidence.get("candidateSha256") != args.candidate_sha256:
        fail("approval evidence does not bind the requested candidate")

    if not args.github_fixture:
        token = os.environ.get("TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN")
        if not token:
            fail("GitHub approval-read credential is missing")
        validator = ROOT / "scripts/security/validate-owasp-exception-candidates.py"
        environment = os.environ | {"GITHUB_TOKEN": token}
        result = subprocess.run([
            "python3", str(validator), "--suppression", str(args.candidate),
            "--approval-record", str(args.approval_record), "--mode", "activation",
            "--scope", SCOPE, "--verify-github-approvals",
        ], env=environment, text=True, capture_output=True, check=False)
        if result.returncode:
            fail("authenticated GitHub approval validation failed")
    else:
        pr = read_pr(args.target_pr, args.github_fixture)
        if pr.get("draft") is not False:
            fail("draft PRs are not eligible for privileged activation")
        base = pr.get("base") if isinstance(pr.get("base"), dict) else {}
        head = pr.get("head") if isinstance(pr.get("head"), dict) else {}
        if base.get("ref") != "master":
            fail("target PR does not target master")
        if head.get("ref") != SOURCE_BRANCH:
            fail("target PR source branch is not approved")
        if head.get("sha") != args.target_sha:
            fail("target PR head no longer matches approved SHA")

    args.output.write_text(json.dumps({
        "repository": REPOSITORY,
        "prNumber": args.target_pr,
        "targetSha": args.target_sha,
        "candidateSha256": args.candidate_sha256,
        "scope": SCOPE,
        "expiresAt": EXPIRY,
    }, sort_keys=True) + "\n")


if __name__ == "__main__":
    try:
        main()
    except (OSError, json.JSONDecodeError) as error:
        fail(type(error).__name__)
