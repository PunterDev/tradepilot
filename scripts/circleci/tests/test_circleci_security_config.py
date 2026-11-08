from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
CONFIG = ROOT / ".circleci/config.yml"


class CircleCiSecurityConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.config = CONFIG.read_text()

    def test_dependency_check_remains_fail_closed(self) -> None:
        self.assertIn("org.owasp:dependency-check-maven:12.2.2:update-only", self.config)
        self.assertIn("org.owasp:dependency-check-maven:12.2.2:check", self.config)
        self.assertIn("-DfailBuildOnCVSS=8", self.config)
        self.assertGreaterEqual(self.config.count("-DfailOnError=true"), 2)
        self.assertIn("-DfailBuildOnUnusedSuppressionRule=true", self.config)
        self.assertIn('readonly GITLEAKS_VERSION="v8.18.4"', self.config)
        self.assertIn("ba6dbb656933921c775ee5a2d1c13a91046e7952e9d919f9bac4cec61d628e7d", self.config)
        self.assertIn("cimg/openjdk:17.0.13@sha256:402f246164077f437f8a19d586137038e83beb63d21bedbc89cd5d11ee20b3e6", self.config)
        self.assertIn("cimg/node:22.15.1@sha256:2914f60f3ffaa8427a9038d5ade497d5e01e3baa974eb78d690c3b3d669c70f6", self.config)
        self.assertIn("semgrep/semgrep:1.100.0@sha256:62c5c71d6da15096bdde79035782d30be8b89a6e2b5fb9b3228d37c3316fa9d4", self.config)

    def test_completed_nvd_update_is_saved_before_cvss_analysis(self) -> None:
        update = self.config.index("update-only")
        save = self.config.index("save_cache:")
        check = self.config.index("dependency-check-maven:12.2.2:check")
        self.assertLess(update, save)
        self.assertLess(save, check)
        self.assertIn("caches: 1d", self.config)
        self.assertIn("{{ .BuildNum }}", self.config)
        self.assertIn("/tmp/dependency-check-exit-status", self.config)
        self.assertLess(self.config.index("store_artifacts:"), self.config.index("Enforce retained Dependency-Check result"))

    def test_pending_candidate_is_proposal_only(self) -> None:
        proposal = self.config.index("security-policy-proposal:")
        activation = self.config.index("security-policy-activation:")
        pending_reference = self.config.index("dependency-check-expanded-sixteen-dec15-candidate.xml")
        active_reference = self.config.index("dependency-check-final-eight-candidate.xml")
        self.assertLess(proposal, pending_reference)
        self.assertLess(pending_reference, activation)
        self.assertGreater(active_reference, activation)

    def test_workflows_are_split_by_github_app_draft_state(self) -> None:
        self.assertIn("- equal: [pull_request, << pipeline.event.name >>]", self.config)
        self.assertIn("- equal: [true, << pipeline.event.github.pull_request.draft >>]", self.config)
        self.assertIn("- equal: [false, << pipeline.event.github.pull_request.draft >>]", self.config)

    def test_full_scans_depend_on_the_activation_gate(self) -> None:
        activation = self.config[self.config.index("activation:"):]
        self.assertIn("- security-policy-activation:\n          context: tradepilot-security-activation", activation)
        self.assertIn("- dependency-check:\n          requires: [security-policy-activation]\n          context: tradepilot-security-activation", activation)
        for job in ("gitleaks", "dependency-check", "frontend-audit", "semgrep"):
            self.assertIn(f"- {job}:\n          requires: [security-policy-activation]", activation)
        proposal = self.config[self.config.index("proposal:"):self.config.index("activation:")]
        self.assertNotIn("tradepilot-security-activation", proposal)
