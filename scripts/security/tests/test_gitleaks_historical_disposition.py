from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
IGNORE = ROOT / ".gitleaksignore"

EXPECTED_FINGERPRINTS = {
    "5f17bd651789341fb661c10518d269e506be0877:backend/src/test/java/com/tradepilot/AuthHardeningTests.java:generic-api-key:47",
    "5f17bd651789341fb661c10518d269e506be0877:backend/src/test/java/com/tradepilot/AuthHardeningTests.java:generic-api-key:184",
    "11f0da307ad0033a7e0665c93fab4bef11d51769:backend/src/test/java/com/tradepilot/RuntimeSmokeTests.java:generic-api-key:51",
    "3dbd3bfe262835cc1080ae30f4f2634977482dcc:backend/src/test/resources/application-test.yml:generic-api-key:13",
    "81d6b5cb75910fb9e895233b9f7cdd9aab6e59c5:scripts/run-account-survival-fullstack.sh:generic-api-key:43",
    "1ac1ef61943a2b2185025bc17b0291c9e4cd352d:scripts/run-account-survival-fullstack.sh:generic-api-key:43",
    "a4249c682d6acc61619e0c56161444dcfb73958a:scripts/run-account-survival-fullstack.sh:generic-api-key:43",
}


class GitleaksHistoricalDispositionTests(unittest.TestCase):
    def test_active_ignore_is_limited_to_the_seven_owner_approved_fingerprints(self) -> None:
        entries = {
            line.strip()
            for line in IGNORE.read_text().splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        }
        self.assertEqual(EXPECTED_FINGERPRINTS, entries)
        for fingerprint in entries:
            self.assertRegex(fingerprint, r"^[0-9a-f]{40}:.+:[^:]+:[0-9]+$")
