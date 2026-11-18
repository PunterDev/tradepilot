#!/usr/bin/env python3
"""Validate the one owner-approved December 15 suppression handoff.

This validator deliberately has no policy inputs. It is committed with the
protected scanner and accepts only the independently reviewed PR #16 scope.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import sys
import xml.etree.ElementTree as element_tree
from datetime import datetime, timezone
from pathlib import Path


REPOSITORY = "PunterDev/tradepilot"
PULL_REQUEST = 16
REVIEWED_COMMIT = "45931748c1551a3bf77d6070f9f37e9fae0c344d"
PREPARED_BASELINE = "bfa203b7938b3a15ac0babb9ab78b87c2e004e3c"
COMMENT_ID = 6938531951
DECISION_TIMESTAMP = "2026-11-17T04:39:29Z"
EXPIRY = "2026-12-15T23:59:59Z"
CANDIDATE_SHA256 = "ce8ac95116e3ed8718f8ce20367cd0720d4300edfbe63be37bae382b9c9cf3bb"
NAMESPACE = "{https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd}"
HANDOFF_XML = "original-eight-dec15.xml"
HANDOFF_MANIFEST = "original-eight-dec15.manifest.json"
ALLOWED_CHANGED_FILES = {
    "docs/security/candidates/dependency-check-original-eight-dec15-candidate.xml": "added",
    "docs/security/candidates/owasp-original-eight-dec15-approval.json": "added",
    "scripts/security/tests/test_validate_owasp_exception_candidates.py": "modified",
}
LIMITATIONS = {
    "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
    "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
    "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
}
TUPLES = frozenset(
    {
        ("CVE-2026-53914", "pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25"),
        ("CVE-2026-47884", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-47890", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-47891", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-47892", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-59313", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-59283", "pkg:maven/org.springframework/spring-core@6.2.19"),
        ("CVE-2026-59270", "pkg:maven/org.springframework.security/spring-security-core@6.5.11"),
    }
)


class ValidationError(ValueError):
    """Raised when untrusted activation data does not meet the fixed policy."""


def read_json(path: Path) -> object:
    try:
        return json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ValidationError(f"cannot read JSON input: {path.name}") from error


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as source:
            for block in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(block)
    except OSError as error:
        raise ValidationError(f"cannot read input: {path.name}") from error
    return digest.hexdigest()


def parse_timestamp(value: object) -> datetime:
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ValidationError("timestamp must be UTC")
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise ValidationError("timestamp is invalid") from error


def package_pattern(package_url: str) -> str:
    return f"^{re.escape(package_url)}$"


def validate_candidate(path: Path, now: datetime) -> None:
    if sha256(path) != CANDIDATE_SHA256:
        raise ValidationError("candidate hash does not match the approved XML")
    if now >= parse_timestamp(EXPIRY):
        raise ValidationError("candidate is expired")
    try:
        root = element_tree.parse(path).getroot()
    except (OSError, element_tree.ParseError) as error:
        raise ValidationError("candidate XML is invalid") from error
    if root.tag != f"{NAMESPACE}suppressions":
        raise ValidationError("candidate namespace is invalid")
    actual: set[tuple[str, str]] = set()
    for suppression in root:
        if suppression.tag != f"{NAMESPACE}suppress" or suppression.attrib != {"until": "2026-12-15"}:
            raise ValidationError("candidate suppression metadata is invalid")
        children = {child.tag.removeprefix(NAMESPACE) for child in suppression}
        if children - {"notes", "packageUrl", "cve"}:
            raise ValidationError("candidate contains an unsupported suppression matcher")
        packages = suppression.findall(f"{NAMESPACE}packageUrl")
        cves = suppression.findall(f"{NAMESPACE}cve")
        notes = suppression.findall(f"{NAMESPACE}notes")
        if (
            len(packages) != 1
            or len(cves) != 1
            or len(notes) != 1
            or not (notes[0].text or "").strip()
        ):
            raise ValidationError("candidate suppression structure is invalid")
        package = packages[0]
        cve = cves[0]
        if package.attrib != {"regex": "true"}:
            raise ValidationError("candidate suppression is incomplete")
        package_value = (package.text or "").strip()
        cve_value = (cve.text or "").strip()
        match = next(
            (
                (approved_cve, approved_purl)
                for approved_cve, approved_purl in TUPLES
                if cve_value == approved_cve and package_value == package_pattern(approved_purl)
            ),
            None,
        )
        if match is None or match in actual:
            raise ValidationError("candidate CVE/PURL scope is invalid")
        actual.add(match)
    if actual != TUPLES:
        raise ValidationError("candidate CVE/PURL scope is incomplete")


def require_mapping(value: object, name: str) -> dict[str, object]:
    if not isinstance(value, dict):
        raise ValidationError(f"{name} is invalid")
    return value


def validate_approval(value: object) -> None:
    approval = require_mapping(value, "approval record")
    expected = {
        "status": "APPROVED",
        "scope": "original-eight-dec15",
        "reviewedCommit": REVIEWED_COMMIT,
        "preparedBaselineCommit": PREPARED_BASELINE,
        "expiry": EXPIRY,
        "decisionTimestamp": DECISION_TIMESTAMP,
    }
    if any(approval.get(key) != expected_value for key, expected_value in expected.items()):
        raise ValidationError("approval record binding is invalid")
    for role in ("repositoryOwner", "securityRiskOwner"):
        actor = require_mapping(approval.get(role), role)
        if actor != {"name": "PunterDev", "decision": "APPROVE", "approvedAt": DECISION_TIMESTAMP}:
            raise ValidationError(f"{role} approval is invalid")
    configuration = require_mapping(approval.get("productionConfiguration"), "production configuration")
    if configuration.get("status") != "OWNER_ATTESTED_WITH_LIMITATIONS" or not configuration.get("evidenceReference"):
        raise ValidationError("production configuration attestation is invalid")
    if not isinstance(configuration.get("limitations"), list) or set(configuration["limitations"]) != LIMITATIONS:
        raise ValidationError("production configuration limitations are invalid")
    governance = require_mapping(approval.get("governance"), "governance")
    if governance.get("mode") != "SOLO_FOUNDER" or governance.get("soloFounder") != {
        "policyAdoption": "ADOPTED", "githubLogin": "PunterDev", "commentId": str(COMMENT_ID)
    }:
        raise ValidationError("solo-founder governance is invalid")
    evidence = require_mapping(approval.get("approvalEvidence"), "approval evidence")
    if evidence.get("repository") != REPOSITORY or evidence.get("pullRequest") != PULL_REQUEST:
        raise ValidationError("approval evidence does not identify PR #16")
    if evidence.get("reviewedPullRequestHeadSha") != REVIEWED_COMMIT or evidence.get("candidateSha256") != CANDIDATE_SHA256:
        raise ValidationError("approval evidence binding is invalid")
    expected_comment = {"githubLogin": "PunterDev", "commentId": str(COMMENT_ID)}
    if any(evidence.get(role) != expected_comment for role in (
        "repositoryOwner", "securityRiskOwner", "productionConfigurationAttestor"
    )):
        raise ValidationError("approval comment evidence is invalid")


def tuple_lines(body: str) -> set[tuple[str, str]]:
    return {
        (match.group(1), match.group(2))
        for match in re.finditer(
            r"(?m)^\s*-\s*(CVE-\d{4}-\d+)\s*\|\s*"
            r"(pkg:maven/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+@[A-Za-z0-9._-]+)\s*$",
            body,
        )
    }


def validate_comment(value: object) -> None:
    comment = require_mapping(value, "GitHub approval comment")
    user = require_mapping(comment.get("user"), "GitHub approval author")
    expected_issue = f"https://api.github.com/repos/{REPOSITORY}/issues/{PULL_REQUEST}"
    if comment.get("id") != COMMENT_ID or user.get("login") != "PunterDev":
        raise ValidationError("GitHub approval identity is invalid")
    if comment.get("author_association") != "OWNER" or comment.get("issue_url") != expected_issue:
        raise ValidationError("GitHub approval association is invalid")
    if comment.get("created_at") != DECISION_TIMESTAMP or comment.get("updated_at") != DECISION_TIMESTAMP:
        raise ValidationError("GitHub approval timestamp is invalid")
    body = comment.get("body")
    if not isinstance(body, str):
        raise ValidationError("GitHub approval body is invalid")
    required_terms = (
        "SOLO_FOUNDER_GOVERNANCE: ADOPT",
        "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
        "NO_INDEPENDENT_SECURITY_REVIEW",
        "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
        "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
        "OWNER_AUTHORIZATION: APPROVE",
        "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
        "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
        "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
        "original-eight-dec15",
        REVIEWED_COMMIT,
        CANDIDATE_SHA256,
        EXPIRY,
        *LIMITATIONS,
    )
    if any(term not in body for term in required_terms) or tuple_lines(body) != TUPLES:
        raise ValidationError("GitHub approval scope evidence is invalid")


def validate_pull_request(value: object, expected_head: str) -> None:
    pull_request = require_mapping(value, "pull request metadata")
    head = require_mapping(pull_request.get("head"), "pull request head")
    base = require_mapping(pull_request.get("base"), "pull request base")
    head_repo = require_mapping(head.get("repo"), "pull request head repository")
    base_repo = require_mapping(base.get("repo"), "pull request base repository")
    if not re.fullmatch(r"[0-9a-f]{40}", expected_head):
        raise ValidationError("expected pull request head is invalid")
    if (
        pull_request.get("number") != PULL_REQUEST
        or pull_request.get("state") != "open"
        or base.get("ref") != "master"
        or base_repo.get("full_name") != REPOSITORY
        or head_repo.get("full_name") != REPOSITORY
        or head.get("sha") != expected_head
    ):
        raise ValidationError("pull request identity is invalid")


def validate_comparison(value: object, expected_head: str) -> None:
    comparison = require_mapping(value, "reviewed source comparison")
    merge_base = require_mapping(comparison.get("merge_base_commit"), "comparison merge base")
    commits = comparison.get("commits")
    files = comparison.get("files")
    if (
        comparison.get("status") != "ahead"
        or comparison.get("ahead_by") != 1
        or comparison.get("total_commits") != 1
        or merge_base.get("sha") != REVIEWED_COMMIT
        or not isinstance(commits, list)
        or len(commits) != 1
        or not isinstance(commits[0], dict)
        or commits[0].get("sha") != expected_head
        or not isinstance(files, list)
        or len(files) != len(ALLOWED_CHANGED_FILES)
    ):
        raise ValidationError("activation commit ancestry is invalid")
    actual_files = {file.get("filename"): file.get("status") for file in files if isinstance(file, dict)}
    if actual_files != ALLOWED_CHANGED_FILES:
        raise ValidationError("activation commit contains unreviewed changes")


def write_handoff(candidate: Path, destination: Path, expected_head: str) -> None:
    if os.path.lexists(destination):
        raise ValidationError("suppression handoff destination already exists")
    try:
        destination.mkdir(mode=0o700)
    except OSError as error:
        raise ValidationError("cannot create suppression handoff") from error
    target = destination / HANDOFF_XML
    shutil.copyfile(candidate, target)
    os.chmod(target, 0o600)
    manifest = {
        "candidateSha256": CANDIDATE_SHA256,
        "expiresAt": EXPIRY,
        "pullRequest": PULL_REQUEST,
        "pullRequestHead": expected_head,
        "reviewedCommit": REVIEWED_COMMIT,
        "scope": "original-eight-dec15",
    }
    (destination / HANDOFF_MANIFEST).write_text(json.dumps(manifest, sort_keys=True, separators=(",", ":")) + "\n")
    os.chmod(destination / HANDOFF_MANIFEST, 0o600)


def verify_handoff(directory: Path, expected_head: str, now: datetime) -> None:
    if not directory.is_dir() or directory.is_symlink():
        raise ValidationError("suppression handoff directory is invalid")
    try:
        inventory = {entry.name for entry in directory.iterdir()}
    except OSError as error:
        raise ValidationError("cannot inspect suppression handoff") from error
    if inventory != {HANDOFF_XML, HANDOFF_MANIFEST}:
        raise ValidationError("suppression handoff inventory is invalid")
    candidate = directory / HANDOFF_XML
    manifest_path = directory / HANDOFF_MANIFEST
    if not candidate.is_file() or candidate.is_symlink() or not manifest_path.is_file() or manifest_path.is_symlink():
        raise ValidationError("suppression handoff files are invalid")
    validate_candidate(candidate, now)
    manifest = read_json(manifest_path)
    expected = {
        "candidateSha256": CANDIDATE_SHA256,
        "expiresAt": EXPIRY,
        "pullRequest": PULL_REQUEST,
        "pullRequestHead": expected_head,
        "reviewedCommit": REVIEWED_COMMIT,
        "scope": "original-eight-dec15",
    }
    if manifest != expected:
        raise ValidationError("suppression handoff binding is invalid")


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    commands = root.add_subparsers(dest="command", required=True)
    validate = commands.add_parser("validate")
    validate.add_argument("--candidate", type=Path, required=True)
    validate.add_argument("--approval", type=Path, required=True)
    validate.add_argument("--comment", type=Path, required=True)
    validate.add_argument("--pull-request", type=Path, required=True)
    validate.add_argument("--comparison", type=Path, required=True)
    validate.add_argument("--expected-head", required=True)
    validate.add_argument("--handoff-dir", type=Path, required=True)
    validate.add_argument("--now")
    verify = commands.add_parser("verify-handoff")
    verify.add_argument("--handoff-dir", type=Path, required=True)
    verify.add_argument("--expected-head", required=True)
    verify.add_argument("--now")
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        now = parse_timestamp(args.now) if args.now else datetime.now(timezone.utc)
        if args.command == "validate":
            validate_candidate(args.candidate, now)
            validate_approval(read_json(args.approval))
            validate_comment(read_json(args.comment))
            validate_pull_request(read_json(args.pull_request), args.expected_head)
            validate_comparison(read_json(args.comparison), args.expected_head)
            write_handoff(args.candidate, args.handoff_dir, args.expected_head)
        else:
            verify_handoff(args.handoff_dir, args.expected_head, now)
    except ValidationError as error:
        print(f"original-eight-dec15 activation validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
