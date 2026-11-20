#!/usr/bin/env python3
"""Validate two independently approved December 15 suppression packages."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import re
import sys
import xml.etree.ElementTree as element_tree
from datetime import datetime, timezone
from pathlib import Path


REPOSITORY = "PunterDev/tradepilot"
PULL_REQUEST = 16
ORIGINAL_REVIEWED_COMMIT = "45931748c1551a3bf77d6070f9f37e9fae0c344d"
ORIGINAL_ACTIVATION_COMMIT = "33706d9db11b120df8c20071617b7857704e35ee"
SECOND_COMMENT_ID = 7031996454
EXPIRY = "2026-12-15T23:59:59Z"
SECOND_CANDIDATE_SHA256 = "2e9637cc067369f4982599c0fa4018c819a40c9570828d5fc4e4813f9e4bcc26"
NAMESPACE_URI = "https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd"
NAMESPACE = f"{{{NAMESPACE_URI}}}"
HANDOFF_XML = "second-eight-transitive-dec15.xml"
HANDOFF_MANIFEST = "second-eight-transitive-dec15.manifest.json"
SECOND_ALLOWED_CHANGED_FILES = {
    "docs/security/candidates/dependency-check-second-eight-transitive-dec15-candidate.xml": "added",
    "docs/security/candidates/owasp-second-eight-transitive-dec15-approval.json": "added",
}
LIMITATIONS = {
    "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
    "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
    "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
}
SECOND_TUPLES = frozenset(
    {
        ("CVE-2026-53914", "pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25"),
        ("CVE-2026-47884", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-47890", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-47891", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-47892", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-59313", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-59283", "pkg:maven/org.springframework/spring-tx@6.2.19"),
        ("CVE-2026-59270", "pkg:maven/org.springframework.security/spring-security-web@6.5.11"),
    }
)


class ValidationError(ValueError):
    """Raised when untrusted activation data does not meet the fixed policy."""


def load_original_validator() -> object:
    path = Path(__file__).with_name("validate_original_eight_dec15_activation.py")
    specification = importlib.util.spec_from_file_location("original_eight_dec15", path)
    if specification is None or specification.loader is None:
        raise ValidationError("protected original validator is unavailable")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


ORIGINAL = load_original_validator()


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


def require_mapping(value: object, name: str) -> dict[str, object]:
    if not isinstance(value, dict):
        raise ValidationError(f"{name} is invalid")
    return value


def require_original(callable_object: object, *arguments: object) -> object:
    try:
        return callable_object(*arguments)
    except ORIGINAL.ValidationError as error:
        raise ValidationError(f"original activation evidence is invalid: {error}") from error


def validate_second_candidate(path: Path, now: datetime) -> None:
    if sha256(path) != SECOND_CANDIDATE_SHA256:
        raise ValidationError("second candidate hash does not match the approved XML")
    if now >= parse_timestamp(EXPIRY):
        raise ValidationError("second candidate is expired")
    try:
        root = element_tree.parse(path).getroot()
    except (OSError, element_tree.ParseError) as error:
        raise ValidationError("second candidate XML is invalid") from error
    if root.tag != f"{NAMESPACE}suppressions":
        raise ValidationError("second candidate namespace is invalid")
    actual: set[tuple[str, str]] = set()
    for suppression in root:
        if suppression.tag != f"{NAMESPACE}suppress" or suppression.attrib != {"until": "2026-12-15"}:
            raise ValidationError("second candidate suppression metadata is invalid")
        children = {child.tag.removeprefix(NAMESPACE) for child in suppression}
        if children - {"notes", "packageUrl", "cve"}:
            raise ValidationError("second candidate contains an unsupported suppression matcher")
        packages = suppression.findall(f"{NAMESPACE}packageUrl")
        cves = suppression.findall(f"{NAMESPACE}cve")
        notes = suppression.findall(f"{NAMESPACE}notes")
        if len(packages) != 1 or len(cves) != 1 or len(notes) != 1 or not (notes[0].text or "").strip():
            raise ValidationError("second candidate suppression structure is invalid")
        if packages[0].attrib != {"regex": "true"}:
            raise ValidationError("second candidate suppression is incomplete")
        candidate = ((cves[0].text or "").strip(), (packages[0].text or "").strip())
        match = next(
            ((cve, purl) for cve, purl in SECOND_TUPLES if candidate == (cve, package_pattern(purl))),
            None,
        )
        if match is None or match in actual:
            raise ValidationError("second candidate CVE/PURL scope is invalid")
        actual.add(match)
    if actual != SECOND_TUPLES:
        raise ValidationError("second candidate CVE/PURL scope is incomplete")


def validate_second_approval(value: object, now: datetime) -> datetime:
    approval = require_mapping(value, "second approval record")
    expected = {
        "status": "APPROVED",
        "scope": "second-eight-transitive-dec15",
        "reviewedCommit": ORIGINAL_ACTIVATION_COMMIT,
        "preparedBaselineCommit": ORIGINAL_ACTIVATION_COMMIT,
        "expiry": EXPIRY,
    }
    if any(approval.get(key) != expected_value for key, expected_value in expected.items()):
        raise ValidationError("second approval record binding is invalid")
    decision_timestamp = parse_timestamp(approval.get("decisionTimestamp"))
    if decision_timestamp > now or decision_timestamp >= parse_timestamp(EXPIRY):
        raise ValidationError("second approval timestamp is expired")
    for role in ("repositoryOwner", "securityRiskOwner"):
        actor = require_mapping(approval.get(role), role)
        if actor != {"name": "PunterDev", "decision": "APPROVE", "approvedAt": approval.get("decisionTimestamp")}:
            raise ValidationError(f"{role} approval is invalid")
    configuration = require_mapping(approval.get("productionConfiguration"), "production configuration")
    if configuration.get("status") != "OWNER_ATTESTED_WITH_LIMITATIONS" or not configuration.get("evidenceReference"):
        raise ValidationError("production configuration attestation is invalid")
    if not isinstance(configuration.get("limitations"), list) or set(configuration["limitations"]) != LIMITATIONS:
        raise ValidationError("production configuration limitations are invalid")
    expected_comment = {"githubLogin": "PunterDev", "commentId": str(SECOND_COMMENT_ID)}
    governance = require_mapping(approval.get("governance"), "governance")
    if governance.get("mode") != "SOLO_FOUNDER" or governance.get("soloFounder") != {
        "policyAdoption": "ADOPTED", **expected_comment
    }:
        raise ValidationError("solo-founder governance is invalid")
    evidence = require_mapping(approval.get("approvalEvidence"), "approval evidence")
    if (
        evidence.get("repository") != REPOSITORY
        or evidence.get("pullRequest") != PULL_REQUEST
        or evidence.get("reviewedPullRequestHeadSha") != ORIGINAL_ACTIVATION_COMMIT
        or evidence.get("candidateSha256") != SECOND_CANDIDATE_SHA256
    ):
        raise ValidationError("second approval evidence binding is invalid")
    if any(evidence.get(role) != expected_comment for role in (
        "repositoryOwner", "securityRiskOwner", "productionConfigurationAttestor"
    )):
        raise ValidationError("second approval comment evidence is invalid")
    return decision_timestamp


def tuple_lines(body: str) -> set[tuple[str, str]]:
    return {
        (match.group(1), match.group(2))
        for match in re.finditer(
            r"(?m)^\s*-\s*(CVE-\d{4}-\d+)\s*\|\s*"
            r"(pkg:maven/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+@[A-Za-z0-9._-]+)\s*$",
            body,
        )
    }


def validate_second_comment(value: object, decision_timestamp: datetime, now: datetime) -> None:
    comment = require_mapping(value, "second GitHub approval comment")
    user = require_mapping(comment.get("user"), "second GitHub approval author")
    expected_issue = f"https://api.github.com/repos/{REPOSITORY}/issues/{PULL_REQUEST}"
    if comment.get("id") != SECOND_COMMENT_ID or user.get("login") != "PunterDev":
        raise ValidationError("second GitHub approval identity is invalid")
    if comment.get("author_association") != "OWNER" or comment.get("issue_url") != expected_issue:
        raise ValidationError("second GitHub approval association is invalid")
    created = parse_timestamp(comment.get("created_at"))
    if (
        comment.get("updated_at") != comment.get("created_at")
        or created != decision_timestamp
        or created > now
        or created >= parse_timestamp(EXPIRY)
    ):
        raise ValidationError("second GitHub approval timestamp is invalid")
    body = comment.get("body")
    if not isinstance(body, str):
        raise ValidationError("second GitHub approval body is invalid")
    required_terms = (
        "SOLO_FOUNDER_GOVERNANCE: ADOPT",
        "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
        "NO_INDEPENDENT_SECURITY_REVIEW",
        "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE",
        "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
        "OWNER_AUTHORIZATION: APPROVE",
        "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
        "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
        "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
        "second-eight-transitive-dec15",
        ORIGINAL_ACTIVATION_COMMIT,
        SECOND_CANDIDATE_SHA256,
        EXPIRY,
        *LIMITATIONS,
    )
    if any(term not in body for term in required_terms) or tuple_lines(body) != SECOND_TUPLES:
        raise ValidationError("second GitHub approval scope evidence is invalid")


def validate_current_pull_request(value: object, expected_head: str) -> None:
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
        raise ValidationError("second activation pull request identity is invalid")


def validate_second_comparison(value: object, expected_head: str) -> None:
    comparison = require_mapping(value, "second activation comparison")
    merge_base = require_mapping(comparison.get("merge_base_commit"), "second activation merge base")
    commits = comparison.get("commits")
    files = comparison.get("files")
    if (
        comparison.get("status") != "ahead"
        or comparison.get("ahead_by") != 1
        or comparison.get("total_commits") != 1
        or merge_base.get("sha") != ORIGINAL_ACTIVATION_COMMIT
        or not isinstance(commits, list)
        or len(commits) != 1
        or not isinstance(commits[0], dict)
        or commits[0].get("sha") != expected_head
        or not isinstance(files, list)
        or len(files) != len(SECOND_ALLOWED_CHANGED_FILES)
    ):
        raise ValidationError("second activation commit ancestry is invalid")
    actual_files = {item.get("filename"): item.get("status") for item in files if isinstance(item, dict)}
    if actual_files != SECOND_ALLOWED_CHANGED_FILES:
        raise ValidationError("second activation commit contains unreviewed changes")


def combined_xml(original: Path, second: Path) -> bytes:
    element_tree.register_namespace("", NAMESPACE_URI)
    root = element_tree.Element(f"{NAMESPACE}suppressions")
    for source in (original, second):
        source_root = element_tree.parse(source).getroot()
        for suppression in source_root:
            root.append(element_tree.fromstring(element_tree.tostring(suppression, encoding="utf-8")))
    return element_tree.tostring(root, encoding="utf-8", xml_declaration=True) + b"\n"


def validate_combined(path: Path, now: datetime) -> None:
    try:
        root = element_tree.parse(path).getroot()
    except (OSError, element_tree.ParseError) as error:
        raise ValidationError("combined suppression XML is invalid") from error
    if root.tag != f"{NAMESPACE}suppressions":
        raise ValidationError("combined suppression namespace is invalid")
    expected = set(ORIGINAL.TUPLES) | SECOND_TUPLES
    actual: set[tuple[str, str]] = set()
    for suppression in root:
        if suppression.tag != f"{NAMESPACE}suppress" or suppression.attrib != {"until": "2026-12-15"}:
            raise ValidationError("combined suppression metadata is invalid")
        children = {child.tag.removeprefix(NAMESPACE) for child in suppression}
        if children - {"notes", "packageUrl", "cve"}:
            raise ValidationError("combined suppression contains an unsupported matcher")
        packages = suppression.findall(f"{NAMESPACE}packageUrl")
        cves = suppression.findall(f"{NAMESPACE}cve")
        notes = suppression.findall(f"{NAMESPACE}notes")
        if (
            len(packages) != 1
            or len(cves) != 1
            or len(notes) != 1
            or not (notes[0].text or "").strip()
            or packages[0].attrib != {"regex": "true"}
        ):
            raise ValidationError("combined suppression structure is invalid")
        candidate = ((cves[0].text or "").strip(), (packages[0].text or "").strip())
        match = next(
            ((cve, purl) for cve, purl in expected if candidate == (cve, package_pattern(purl))),
            None,
        )
        if match is None or match in actual:
            raise ValidationError("combined suppression scope is invalid")
        actual.add(match)
    if actual != expected or now >= parse_timestamp(EXPIRY):
        raise ValidationError("combined suppression scope is incomplete or expired")


def write_handoff(original: Path, second: Path, destination: Path, expected_head: str) -> None:
    if os.path.lexists(destination):
        raise ValidationError("second suppression handoff destination already exists")
    try:
        destination.mkdir(mode=0o700)
    except OSError as error:
        raise ValidationError("cannot create second suppression handoff") from error
    target = destination / HANDOFF_XML
    target.write_bytes(combined_xml(original, second))
    os.chmod(target, 0o600)
    manifest = {
        "combinedSha256": sha256(target),
        "expiresAt": EXPIRY,
        "originalActivationCommit": ORIGINAL_ACTIVATION_COMMIT,
        "originalCandidateSha256": ORIGINAL.CANDIDATE_SHA256,
        "pullRequest": PULL_REQUEST,
        "pullRequestHead": expected_head,
        "scopes": ["original-eight-dec15", "second-eight-transitive-dec15"],
        "secondCandidateSha256": SECOND_CANDIDATE_SHA256,
        "secondReviewedCommit": ORIGINAL_ACTIVATION_COMMIT,
    }
    manifest_path = destination / HANDOFF_MANIFEST
    manifest_path.write_text(json.dumps(manifest, sort_keys=True, separators=(",", ":")) + "\n")
    os.chmod(manifest_path, 0o600)


def verify_handoff(directory: Path, expected_head: str, now: datetime) -> None:
    if not directory.is_dir() or directory.is_symlink():
        raise ValidationError("second suppression handoff directory is invalid")
    try:
        inventory = {entry.name for entry in directory.iterdir()}
    except OSError as error:
        raise ValidationError("cannot inspect second suppression handoff") from error
    if inventory != {HANDOFF_XML, HANDOFF_MANIFEST}:
        raise ValidationError("second suppression handoff inventory is invalid")
    candidate = directory / HANDOFF_XML
    manifest_path = directory / HANDOFF_MANIFEST
    if not candidate.is_file() or candidate.is_symlink() or not manifest_path.is_file() or manifest_path.is_symlink():
        raise ValidationError("second suppression handoff files are invalid")
    validate_combined(candidate, now)
    manifest = read_json(manifest_path)
    expected = {
        "combinedSha256": sha256(candidate),
        "expiresAt": EXPIRY,
        "originalActivationCommit": ORIGINAL_ACTIVATION_COMMIT,
        "originalCandidateSha256": ORIGINAL.CANDIDATE_SHA256,
        "pullRequest": PULL_REQUEST,
        "pullRequestHead": expected_head,
        "scopes": ["original-eight-dec15", "second-eight-transitive-dec15"],
        "secondCandidateSha256": SECOND_CANDIDATE_SHA256,
        "secondReviewedCommit": ORIGINAL_ACTIVATION_COMMIT,
    }
    if manifest != expected:
        raise ValidationError("second suppression handoff binding is invalid")


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    commands = root.add_subparsers(dest="command", required=True)
    validate = commands.add_parser("validate")
    for name in ("original-candidate", "original-approval", "original-comment", "stage-a-comparison", "second-candidate", "second-approval", "second-comment", "pull-request", "stage-b-comparison"):
        validate.add_argument(f"--{name}", type=Path, required=True)
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
            require_original(ORIGINAL.validate_candidate, args.original_candidate, now)
            require_original(ORIGINAL.validate_approval, ORIGINAL.read_json(args.original_approval))
            require_original(ORIGINAL.validate_comment, ORIGINAL.read_json(args.original_comment))
            require_original(ORIGINAL.validate_comparison, ORIGINAL.read_json(args.stage_a_comparison), ORIGINAL_ACTIVATION_COMMIT)
            validate_second_candidate(args.second_candidate, now)
            decision_timestamp = validate_second_approval(read_json(args.second_approval), now)
            validate_second_comment(read_json(args.second_comment), decision_timestamp, now)
            validate_current_pull_request(read_json(args.pull_request), args.expected_head)
            validate_second_comparison(read_json(args.stage_b_comparison), args.expected_head)
            write_handoff(args.original_candidate, args.second_candidate, args.handoff_dir, args.expected_head)
        else:
            verify_handoff(args.handoff_dir, args.expected_head, now)
    except ValidationError as error:
        print(f"second-eight-transitive-dec15 activation validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
