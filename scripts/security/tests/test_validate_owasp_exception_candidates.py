from __future__ import annotations

import json
import hashlib
import runpy
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "scripts/security/validate-owasp-exception-candidates.py"
CANDIDATE = ROOT / "docs/security/candidates/dependency-check-final-eight-candidate.xml"
EXPANDED_CANDIDATE = ROOT / "docs/security/candidates/dependency-check-final-sixteen-candidate.xml"
PENDING_APPROVAL = ROOT / "docs/security/candidates/owasp-final-eight-approval.template.json"
APPROVED_FINDINGS = [
    {"cve": "CVE-2026-53914", "purl": "pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25", "cvss": 9.8},
    {"cve": "CVE-2026-47884", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47890", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47891", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47892", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-59313", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-59283", "purl": "pkg:maven/org.springframework/spring-core@6.2.19", "cvss": 9.1},
    {"cve": "CVE-2026-59270", "purl": "pkg:maven/org.springframework.security/spring-security-core@6.5.11", "cvss": 9.1},
]
EXPANDED_FINDINGS = APPROVED_FINDINGS + [
    {"cve": "CVE-2026-53914", "purl": "pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25", "cvss": 9.8},
    {"cve": "CVE-2026-47884", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47890", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47891", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-47892", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-59313", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.8},
    {"cve": "CVE-2026-59283", "purl": "pkg:maven/org.springframework/spring-tx@6.2.19", "cvss": 9.1},
    {"cve": "CVE-2026-59270", "purl": "pkg:maven/org.springframework.security/spring-security-web@6.5.11", "cvss": 9.1},
]
VALIDATOR_MODULE = runpy.run_path(str(VALIDATOR))
ISSUE_URL = "https://api.github.com/repos/PunterDev/tradepilot/issues/6"
ATTESTATION_LIMITATIONS = [
    "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
    "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
    "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
]


class OwaspExceptionPolicyTests(unittest.TestCase):
    def run_validator(self, suppression: Path = CANDIDATE, approval: Path = PENDING_APPROVAL, findings: list[dict] | None = None, *, mode: str = "proposal", as_of: str = "2026-11-03T00:00:00Z", scope: str = "original-eight") -> subprocess.CompletedProcess[str]:
        command = ["python3", str(VALIDATOR), "--suppression", str(suppression), "--approval-record", str(approval), "--mode", mode, "--as-of", as_of, "--scope", scope]
        if findings is not None:
            with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as findings_file:
                findings_file.write(json.dumps(findings))
                findings_path = Path(findings_file.name)
            self.addCleanup(findings_path.unlink, missing_ok=True)
            command.extend(("--findings", str(findings_path)))
        return subprocess.run(command, text=True, capture_output=True, check=False)

    def approved_record(self, directory: Path, *, candidate: Path = CANDIDATE, scope: str = "original-eight") -> Path:
        path = directory / "approval.json"
        path.write_text(json.dumps({
            "status": "APPROVED",
            "scope": scope,
            "reviewedCommit": "5d6b05af6a2c7234ec3d666d8cabc3d45f020a35",
            "expiry": "2026-11-30T23:59:59Z",
            "decisionTimestamp": "2026-11-03T00:00:00Z",
            "repositoryOwner": {"name": "Owner", "decision": "APPROVE", "approvedAt": "2026-11-03T00:00:00Z"},
            "securityRiskOwner": {"name": "Security owner", "decision": "APPROVE", "approvedAt": "2026-11-03T00:00:00Z"},
            "productionConfiguration": {
                "status": "OWNER_ATTESTED_WITH_LIMITATIONS",
                "evidenceReference": "Render owner attestation, 2026-10-31",
                "limitations": ATTESTATION_LIMITATIONS,
            },
            "approvalEvidence": {
                "repository": "PunterDev/tradepilot",
                "pullRequest": 6,
                "candidateSha256": hashlib.sha256(candidate.read_bytes()).hexdigest(),
                "repositoryOwner": {"githubLogin": "owner", "commentId": "101"},
                "securityRiskOwner": {"githubLogin": "independent", "commentId": "102"},
                "productionConfigurationAttestor": {"githubLogin": "operator", "commentId": "103"},
            },
        }))
        return path

    def approval_without_production_attestation(self, directory: Path) -> Path:
        path = self.approved_record(directory)
        approval = json.loads(path.read_text())
        approval["productionConfiguration"]["status"] = "PENDING"
        path.write_text(json.dumps(approval))
        return path

    @staticmethod
    def comment(login: str, body: str, *, author_association: str = "OWNER") -> dict:
        return {
            "user": {"login": login},
            "author_association": author_association,
            "issue_url": ISSUE_URL,
            "created_at": "2026-11-02T23:59:00Z",
            "updated_at": "2026-11-03T00:00:00Z",
            "body": body,
        }

    @staticmethod
    def exact_scope_terms() -> str:
        return "\n" + "\n".join(
            f"- {finding['cve']} | {finding['purl']}" for finding in APPROVED_FINDINGS
        )

    @staticmethod
    def exact_expanded_scope_lines() -> str:
        return "\n".join(
            f"- {finding['cve']} | {finding['purl']}" for finding in EXPANDED_FINDINGS
        )

    def solo_founder_record(self, directory: Path) -> dict:
        approval = json.loads(self.approved_record(directory).read_text())
        approval["governance"] = {
            "mode": "SOLO_FOUNDER",
            "soloFounder": {
                "policyAdoption": "ADOPTED",
                "githubLogin": "founder",
                "commentId": "201",
            },
        }
        for role in (
            "repositoryOwner",
            "securityRiskOwner",
            "productionConfigurationAttestor",
        ):
            approval["approvalEvidence"][role] = {"githubLogin": "founder", "commentId": "201"}
        return approval

    def test_candidate_has_exactly_the_eight_reviewed_tuples(self) -> None:
        result = self.run_validator()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_expanded_candidate_has_exactly_sixteen_reviewed_tuples(self) -> None:
        result = self.run_validator(
            suppression=EXPANDED_CANDIDATE,
            findings=EXPANDED_FINDINGS,
            scope="expanded-sixteen",
        )
        self.assertEqual(0, result.returncode, result.stderr)

    def test_complete_synthetic_approval_can_validate_only_the_eight_tuples(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            result = self.run_validator(
                approval=self.approved_record(Path(temp)),
                findings=APPROVED_FINDINGS,
                mode="activation",
            )
        self.assertEqual(0, result.returncode, result.stderr)

    def test_github_comment_verification_requires_distinct_authenticated_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = json.loads(self.approved_record(Path(temp)).read_text())
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        comments = {
            "101": self.comment("owner", f"OWNER_AUTHORIZATION: APPROVE {approval['reviewedCommit']} {scope_hash} 2026-11-30T23:59:59Z"),
            "102": self.comment("independent", f"SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY {approval['reviewedCommit']} {scope_hash} 2026-11-30T23:59:59Z"),
            "103": self.comment("operator", "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED"),
        }
        VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, comment_id: comments[comment_id])

    def test_github_comment_verification_rejects_missing_scope_term(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = json.loads(self.approved_record(Path(temp)).read_text())
        comments = {
            "101": self.comment("owner", "OWNER_AUTHORIZATION: APPROVE"),
            "102": self.comment("independent", "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY"),
            "103": self.comment("operator", "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED"),
        }
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "missing required scope evidence"):
            VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, comment_id: comments[comment_id])

    def test_solo_founder_authenticated_comment_can_supply_separate_evidence_roles(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(Path(temp))
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        comment = self.comment(
            "founder",
            " ".join((
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
                approval["reviewedCommit"],
                scope_hash,
                approval["expiry"],
                self.exact_scope_terms(),
            )),
        )
        VALIDATOR_MODULE["validate_approval"](approval, "original-eight")
        VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, _comment_id: comment)

    def test_shared_identity_requires_adopted_solo_founder_policy(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = json.loads(self.approved_record(Path(temp)).read_text())
        for role in (
            "repositoryOwner",
            "securityRiskOwner",
            "productionConfigurationAttestor",
        ):
            approval["approvalEvidence"][role] = {"githubLogin": "founder", "commentId": "201"}
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "shared evidence identities"):
            VALIDATOR_MODULE["validate_approval"](approval, "original-eight")

    def test_solo_founder_comment_requires_explicit_consent_and_all_tuples(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(Path(temp))
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        incomplete = self.comment(
            "founder",
            " ".join((
                "SOLO_FOUNDER_GOVERNANCE: ADOPT",
                "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
                "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
                "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
                "OWNER_AUTHORIZATION: APPROVE",
                "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
                "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
                "production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
                approval["reviewedCommit"], scope_hash, approval["expiry"],
                "CVE-2026-53914 pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25",
            )),
        )
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "missing required scope evidence"):
            VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, _comment_id: incomplete)

    def test_github_comment_verification_rejects_unverified_owner_identity(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = json.loads(self.approved_record(Path(temp)).read_text())
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        comments = {
            "101": self.comment("impostor", f"OWNER_AUTHORIZATION: APPROVE {approval['reviewedCommit']} {scope_hash} {approval['expiry']}"),
            "102": self.comment("independent", f"SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY {approval['reviewedCommit']} {scope_hash} {approval['expiry']}"),
            "103": self.comment("operator", "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED"),
        }
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "author does not match"):
            VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, comment_id: comments[comment_id])

    def test_solo_founder_comment_requires_owner_association_and_current_edit_timestamp(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(Path(temp))
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        comment = self.comment(
            "founder",
            " ".join((
                "SOLO_FOUNDER_GOVERNANCE: ADOPT",
                "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
                "NO_INDEPENDENT_SECURITY_REVIEW",
                "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
                "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
                "OWNER_AUTHORIZATION: APPROVE",
                "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
                "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
                "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
                "production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files",
                "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
                "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
                "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
                approval["reviewedCommit"], scope_hash, approval["expiry"], self.exact_scope_terms(),
            )),
            author_association="MEMBER",
        )
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "not a repository owner"):
            VALIDATOR_MODULE["verify_github_approvals"](approval, CANDIDATE, VALIDATOR_MODULE["ORIGINAL_EIGHT"], lambda _repository, _comment_id: comment)

    def test_unrelated_critical_finding_remains_blocking(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            result = self.run_validator(
                approval=self.approved_record(directory),
                findings=[{"cve": "CVE-2099-0001", "purl": "pkg:maven/example/unrelated@1.0.0", "cvss": 9.8}],
                mode="activation",
            )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unrelated CVSS >= 8", result.stderr)

    def test_different_dependency_version_is_blocking(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            result = self.run_validator(
                approval=self.approved_record(directory),
                findings=[{"cve": "CVE-2026-47884", "purl": "pkg:maven/org.springframework/spring-core@6.2.20", "cvss": 9.8}],
                mode="activation",
            )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unrelated CVSS >= 8", result.stderr)

    def test_changed_package_version_in_candidate_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "changed-version.xml"
            changed.write_text(CANDIDATE.read_text().replace("6\\.2\\.19", "6\\.2\\.20", 1))
            result = self.run_validator(suppression=changed)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unapproved CVE", result.stderr)

    def test_same_cve_on_unapproved_third_purl_remains_blocking(self) -> None:
        result = self.run_validator(
            suppression=EXPANDED_CANDIDATE,
            findings=EXPANDED_FINDINGS + [{
                "cve": "CVE-2026-53914",
                "purl": "pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk8@1.9.25",
                "cvss": 9.8,
            }],
            scope="expanded-sixteen",
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unrelated CVSS >= 8", result.stderr)

    def test_incorrect_cve_purl_pair_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "wrong-pair.xml"
            changed.write_text(EXPANDED_CANDIDATE.read_text().replace(
                "kotlin\\-stdlib\\-jdk7@1\\.9\\.25$</packageUrl>\n    <cve>CVE-2026-53914</cve>",
                "kotlin\\-stdlib\\-jdk7@1\\.9\\.25$</packageUrl>\n    <cve>CVE-2026-47884</cve>",
                1,
            ))
            result = self.run_validator(suppression=changed, scope="expanded-sixteen")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unapproved CVE", result.stderr)

    def test_duplicate_expanded_tuple_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "duplicate.xml"
            changed.write_text(EXPANDED_CANDIDATE.read_text().replace(
                "</suppressions>",
                """  <suppress until=\"2026-11-30\">\n    <packageUrl regex=\"true\">^pkg:maven/org\\.springframework/spring\\-tx@6\\.2\\.19$</packageUrl>\n    <cve>CVE-2026-47884</cve>\n  </suppress>\n</suppressions>""",
            ))
            result = self.run_validator(suppression=changed, scope="expanded-sixteen")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("duplicate suppression", result.stderr)

    def test_missing_expanded_tuple_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "missing.xml"
            changed.write_text(EXPANDED_CANDIDATE.read_text().replace(
                """  <suppress until=\"2026-11-30\">\n    <notes>Pending amendment only: same embedded UnboundID LDAP listener prerequisite, separately reported against spring-security-web.</notes>\n    <packageUrl regex=\"true\">^pkg:maven/org\\.springframework\\.security/spring\\-security\\-web@6\\.5\\.11$</packageUrl>\n    <cve>CVE-2026-59270</cve>\n  </suppress>\n""",
                "",
            ))
            result = self.run_validator(suppression=changed, scope="expanded-sixteen")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("scope mismatch", result.stderr)

    def test_expanded_scope_comment_requires_each_exact_tuple_line(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(
                Path(temp),
            )
        approval["scope"] = "expanded-sixteen"
        approval["reviewedCommit"] = "db2ace78d080b86bd1520c7b027398557235c933"
        approval["approvalEvidence"]["candidateSha256"] = hashlib.sha256(EXPANDED_CANDIDATE.read_bytes()).hexdigest()
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        body = "\n".join((
            "SOLO_FOUNDER_GOVERNANCE: ADOPT",
            "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
            "NO_INDEPENDENT_SECURITY_REVIEW",
            "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
            "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
            "OWNER_AUTHORIZATION: APPROVE",
            "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
            "production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files",
            "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
            "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
            "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
            approval["reviewedCommit"], scope_hash, approval["expiry"],
            "\n".join(self.exact_expanded_scope_lines().splitlines()[:-1]),
        ))
        comment = self.comment("founder", body)
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "tuple scope mismatch"):
            VALIDATOR_MODULE["verify_github_approvals"](
                approval,
                EXPANDED_CANDIDATE,
                VALIDATOR_MODULE["EXPANDED_SIXTEEN"],
                lambda _repository, _comment_id: comment,
            )

    def test_expanded_scope_authenticated_comment_accepts_all_sixteen_exact_tuple_lines(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(Path(temp))
        approval["scope"] = "expanded-sixteen"
        approval["reviewedCommit"] = "db2ace78d080b86bd1520c7b027398557235c933"
        approval["approvalEvidence"]["candidateSha256"] = hashlib.sha256(EXPANDED_CANDIDATE.read_bytes()).hexdigest()
        scope_hash = approval["approvalEvidence"]["candidateSha256"]
        body = "\n".join((
            "SOLO_FOUNDER_GOVERNANCE: ADOPT",
            "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
            "NO_INDEPENDENT_SECURITY_REVIEW",
            "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
            "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
            "OWNER_AUTHORIZATION: APPROVE",
            "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
            "production SPRING_LDAP_EMBEDDED_* SPRING_EXPRESSION_COMPILER_MODE Environment Groups Secret Files",
            "LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED",
            "HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED",
            "FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED",
            approval["reviewedCommit"], scope_hash, approval["expiry"],
            self.exact_expanded_scope_lines(),
        ))
        comment = self.comment("founder", body)
        VALIDATOR_MODULE["validate_approval"](approval, "expanded-sixteen")
        VALIDATOR_MODULE["verify_github_approvals"](
            approval,
            EXPANDED_CANDIDATE,
            VALIDATOR_MODULE["EXPANDED_SIXTEEN"],
            lambda _repository, _comment_id: comment,
        )

    def test_candidate_sha_mismatch_remains_blocking(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = self.solo_founder_record(Path(temp))
        approval["approvalEvidence"]["candidateSha256"] = "0" * 64
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "does not bind"):
            VALIDATOR_MODULE["verify_github_approvals"](
                approval,
                CANDIDATE,
                VALIDATOR_MODULE["ORIGINAL_EIGHT"],
                lambda _repository, _comment_id: self.comment("founder", ""),
            )

    def test_unapproved_cve_in_candidate_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            changed = Path(temp) / "unapproved-cve.xml"
            changed.write_text(CANDIDATE.read_text().replace("CVE-2026-47884", "CVE-2099-0001", 1))
            result = self.run_validator(suppression=changed)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unapproved CVE", result.stderr)

    def test_expired_candidate_fails_closed(self) -> None:
        result = self.run_validator(as_of="2026-12-01T00:00:00Z")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("expired", result.stderr)

    def test_missing_approval_fails_closed(self) -> None:
        result = self.run_validator(mode="activation")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not APPROVED", result.stderr)

    def test_missing_production_attestation_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            result = self.run_validator(
                approval=self.approval_without_production_attestation(Path(temp)),
                mode="activation",
            )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("owner-attested with limitations", result.stderr)

    def test_missing_production_attestation_limitation_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            approval = json.loads(self.approved_record(Path(temp)).read_text())
        approval["productionConfiguration"]["limitations"] = ATTESTATION_LIMITATIONS[:2]
        with self.assertRaisesRegex(VALIDATOR_MODULE["PolicyError"], "does not record all limitations"):
            VALIDATOR_MODULE["validate_approval"](approval, "original-eight")

    def test_malformed_exception_xml_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            invalid = Path(temp) / "invalid.xml"
            invalid.write_text("<suppressions>")
            result = self.run_validator(suppression=invalid)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("cannot parse suppression XML", result.stderr)

    def test_cpe_or_cvss_wide_matcher_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            broadened = Path(temp) / "broadened.xml"
            broadened.write_text(CANDIDATE.read_text().replace("<cve>CVE-2026-53914</cve>", "<cpe>cpe:/a:jetbrains:kotlin</cpe><cve>CVE-2026-53914</cve>", 1))
            result = self.run_validator(suppression=broadened)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("broad or unsupported suppression matcher", result.stderr)
