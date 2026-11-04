#!/usr/bin/env python3
"""Fail-closed validation for exact Dependency-Check exception candidates."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as element_tree
from datetime import datetime, timezone
from pathlib import Path


SUPPRESSION_EXPIRY = "2026-11-30"
APPROVAL_EXPIRY = "2026-11-30T23:59:59Z"
ORIGINAL_EIGHT = frozenset(
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
EXPANDED_SIXTEEN = ORIGINAL_EIGHT | frozenset(
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
SCOPES = {
    "original-eight": ORIGINAL_EIGHT,
    "expanded-sixteen": EXPANDED_SIXTEEN,
}
SUPPRESSION_NAMESPACE = "{https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd}"
APPROVAL_REPOSITORY = "PunterDev/tradepilot"
APPROVAL_PR = 6
REVIEWED_COMMITS = {
    "original-eight": "5d6b05af6a2c7234ec3d666d8cabc3d45f020a35",
    "expanded-sixteen": "db2ace78d080b86bd1520c7b027398557235c933",
}
SOLO_FOUNDER_MODE = "SOLO_FOUNDER"
OWNER_ATTESTED_CONFIG_STATUS = "OWNER_ATTESTED_WITH_LIMITATIONS"
REQUIRED_CONFIGURATION_LIMITATIONS = frozenset(
    {
        "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
        "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
        "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
    }
)


class PolicyError(ValueError):
    """A candidate breaks the narrowly approved exception policy."""


def parse_timestamp(value: str) -> datetime:
    if not value.endswith("Z"):
        raise PolicyError(f"timestamp must be UTC with Z suffix: {value}")
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise PolicyError(f"invalid timestamp: {value}") from error


def parse_suppression_expiry(value: str) -> datetime:
    if value != SUPPRESSION_EXPIRY:
        raise PolicyError(f"each suppression must expire exactly on {SUPPRESSION_EXPIRY}")
    try:
        return datetime.fromisoformat(value).replace(tzinfo=timezone.utc)
    except ValueError as error:
        raise PolicyError(f"invalid suppression expiry: {value}") from error


def exact_package_pattern(package_url: str) -> str:
    return f"^{re.escape(package_url)}$"


def child_text(element: element_tree.Element, name: str) -> str | None:
    child = element.find(f"{SUPPRESSION_NAMESPACE}{name}")
    return child.text.strip() if child is not None and child.text else None


def validate_suppressions(
    path: Path, as_of: datetime, approved: frozenset[tuple[str, str]]
) -> set[tuple[str, str]]:
    try:
        root = element_tree.parse(path).getroot()
    except (OSError, element_tree.ParseError) as error:
        raise PolicyError(f"cannot parse suppression XML: {error}") from error

    if root.tag != f"{SUPPRESSION_NAMESPACE}suppressions":
        raise PolicyError("suppression XML must use the Dependency-Check 1.3 namespace")

    actual: set[tuple[str, str]] = set()
    for suppression in root:
        if suppression.tag != f"{SUPPRESSION_NAMESPACE}suppress":
            raise PolicyError("only suppress elements are allowed")
        forbidden = {child.tag.removeprefix(SUPPRESSION_NAMESPACE) for child in suppression}
        if forbidden - {"notes", "packageUrl", "cve"}:
            raise PolicyError(f"broad or unsupported suppression matcher present: {sorted(forbidden)}")

        expiry = suppression.get("until")
        if as_of >= parse_suppression_expiry(expiry or ""):
            raise PolicyError(f"suppression is expired: {expiry}")

        package = suppression.find(f"{SUPPRESSION_NAMESPACE}packageUrl")
        if package is None or package.get("regex") != "true":
            raise PolicyError("each suppression needs an exact regex packageUrl")
        package_pattern = (package.text or "").strip()
        cve = child_text(suppression, "cve")
        if not cve:
            raise PolicyError("each suppression needs one explicit CVE")

        tuple_value = next(
            (
                (approved_cve, package_url)
                for approved_cve, package_url in approved
                if approved_cve == cve and package_pattern == exact_package_pattern(package_url)
            ),
            None,
        )
        if tuple_value is None:
            raise PolicyError(f"unapproved CVE/PURL tuple: {cve}")
        if tuple_value in actual:
            raise PolicyError(f"duplicate suppression: {tuple_value[0]} {tuple_value[1]}")
        actual.add(tuple_value)

    if actual != approved:
        missing = approved - actual
        extra = actual - approved
        raise PolicyError(f"suppression scope mismatch; missing={sorted(missing)} extra={sorted(extra)}")
    return actual


def validate_findings(path: Path | None, approved: frozenset[tuple[str, str]]) -> None:
    if path is None:
        return
    try:
        findings = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise PolicyError(f"cannot parse findings inventory: {error}") from error
    if not isinstance(findings, list):
        raise PolicyError("findings inventory must be a JSON list")
    for finding in findings:
        try:
            tuple_value = (finding["cve"], finding["purl"])
            cvss = float(finding["cvss"])
        except (KeyError, TypeError, ValueError) as error:
            raise PolicyError("findings inventory contains an invalid finding") from error
        if cvss >= 8 and tuple_value not in approved:
            raise PolicyError(f"unrelated CVSS >= 8 finding remains blocking: {tuple_value}")


def load_approval(path: Path) -> dict:
    try:
        return json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise PolicyError(f"cannot parse approval record: {error}") from error


def validate_approval(approval: dict, scope: str) -> None:
    if approval.get("status") != "APPROVED":
        raise PolicyError("approval record is not APPROVED")
    if approval.get("reviewedCommit") != REVIEWED_COMMITS[scope]:
        raise PolicyError("approval record does not identify the reviewed commit")
    if approval.get("scope") not in (None, scope):
        raise PolicyError("approval record scope does not match the candidate scope")
    decision_timestamp = approval.get("decisionTimestamp")
    if not decision_timestamp:
        raise PolicyError("approval record lacks a decision timestamp")
    parsed_decision_timestamp = parse_timestamp(decision_timestamp)
    if parsed_decision_timestamp > parse_timestamp(APPROVAL_EXPIRY):
        raise PolicyError("approval decision timestamp is after the exception expiry")
    for role in ("repositoryOwner", "securityRiskOwner"):
        actor = approval.get(role, {})
        if not actor.get("name") or actor.get("decision") != "APPROVE" or not actor.get("approvedAt"):
            raise PolicyError(f"{role} approval is incomplete")
        if parse_timestamp(actor["approvedAt"]) != parsed_decision_timestamp:
            raise PolicyError(f"{role} approval timestamp does not match the decision timestamp")
    production_configuration = approval.get("productionConfiguration", {})
    if production_configuration.get("status") != OWNER_ATTESTED_CONFIG_STATUS:
        raise PolicyError("production configuration must remain owner-attested with limitations")
    if not production_configuration.get("evidenceReference"):
        raise PolicyError("production configuration attestation lacks an evidence reference")
    limitations = production_configuration.get("limitations")
    if not isinstance(limitations, list) or not REQUIRED_CONFIGURATION_LIMITATIONS.issubset(limitations):
        raise PolicyError("production configuration attestation does not record all limitations")
    if approval.get("expiry") != APPROVAL_EXPIRY:
        raise PolicyError("approval expiry does not match the exception expiry")

    evidence = approval.get("approvalEvidence", {})
    if evidence.get("repository") != APPROVAL_REPOSITORY or evidence.get("pullRequest") != APPROVAL_PR:
        raise PolicyError("approval evidence must identify the reviewed repository and PR")
    for role in ("repositoryOwner", "securityRiskOwner", "productionConfigurationAttestor"):
        comment = evidence.get(role, {})
        if not comment.get("githubLogin") or not comment.get("commentId"):
            raise PolicyError(f"{role} GitHub approval evidence is incomplete")
    role_logins = {
        evidence["repositoryOwner"]["githubLogin"],
        evidence["securityRiskOwner"]["githubLogin"],
        evidence["productionConfigurationAttestor"]["githubLogin"],
    }
    if len(role_logins) < 3:
        governance = approval.get("governance", {})
        solo_founder = governance.get("soloFounder", {})
        if (
            governance.get("mode") != SOLO_FOUNDER_MODE
            or solo_founder.get("policyAdoption") != "ADOPTED"
            or len(role_logins) != 1
            or solo_founder.get("githubLogin") not in role_logins
            or not solo_founder.get("commentId")
        ):
            raise PolicyError("shared evidence identities require one adopted solo-founder governance record")


def candidate_sha256(path: Path) -> str:
    try:
        return hashlib.sha256(path.read_bytes()).hexdigest()
    except OSError as error:
        raise PolicyError(f"cannot read suppression XML for approval verification: {error}") from error


def fetch_github_comment(repository: str, comment_id: str) -> dict:
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        raise PolicyError("GITHUB_TOKEN is required to verify GitHub approval comments")
    request = urllib.request.Request(
        f"https://api.github.com/repos/{repository}/issues/comments/{comment_id}",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return json.loads(response.read())
    except (urllib.error.URLError, urllib.error.HTTPError, json.JSONDecodeError) as error:
        raise PolicyError(f"cannot verify GitHub approval comment {comment_id}: {error}") from error


def verify_comment(
    comment: dict,
    expected_login: str,
    required_terms: tuple[str, ...],
    role: str,
    decision_timestamp: datetime,
    require_owner_association: bool = False,
) -> None:
    actual_login = comment.get("user", {}).get("login")
    body = comment.get("body", "")
    expected_issue_url = f"https://api.github.com/repos/{APPROVAL_REPOSITORY}/issues/{APPROVAL_PR}"
    if actual_login != expected_login:
        raise PolicyError(f"{role} comment author does not match the approval record")
    if comment.get("issue_url") != expected_issue_url:
        raise PolicyError(f"{role} evidence is not a comment on the reviewed PR")
    if require_owner_association and comment.get("author_association") != "OWNER":
        raise PolicyError(f"{role} comment author is not a repository owner")
    try:
        created_at = parse_timestamp(comment["created_at"])
        updated_at = parse_timestamp(comment["updated_at"])
    except KeyError as error:
        raise PolicyError(f"{role} approval comment lacks a GitHub timestamp") from error
    if updated_at < created_at:
        raise PolicyError(f"{role} approval comment has an invalid edit timestamp")
    if updated_at < decision_timestamp:
        raise PolicyError(f"{role} approval comment was not updated at or after the recorded decision")
    if updated_at > parse_timestamp(APPROVAL_EXPIRY):
        raise PolicyError(f"{role} approval comment was updated after the exception expiry")
    missing_terms = [term for term in required_terms if term not in body]
    if missing_terms:
        raise PolicyError(f"{role} approval comment is missing required scope evidence: {missing_terms}")


def parse_approved_tuple_lines(body: str) -> set[tuple[str, str]]:
    return {
        (match.group(1), match.group(2))
        for match in re.finditer(
            r"(?m)^\s*-\s*(CVE-\d{4}-\d+)\s*\|\s*"
            r"(pkg:maven/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+@[A-Za-z0-9._-]+)\s*$",
            body,
        )
    }


def verify_exact_tuple_lines(body: str, approved: frozenset[tuple[str, str]], role: str) -> None:
    actual = parse_approved_tuple_lines(body)
    if actual != approved:
        missing = approved - actual
        extra = actual - approved
        raise PolicyError(
            f"{role} approval comment tuple scope mismatch; "
            f"missing={sorted(missing)} extra={sorted(extra)}"
        )


def verify_github_approvals(
    approval: dict,
    suppression: Path,
    approved: frozenset[tuple[str, str]],
    fetch_comment=fetch_github_comment,
) -> None:
    evidence = approval["approvalEvidence"]
    scope_hash = candidate_sha256(suppression)
    if evidence.get("candidateSha256") != scope_hash:
        raise PolicyError("approval evidence does not bind to the current candidate XML")

    scope = approval.get("scope", "original-eight")
    common_terms = (
        REVIEWED_COMMITS[scope],
        scope_hash,
        APPROVAL_EXPIRY,
    )
    decision_timestamp = parse_timestamp(approval["decisionTimestamp"])
    owner = evidence["repositoryOwner"]
    risk_owner = evidence["securityRiskOwner"]
    config_attestor = evidence["productionConfigurationAttestor"]
    verify_comment(
        fetch_comment(APPROVAL_REPOSITORY, str(owner["commentId"])),
        owner["githubLogin"],
        ("OWNER_AUTHORIZATION: APPROVE",) + common_terms,
        "repository-owner",
        decision_timestamp,
    )
    verify_comment(
        fetch_comment(APPROVAL_REPOSITORY, str(risk_owner["commentId"])),
        risk_owner["githubLogin"],
        ("SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",) + common_terms,
        "security-risk owner",
        decision_timestamp,
    )
    verify_comment(
        fetch_comment(APPROVAL_REPOSITORY, str(config_attestor["commentId"])),
        config_attestor["githubLogin"],
        (
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "production",
            "SPRING_LDAP_EMBEDDED_*",
            "SPRING_EXPRESSION_COMPILER_MODE",
            "Environment Groups",
            "Secret Files",
            "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
            "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
            "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
        ),
        "production-configuration attestor",
        decision_timestamp,
    )

    governance = approval.get("governance", {})
    if governance.get("mode") == SOLO_FOUNDER_MODE:
        solo_founder = governance["soloFounder"]
        comment = fetch_comment(APPROVAL_REPOSITORY, str(solo_founder["commentId"]))
        verify_comment(
            comment,
            solo_founder["githubLogin"],
            (
                "SOLO_FOUNDER_GOVERNANCE: ADOPT",
                "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
                "NO_INDEPENDENT_SECURITY_REVIEW",
                "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
                "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
                "OWNER_AUTHORIZATION: APPROVE",
                "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
                "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
                "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
                "production",
                "SPRING_LDAP_EMBEDDED_*",
                "SPRING_EXPRESSION_COMPILER_MODE",
                "Environment Groups",
                "Secret Files",
                "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
                "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
                "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
            ) + common_terms,
            "solo-founder governance",
            decision_timestamp,
            require_owner_association=True,
        )
        verify_exact_tuple_lines(comment.get("body", ""), approved, "solo-founder governance")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--suppression", type=Path, required=True)
    parser.add_argument("--approval-record", type=Path, required=True)
    parser.add_argument("--findings", type=Path)
    parser.add_argument("--as-of", default=datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"))
    parser.add_argument("--mode", choices=("proposal", "activation"), required=True)
    parser.add_argument("--scope", choices=tuple(SCOPES), default="original-eight")
    parser.add_argument("--verify-github-approvals", action="store_true")
    args = parser.parse_args()

    try:
        as_of = parse_timestamp(args.as_of)
        approved = SCOPES[args.scope]
        validate_suppressions(args.suppression, as_of, approved)
        validate_findings(args.findings, approved)
        approval = load_approval(args.approval_record)
        if args.mode == "activation":
            validate_approval(approval, args.scope)
            if args.verify_github_approvals:
                verify_github_approvals(approval, args.suppression, approved)
        else:
            if approval.get("status") != "PENDING":
                raise PolicyError("proposal mode requires a PENDING approval record")
    except (OSError, json.JSONDecodeError, PolicyError) as error:
        print(f"OWASP exception policy: FAIL: {error}", file=sys.stderr)
        return 1

    print(f"OWASP exception policy: PASS ({args.mode} mode)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
