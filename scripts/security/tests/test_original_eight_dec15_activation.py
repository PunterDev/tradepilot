#!/usr/bin/env python3
"""Regression coverage for the protected original-eight-dec15 handoff."""

from __future__ import annotations

import copy
import hashlib
import json
import runpy
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = ROOT / "scripts/security/validate_original_eight_dec15_activation.py"
MODULE = runpy.run_path(str(VALIDATOR))


class OriginalEightDecemberActivationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.candidate = self.root / "candidate.xml"
        self.candidate.write_text(self.candidate_xml())
        self.policy = MODULE["validate_candidate"].__globals__
        self.original_hash = self.policy["CANDIDATE_SHA256"]
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        self.head = "b" * 40
        self.now = datetime(2026, 11, 18, tzinfo=timezone.utc)

    def tearDown(self) -> None:
        self.policy["CANDIDATE_SHA256"] = self.original_hash
        self.temp.cleanup()

    @staticmethod
    def candidate_xml(extra: str = "") -> str:
        entries = "\n".join(
            "  <suppress until=\"2026-12-15\">\n"
            "    <notes>synthetic reviewed exception</notes>\n"
            f"    <packageUrl regex=\"true\">^{__import__('re').escape(purl)}$</packageUrl>\n"
            f"    <cve>{cve}</cve>\n"
            "  </suppress>"
            for cve, purl in sorted(MODULE["TUPLES"])
        )
        return (
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">\n"
            f"{entries}{extra}\n</suppressions>\n"
        )

    def approval(self) -> dict[str, object]:
        comment = {"githubLogin": "PunterDev", "commentId": str(MODULE["COMMENT_ID"])}
        return {
            "status": "APPROVED",
            "scope": "original-eight-dec15",
            "reviewedCommit": MODULE["REVIEWED_COMMIT"],
            "preparedBaselineCommit": MODULE["PREPARED_BASELINE"],
            "expiry": MODULE["EXPIRY"],
            "decisionTimestamp": MODULE["DECISION_TIMESTAMP"],
            "repositoryOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": MODULE["DECISION_TIMESTAMP"]},
            "securityRiskOwner": {"name": "PunterDev", "decision": "APPROVE", "approvedAt": MODULE["DECISION_TIMESTAMP"]},
            "productionConfiguration": {
                "status": "OWNER_ATTESTED_WITH_LIMITATIONS", "evidenceReference": "owner review",
                "limitations": sorted(MODULE["LIMITATIONS"]),
            },
            "governance": {"mode": "SOLO_FOUNDER", "soloFounder": {"policyAdoption": "ADOPTED", **comment}},
            "approvalEvidence": {
                "repository": MODULE["REPOSITORY"], "pullRequest": MODULE["PULL_REQUEST"],
                "reviewedPullRequestHeadSha": MODULE["REVIEWED_COMMIT"],
                "candidateSha256": self.policy["CANDIDATE_SHA256"],
                "repositoryOwner": comment, "securityRiskOwner": comment,
                "productionConfigurationAttestor": comment,
            },
        }

    def comment(self) -> dict[str, object]:
        terms = (
            "SOLO_FOUNDER_GOVERNANCE: ADOPT",
            "SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER",
            "NO_INDEPENDENT_SECURITY_REVIEW",
            "EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED",
            "TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER",
            "OWNER_AUTHORIZATION: APPROVE",
            "SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY",
            "PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS",
            "OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK",
            "original-eight-dec15", MODULE["REVIEWED_COMMIT"], self.policy["CANDIDATE_SHA256"], MODULE["EXPIRY"],
            *sorted(MODULE["LIMITATIONS"]),
        )
        tuples = "\n".join(f"- {cve} | {purl}" for cve, purl in sorted(MODULE["TUPLES"]))
        return {
            "id": MODULE["COMMENT_ID"], "user": {"login": "PunterDev"}, "author_association": "OWNER",
            "issue_url": f"https://api.github.com/repos/{MODULE['REPOSITORY']}/issues/{MODULE['PULL_REQUEST']}",
            "created_at": MODULE["DECISION_TIMESTAMP"], "updated_at": MODULE["DECISION_TIMESTAMP"],
            "body": "\n".join((*terms, tuples)),
        }

    def pull_request(self) -> dict[str, object]:
        repository = {"full_name": MODULE["REPOSITORY"]}
        return {
            "number": MODULE["PULL_REQUEST"], "state": "open",
            "base": {"ref": "master", "repo": repository},
            "head": {"sha": self.head, "repo": repository},
        }

    def comparison(self) -> dict[str, object]:
        return {
            "status": "ahead", "ahead_by": 1, "total_commits": 1,
            "merge_base_commit": {"sha": MODULE["REVIEWED_COMMIT"]},
            "commits": [{"sha": self.head}],
            "files": [
                {"filename": path, "status": status}
                for path, status in MODULE["ALLOWED_CHANGED_FILES"].items()
            ],
        }

    def write(self, name: str, value: object) -> Path:
        path = self.root / name
        path.write_text(json.dumps(value))
        return path

    def validate(self) -> Path:
        handoff = self.root / "handoff"
        MODULE["validate_candidate"](self.candidate, self.now)
        MODULE["validate_approval"](self.approval())
        MODULE["validate_comment"](self.comment())
        MODULE["validate_pull_request"](self.pull_request(), self.head)
        MODULE["validate_comparison"](self.comparison(), self.head)
        MODULE["write_handoff"](self.candidate, handoff, self.head)
        return handoff

    def test_fixed_production_policy_constants_are_specific(self) -> None:
        self.assertEqual(MODULE["PULL_REQUEST"], 16)
        self.assertEqual(MODULE["COMMENT_ID"], 6938531951)
        self.assertEqual(len(MODULE["TUPLES"]), 8)
        self.assertRegex(self.original_hash, r"^[0-9a-f]{64}$")
        self.assertNotIn(6, (MODULE["PULL_REQUEST"],))

    def test_exact_scope_creates_a_bound_handoff(self) -> None:
        handoff = self.validate()
        MODULE["verify_handoff"](handoff, self.head, self.now)
        self.assertEqual(hashlib.sha256((handoff / MODULE["HANDOFF_XML"]).read_bytes()).hexdigest(), self.policy["CANDIDATE_SHA256"])

    def test_changed_candidate_or_hash_fails_closed(self) -> None:
        self.candidate.write_text(self.candidate.read_text() + "<!-- changed -->")
        with self.assertRaisesRegex(MODULE["ValidationError"], "hash"):
            MODULE["validate_candidate"](self.candidate, self.now)

    def test_extra_or_missing_cve_purl_fails_closed(self) -> None:
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        self.candidate.write_text(self.candidate_xml().replace("</suppressions>", "  <suppress until=\"2026-12-15\"><notes>extra</notes><packageUrl regex=\"true\">^pkg:maven/example/extra@1$</packageUrl><cve>CVE-2099-0001</cve></suppress>\n</suppressions>"))
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_candidate"](self.candidate, self.now)
        self.candidate.write_text(self.candidate_xml().replace("<cve>CVE-2026-53914</cve>", "<cve>CVE-2099-0001</cve>", 1))
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_candidate"](self.candidate, self.now)

    def test_duplicate_suppression_matcher_fails_closed(self) -> None:
        self.candidate.write_text(self.candidate_xml().replace(
            "<cve>CVE-2026-53914</cve>",
            "<cve>CVE-2026-53914</cve><cve>CVE-2026-53914</cve>",
            1,
        ))
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        with self.assertRaisesRegex(MODULE["ValidationError"], "structure"):
            MODULE["validate_candidate"](self.candidate, self.now)

    def test_sixteen_rule_candidate_fails_closed(self) -> None:
        self.candidate.write_text(self.candidate_xml().replace(
            "</suppressions>",
            "  <suppress until=\"2026-12-15\"><notes>unapproved amendment</notes>"
            "<packageUrl regex=\"true\">^pkg:maven/org\\.jetbrains\\.kotlin/kotlin\\-stdlib\\-jdk7@1\\.9\\.25$</packageUrl>"
            "<cve>CVE-2026-53914</cve></suppress>\n</suppressions>",
        ))
        self.policy["CANDIDATE_SHA256"] = hashlib.sha256(self.candidate.read_bytes()).hexdigest()
        with self.assertRaisesRegex(MODULE["ValidationError"], "scope"):
            MODULE["validate_candidate"](self.candidate, self.now)

    def test_expired_candidate_and_missing_approval_fail_closed(self) -> None:
        with self.assertRaisesRegex(MODULE["ValidationError"], "expired"):
            MODULE["validate_candidate"](self.candidate, datetime(2026, 12, 16, tzinfo=timezone.utc))
        with self.assertRaisesRegex(MODULE["ValidationError"], "cannot read JSON"):
            MODULE["read_json"](self.root / "missing-approval.json")

    def test_forged_or_non_owner_comment_fails_closed(self) -> None:
        forged = self.comment()
        forged["user"] = {"login": "impostor"}
        with self.assertRaisesRegex(MODULE["ValidationError"], "identity"):
            MODULE["validate_comment"](forged)
        non_owner = self.comment()
        non_owner["author_association"] = "MEMBER"
        with self.assertRaisesRegex(MODULE["ValidationError"], "association"):
            MODULE["validate_comment"](non_owner)

    def test_wrong_pr_reviewed_commit_or_historical_pr_six_fails_closed(self) -> None:
        wrong_pr = self.pull_request()
        wrong_pr["number"] = 6
        with self.assertRaisesRegex(MODULE["ValidationError"], "identity"):
            MODULE["validate_pull_request"](wrong_pr, self.head)
        approval = self.approval()
        approval["approvalEvidence"]["pullRequest"] = 6
        with self.assertRaisesRegex(MODULE["ValidationError"], "PR #16"):
            MODULE["validate_approval"](approval)
        approval = self.approval()
        approval["reviewedCommit"] = "a" * 40
        with self.assertRaisesRegex(MODULE["ValidationError"], "binding"):
            MODULE["validate_approval"](approval)

    def test_security_relevant_post_review_change_fails_closed(self) -> None:
        comparison = self.comparison()
        comparison["files"].append({"filename": "backend/pom.xml", "status": "modified"})
        comparison["ahead_by"] = comparison["total_commits"] = 2
        with self.assertRaisesRegex(MODULE["ValidationError"], "activation commit"):
            MODULE["validate_comparison"](comparison, self.head)

    def test_handoff_missing_substituted_or_unsafe_fails_closed(self) -> None:
        handoff = self.validate()
        (handoff / MODULE["HANDOFF_XML"]).write_text("replacement")
        with self.assertRaisesRegex(MODULE["ValidationError"], "hash"):
            MODULE["verify_handoff"](handoff, self.head, self.now)
        handoff = self.validate() if not handoff.exists() else self.root / "other-handoff"
        if not handoff.exists():
            MODULE["write_handoff"](self.candidate, handoff, self.head)
        (handoff / "unexpected").write_text("x")
        with self.assertRaisesRegex(MODULE["ValidationError"], "inventory"):
            MODULE["verify_handoff"](handoff, self.head, self.now)
        with self.assertRaisesRegex(MODULE["ValidationError"], "directory"):
            MODULE["verify_handoff"](self.root / "missing-handoff", self.head, self.now)


if __name__ == "__main__":
    unittest.main()
