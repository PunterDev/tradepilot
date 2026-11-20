#!/usr/bin/env python3
"""Regression coverage for the two-package protected suppression handoff."""

from __future__ import annotations

import copy
import hashlib
import json
import re
import runpy
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "scripts/security/validate_second_eight_dec15_activation.py"
MODULE = runpy.run_path(str(VALIDATOR))
ORIGINAL = MODULE["ORIGINAL"]


class SecondEightDecemberActivationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.original_candidate = self.root / "original.xml"
        self.second_candidate = self.root / "second.xml"
        self.original_candidate.write_text(self.candidate_xml(ORIGINAL.TUPLES))
        self.second_candidate.write_text(self.candidate_xml(MODULE["SECOND_TUPLES"]))
        self.policy = MODULE["validate_second_candidate"].__globals__
        self.original_hash = ORIGINAL.CANDIDATE_SHA256
        self.second_hash = self.policy["SECOND_CANDIDATE_SHA256"]
        ORIGINAL.CANDIDATE_SHA256 = self.digest(self.original_candidate)
        self.policy["SECOND_CANDIDATE_SHA256"] = self.digest(self.second_candidate)
        self.head = "c" * 40
        self.now = datetime(2026, 11, 20, tzinfo=timezone.utc)

    def tearDown(self) -> None:
        ORIGINAL.CANDIDATE_SHA256 = self.original_hash
        self.policy["SECOND_CANDIDATE_SHA256"] = self.second_hash
        self.temp.cleanup()

    @staticmethod
    def digest(path: Path) -> str:
        return hashlib.sha256(path.read_bytes()).hexdigest()

    @staticmethod
    def candidate_xml(tuples: object, extra: str = "") -> str:
        entries = "\n".join(
            "  <suppress until=\"2026-12-15\">\n"
            "    <notes>synthetic reviewed exception</notes>\n"
            f"    <packageUrl regex=\"true\">^{re.escape(purl)}$</packageUrl>\n"
            f"    <cve>{cve}</cve>\n"
            "  </suppress>"
            for cve, purl in sorted(tuples)
        )
        return (
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">\n"
            f"{entries}{extra}\n</suppressions>\n"
        )

    def comment_reference(self, comment_id: int) -> dict[str, str]:
        return {"githubLogin": "PunterDev", "commentId": str(comment_id)}

    def original_approval(self) -> dict[str, object]:
        reference = self.comment_reference(ORIGINAL.COMMENT_ID)
        return {
            "status": "APPROVED", "scope": "original-eight-dec15",
            "reviewedCommit": ORIGINAL.REVIEWED_COMMIT,
            "preparedBaselineCommit": ORIGINAL.PREPARED_BASELINE,
            "expiry": ORIGINAL.EXPIRY, "decisionTimestamp": ORIGINAL.DECISION_TIMESTAMP,
            "repositoryOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": ORIGINAL.DECISION_TIMESTAMP},
            "securityRiskOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": ORIGINAL.DECISION_TIMESTAMP},
            "productionConfiguration": {"status": "OWNER_ATTESTED_WITH_LIMITATIONS", "evidenceReference": "owner review", "limitations": sorted(ORIGINAL.LIMITATIONS)},
            "governance": {"mode": "SOLO_FOUNDER", "soloFounder": {"policyAdoption": "ADOPTED", **reference}},
            "approvalEvidence": {
                "repository": ORIGINAL.REPOSITORY, "pullRequest": ORIGINAL.PULL_REQUEST,
                "reviewedPullRequestHeadSha": ORIGINAL.REVIEWED_COMMIT,
                "candidateSha256": ORIGINAL.CANDIDATE_SHA256,
                "repositoryOwner": reference, "securityRiskOwner": reference,
                "productionConfigurationAttestor": reference,
            },
        }

    def original_comment(self) -> dict[str, object]:
        terms = (
            "SOLO_FOUNDER_GOVERNANCE: ADOPT", "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
            "NO_INDEPENDENT_SECURITY_REVIEW", "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
            "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER", "OWNER_AUTHORIZATION: APPROVE",
            "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK", "original-eight-dec15", ORIGINAL.REVIEWED_COMMIT,
            ORIGINAL.CANDIDATE_SHA256, ORIGINAL.EXPIRY, *sorted(ORIGINAL.LIMITATIONS),
        )
        tuples = "\n".join(f"- {cve} | {purl}" for cve, purl in sorted(ORIGINAL.TUPLES))
        return {
            "id": ORIGINAL.COMMENT_ID, "user": {"login": "PunterDev"}, "author_association": "OWNER",
            "issue_url": f"https://api.github.com/repos/{ORIGINAL.REPOSITORY}/issues/{ORIGINAL.PULL_REQUEST}",
            "created_at": ORIGINAL.DECISION_TIMESTAMP, "updated_at": ORIGINAL.DECISION_TIMESTAMP,
            "body": "\n".join((*terms, tuples)),
        }

    def second_approval(self) -> dict[str, object]:
        timestamp = "2026-11-19T00:00:00Z"
        reference = self.comment_reference(MODULE["SECOND_COMMENT_ID"])
        return {
            "status": "APPROVED", "scope": "second-eight-transitive-dec15",
            "reviewedCommit": MODULE["ORIGINAL_ACTIVATION_COMMIT"],
            "preparedBaselineCommit": MODULE["ORIGINAL_ACTIVATION_COMMIT"],
            "expiry": MODULE["EXPIRY"], "decisionTimestamp": timestamp,
            "repositoryOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": timestamp},
            "securityRiskOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": timestamp},
            "productionConfiguration": {"status": "OWNER_ATTESTED_WITH_LIMITATIONS", "evidenceReference": "owner review", "limitations": sorted(MODULE["LIMITATIONS"])},
            "governance": {"mode": "SOLO_FOUNDER", "soloFounder": {"policyAdoption": "ADOPTED", **reference}},
            "approvalEvidence": {
                "repository": MODULE["REPOSITORY"], "pullRequest": MODULE["PULL_REQUEST"],
                "reviewedPullRequestHeadSha": MODULE["ORIGINAL_ACTIVATION_COMMIT"],
                "candidateSha256": self.policy["SECOND_CANDIDATE_SHA256"],
                "repositoryOwner": reference, "securityRiskOwner": reference,
                "productionConfigurationAttestor": reference,
            },
        }

    def second_comment(self) -> dict[str, object]:
        approval = self.second_approval()
        terms = (
            "SOLO_FOUNDER_GOVERNANCE: ADOPT", "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
            "NO_INDEPENDENT_SECURITY_REVIEW", "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
            "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER", "OWNER_AUTHORIZATION: APPROVE",
            "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK", "second-eight-transitive-dec15",
            MODULE["ORIGINAL_ACTIVATION_COMMIT"], self.policy["SECOND_CANDIDATE_SHA256"], MODULE["EXPIRY"],
            *sorted(MODULE["LIMITATIONS"]),
        )
        tuples = "\n".join(f"- {cve} | {purl}" for cve, purl in sorted(MODULE["SECOND_TUPLES"]))
        return {
            "id": MODULE["SECOND_COMMENT_ID"], "user": {"login": "PunterDev"}, "author_association": "OWNER",
            "issue_url": f"https://api.github.com/repos/{MODULE['REPOSITORY']}/issues/{MODULE['PULL_REQUEST']}",
            "created_at": approval["decisionTimestamp"], "updated_at": approval["decisionTimestamp"],
            "body": "\n".join((*terms, tuples)),
        }

    def pull_request(self) -> dict[str, object]:
        repository = {"full_name": MODULE["REPOSITORY"]}
        return {"number": 16, "state": "open", "base": {"ref": "master", "repo": repository}, "head": {"sha": self.head, "repo": repository}}

    def stage_a_comparison(self) -> dict[str, object]:
        return {
            "status": "ahead", "ahead_by": 1, "total_commits": 1,
            "merge_base_commit": {"sha": ORIGINAL.REVIEWED_COMMIT},
            "commits": [{"sha": MODULE["ORIGINAL_ACTIVATION_COMMIT"]}],
            "files": [{"filename": path, "status": status} for path, status in ORIGINAL.ALLOWED_CHANGED_FILES.items()],
        }

    def stage_b_comparison(self) -> dict[str, object]:
        return {
            "status": "ahead", "ahead_by": 1, "total_commits": 1,
            "merge_base_commit": {"sha": MODULE["ORIGINAL_ACTIVATION_COMMIT"]}, "commits": [{"sha": self.head}],
            "files": [{"filename": path, "status": status} for path, status in MODULE["SECOND_ALLOWED_CHANGED_FILES"].items()],
        }

    def write(self, name: str, value: object) -> Path:
        path = self.root / name
        path.write_text(json.dumps(value))
        return path

    def validate(self) -> Path:
        handoff = self.root / "handoff"
        MODULE["require_original"](ORIGINAL.validate_candidate, self.original_candidate, self.now)
        MODULE["require_original"](ORIGINAL.validate_approval, self.original_approval())
        MODULE["require_original"](ORIGINAL.validate_comment, self.original_comment())
        MODULE["require_original"](ORIGINAL.validate_comparison, self.stage_a_comparison(), MODULE["ORIGINAL_ACTIVATION_COMMIT"])
        MODULE["validate_second_candidate"](self.second_candidate, self.now)
        timestamp = MODULE["validate_second_approval"](self.second_approval(), self.now)
        MODULE["validate_second_comment"](self.second_comment(), timestamp, self.now)
        MODULE["validate_current_pull_request"](self.pull_request(), self.head)
        MODULE["validate_second_comparison"](self.stage_b_comparison(), self.head)
        MODULE["write_handoff"](self.original_candidate, self.second_candidate, handoff, self.head)
        return handoff

    def test_exact_two_scope_handoff_is_bound_and_reverifiable(self) -> None:
        handoff = self.validate()
        MODULE["verify_handoff"](handoff, self.head, self.now)
        self.assertEqual(len(MODULE["SECOND_TUPLES"]), 8)
        self.assertEqual(len(MODULE["ORIGINAL"].TUPLES | MODULE["SECOND_TUPLES"]), 16)

    def test_candidate_hash_scope_and_regex_fail_closed(self) -> None:
        self.second_candidate.write_text(self.second_candidate.read_text().replace("^pkg:maven", "pkg:maven", 1))
        self.policy["SECOND_CANDIDATE_SHA256"] = self.digest(self.second_candidate)
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_second_candidate"](self.second_candidate, self.now)
        self.second_candidate.write_text(self.candidate_xml(MODULE["SECOND_TUPLES"], "<suppress until=\"2026-12-15\"><notes>x</notes><packageUrl regex=\"true\">^pkg:maven/example/extra@1$</packageUrl><cve>CVE-2099-0001</cve></suppress>"))
        self.policy["SECOND_CANDIDATE_SHA256"] = self.digest(self.second_candidate)
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_second_candidate"](self.second_candidate, self.now)

    def test_forged_owner_comment_timestamp_and_tuple_set_fail_closed(self) -> None:
        comment = self.second_comment()
        comment["author_association"] = "MEMBER"
        timestamp = MODULE["validate_second_approval"](self.second_approval(), self.now)
        with self.assertRaisesRegex(MODULE["ValidationError"], "association"):
            MODULE["validate_second_comment"](comment, timestamp, self.now)
        comment = self.second_comment()
        comment["created_at"] = "2026-11-18T00:00:00Z"
        comment["updated_at"] = comment["created_at"]
        with self.assertRaisesRegex(MODULE["ValidationError"], "timestamp"):
            MODULE["validate_second_comment"](comment, timestamp, self.now)
        comment = self.second_comment()
        comment["body"] = comment["body"].replace("spring-security-web", "spring-security-core")
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_second_comment"](comment, timestamp, self.now)

    def test_missing_approval_and_duplicate_or_unsupported_matcher_fail_closed(self) -> None:
        approval = self.second_approval()
        del approval["productionConfiguration"]
        with self.assertRaisesRegex(MODULE["ValidationError"], "production configuration"):
            MODULE["validate_second_approval"](approval, self.now)
        duplicate = re.search(r"  <suppress until=.*?  </suppress>", self.second_candidate.read_text(), re.DOTALL)
        self.assertIsNotNone(duplicate)
        self.second_candidate.write_text(self.second_candidate.read_text().replace(
            "</suppressions>", duplicate.group(0) + "\n</suppressions>", 1,
        ))
        self.policy["SECOND_CANDIDATE_SHA256"] = self.digest(self.second_candidate)
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_second_candidate"](self.second_candidate, self.now)
        self.second_candidate.write_text(self.candidate_xml(MODULE["SECOND_TUPLES"]).replace(
            "</notes>", "</notes><cpe>cpe:/a:example</cpe>", 1,
        ))
        self.policy["SECOND_CANDIDATE_SHA256"] = self.digest(self.second_candidate)
        with self.assertRaisesRegex(MODULE["ValidationError"], "unsupported"):
            MODULE["validate_second_candidate"](self.second_candidate, self.now)

    def test_wrong_pr_hash_and_reviewed_head_fail_closed(self) -> None:
        pull_request = self.pull_request()
        pull_request["number"] = 17
        with self.assertRaisesRegex(MODULE["ValidationError"], "identity"):
            MODULE["validate_current_pull_request"](pull_request, self.head)
        approval = self.second_approval()
        approval["approvalEvidence"]["candidateSha256"] = "0" * 64
        with self.assertRaisesRegex(MODULE["ValidationError"], "binding"):
            MODULE["validate_second_approval"](approval, self.now)
        approval = self.second_approval()
        approval["reviewedCommit"] = "0" * 40
        with self.assertRaisesRegex(MODULE["ValidationError"], "binding"):
            MODULE["validate_second_approval"](approval, self.now)

    def test_modified_original_stage_or_extra_second_commit_fail_closed(self) -> None:
        comparison = self.stage_a_comparison()
        comparison["files"].append({"filename": "backend/pom.xml", "status": "modified"})
        with self.assertRaisesRegex(MODULE["ValidationError"], "original activation evidence"):
            MODULE["require_original"](ORIGINAL.validate_comparison, comparison, MODULE["ORIGINAL_ACTIVATION_COMMIT"])
        comparison = self.stage_b_comparison()
        comparison["ahead_by"] = comparison["total_commits"] = 2
        comparison["files"].append({"filename": "backend/pom.xml", "status": "modified"})
        with self.assertRaisesRegex(MODULE["ValidationError"], "ancestry|unreviewed"):
            MODULE["validate_second_comparison"](comparison, self.head)
        comparison = self.stage_b_comparison()
        comparison["files"][0]["status"] = "modified"
        with self.assertRaisesRegex(MODULE["ValidationError"], "unreviewed"):
            MODULE["validate_second_comparison"](comparison, self.head)

    def test_expired_missing_or_substituted_handoff_fails_closed(self) -> None:
        with self.assertRaisesRegex(MODULE["ValidationError"], "expired"):
            MODULE["validate_second_candidate"](self.second_candidate, datetime(2026, 12, 16, tzinfo=timezone.utc))
        handoff = self.validate()
        (handoff / MODULE["HANDOFF_XML"]).write_text("substituted")
        with self.assertRaisesRegex(MODULE["ValidationError"], "combined suppression XML"):
            MODULE["verify_handoff"](handoff, self.head, self.now)
        with self.assertRaisesRegex(MODULE["ValidationError"], "directory"):
            MODULE["verify_handoff"](self.root / "missing", self.head, self.now)

    def test_fixed_policy_has_no_pr_controlled_inputs(self) -> None:
        self.assertEqual(MODULE["PULL_REQUEST"], 16)
        self.assertEqual(MODULE["SECOND_COMMENT_ID"], 7031996454)
        self.assertEqual(MODULE["ORIGINAL_ACTIVATION_COMMIT"], "33706d9db11b120df8c20071617b7857704e35ee")
        self.assertRegex(self.second_hash, r"^[0-9a-f]{64}$")


if __name__ == "__main__":
    unittest.main()
