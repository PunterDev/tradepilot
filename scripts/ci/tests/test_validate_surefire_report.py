from __future__ import annotations

import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).resolve().parents[1] / "validate_surefire_report.py"
SPEC = importlib.util.spec_from_file_location("validate_surefire_report", MODULE_PATH)
assert SPEC and SPEC.loader
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


class ValidateSurefireReportTests(unittest.TestCase):
    EXPECTED_SUITE = "com.tradepilot.FlywayPostgresBaselineIntegrationTest"
    EXPECTED_CLASS = "com.tradepilot.FlywayPostgresBaselineIntegrationTest"
    REQUIRED = {"freshMigration", "v030Upgrade", "duplicateCurrentRollback"}

    def report(self, body: str) -> Path:
        directory = Path(self.enterContext(tempfile.TemporaryDirectory()))
        path = directory / "TEST-example.xml"
        path.write_text(body, encoding="utf-8")
        return path

    def valid_xml(self) -> str:
        return f"""\
<testsuite name="{self.EXPECTED_SUITE}" tests="3" failures="0" errors="0" skipped="0">
  <testcase classname="{self.EXPECTED_CLASS}" name="freshMigration"/>
  <testcase classname="{self.EXPECTED_CLASS}" name="v030Upgrade"/>
  <testcase classname="{self.EXPECTED_CLASS}" name="duplicateCurrentRollback"/>
</testsuite>
"""

    def validate(self, body: str):
        return VALIDATOR.validate_report(
            self.report(body),
            self.REQUIRED,
            self.EXPECTED_SUITE,
            self.EXPECTED_CLASS,
        )

    def cli_command(self, report: Path) -> list[str]:
        command = [
            sys.executable,
            str(MODULE_PATH),
            "--report",
            str(report),
            "--expected-suite",
            self.EXPECTED_SUITE,
            "--expected-class",
            self.EXPECTED_CLASS,
        ]
        for required_test in sorted(self.REQUIRED):
            command.extend(("--required-test", required_test))
        return command

    def run_cli(self, body: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            self.cli_command(self.report(body)),
            capture_output=True,
            check=False,
            text=True,
        )

    def with_additional_test(self, body: str, name: str = "additionalMigrationCheck") -> str:
        testcase = f'  <testcase classname="{self.EXPECTED_CLASS}" name="{name}"/>\n'
        return body.replace('tests="3"', 'tests="4"').replace("</testsuite>", testcase + "</testsuite>")

    def test_accepts_complete_successful_required_evidence(self):
        counts = self.validate(self.valid_xml())
        self.assertEqual({"tests": 3, "failures": 0, "errors": 0, "skipped": 0}, counts)

    def test_accepts_additional_unique_test_from_expected_class(self):
        counts = self.validate(self.with_additional_test(self.valid_xml()))
        self.assertEqual(4, counts["tests"])

    def test_rejects_missing_report(self):
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "Missing Surefire report"):
            VALIDATOR.validate_report(
                Path("does-not-exist.xml"), self.REQUIRED, self.EXPECTED_SUITE, self.EXPECTED_CLASS
            )

    def test_rejects_malformed_report(self):
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "Malformed Surefire report"):
            self.validate("<testsuite>")

    def test_rejects_empty_report(self):
        xml = (
            f'<testsuite name="{self.EXPECTED_SUITE}" tests="0" '
            'failures="0" errors="0" skipped="0"/>'
        )
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "no executed test cases"):
            self.validate(xml)

    def test_rejects_missing_required_test(self):
        xml = self.valid_xml().replace('name="duplicateCurrentRollback"', 'name="otherTest"')
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "duplicateCurrentRollback"):
            self.validate(xml)

    def test_rejects_wrong_suite_name(self):
        xml = self.valid_xml().replace(f'name="{self.EXPECTED_SUITE}"', 'name="WrongSuite"', 1)
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "suite identity"):
            self.validate(xml)

    def test_rejects_wrong_testcase_classname(self):
        xml = self.valid_xml().replace(f'classname="{self.EXPECTED_CLASS}"', 'classname="wrong.Class"', 1)
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "class identity"):
            self.validate(xml)

    def test_rejects_wrong_suite_and_testcase_class_identity(self):
        xml = self.valid_xml().replace(
            f'name="{self.EXPECTED_SUITE}"', 'name="WrongSuite"', 1
        ).replace(f'classname="{self.EXPECTED_CLASS}"', 'classname="wrong.Class"')
        with self.assertRaises(VALIDATOR.ReportValidationError):
            self.validate(xml)

    def test_rejects_missing_suite_name(self):
        xml = self.valid_xml().replace(f' name="{self.EXPECTED_SUITE}"', "", 1)
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "suite identity"):
            self.validate(xml)

    def test_rejects_missing_testcase_classname(self):
        xml = self.valid_xml().replace(f' classname="{self.EXPECTED_CLASS}"', "", 1)
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "class identity"):
            self.validate(xml)

    def test_rejects_duplicate_required_testcase_identity(self):
        duplicate = f'  <testcase classname="{self.EXPECTED_CLASS}" name="freshMigration"/>\n'
        xml = self.valid_xml().replace('tests="3"', 'tests="4"').replace("</testsuite>", duplicate + "</testsuite>")
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "Duplicate testcase"):
            self.validate(xml)

    def test_rejects_duplicate_nonmandatory_testcase_identity(self):
        extra = f'  <testcase classname="{self.EXPECTED_CLASS}" name="additionalCheck"/>\n'
        xml = self.valid_xml().replace('tests="3"', 'tests="5"').replace("</testsuite>", extra + extra + "</testsuite>")
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "Duplicate testcase"):
            self.validate(xml)

    def test_rejects_failed_errored_or_skipped_required_test(self):
        outcome_attributes = {"failure": "failures", "error": "errors", "skipped": "skipped"}
        for outcome, attribute in outcome_attributes.items():
            with self.subTest(outcome=outcome):
                xml = self.valid_xml().replace(
                    f'{attribute}="0"', f'{attribute}="1"'
                ).replace(
                    'name="freshMigration"/>', f'name="freshMigration"><{outcome}/></testcase>'
                )
                with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "did not complete cleanly"):
                    self.validate(xml)

    def test_rejects_count_without_matching_testcase_evidence(self):
        xml = self.valid_xml().replace('tests="3"', 'tests="4"')
        with self.assertRaisesRegex(VALIDATOR.ReportValidationError, "does not match testcase evidence"):
            self.validate(xml)

    def test_rejects_missing_or_invalid_counts(self):
        missing = self.valid_xml().replace(' errors="0"', "")
        invalid = self.valid_xml().replace('tests="3"', 'tests="many"')
        for xml in (missing, invalid):
            with self.subTest(xml=xml):
                with self.assertRaises(VALIDATOR.ReportValidationError):
                    self.validate(xml)

    def test_cli_accepts_valid_and_additional_unique_evidence(self):
        for xml in (self.valid_xml(), self.with_additional_test(self.valid_xml())):
            with self.subTest(tests=xml.count("<testcase")):
                result = self.run_cli(xml)
                self.assertEqual(0, result.returncode, result.stderr)

    def test_cli_rejects_every_invalid_evidence_category(self):
        valid = self.valid_xml()
        duplicate_required = valid.replace('tests="3"', 'tests="4"').replace(
            "</testsuite>",
            f'  <testcase classname="{self.EXPECTED_CLASS}" name="freshMigration"/>\n</testsuite>',
        )
        extra = f'  <testcase classname="{self.EXPECTED_CLASS}" name="additionalCheck"/>\n'
        duplicate_extra = valid.replace('tests="3"', 'tests="5"').replace(
            "</testsuite>", extra + extra + "</testsuite>"
        )
        invalid_reports = {
            "wrong_suite": valid.replace(f'name="{self.EXPECTED_SUITE}"', 'name="WrongSuite"', 1),
            "wrong_class": valid.replace(f'classname="{self.EXPECTED_CLASS}"', 'classname="wrong.Class"', 1),
            "wrong_suite_and_class": valid.replace(
                f'name="{self.EXPECTED_SUITE}"', 'name="WrongSuite"', 1
            ).replace(f'classname="{self.EXPECTED_CLASS}"', 'classname="wrong.Class"'),
            "duplicate_required": duplicate_required,
            "duplicate_extra": duplicate_extra,
            "missing_suite": valid.replace(f' name="{self.EXPECTED_SUITE}"', "", 1),
            "missing_class": valid.replace(f' classname="{self.EXPECTED_CLASS}"', "", 1),
            "missing_required": valid.replace('name="duplicateCurrentRollback"', 'name="otherTest"'),
            "failed": valid.replace('failures="0"', 'failures="1"').replace(
                'name="freshMigration"/>', 'name="freshMigration"><failure/></testcase>'
            ),
            "errored": valid.replace('errors="0"', 'errors="1"').replace(
                'name="freshMigration"/>', 'name="freshMigration"><error/></testcase>'
            ),
            "skipped": valid.replace('skipped="0"', 'skipped="1"').replace(
                'name="freshMigration"/>', 'name="freshMigration"><skipped/></testcase>'
            ),
            "malformed": "<testsuite>",
            "empty": (
                f'<testsuite name="{self.EXPECTED_SUITE}" tests="0" '
                'failures="0" errors="0" skipped="0"/>'
            ),
            "inconsistent_count": valid.replace('tests="3"', 'tests="4"'),
        }
        for category, xml in invalid_reports.items():
            with self.subTest(category=category):
                result = self.run_cli(xml)
                self.assertNotEqual(0, result.returncode, result.stdout)

        missing_report = Path(self.enterContext(tempfile.TemporaryDirectory())) / "missing.xml"
        result = subprocess.run(
            self.cli_command(missing_report), capture_output=True, check=False, text=True
        )
        self.assertNotEqual(0, result.returncode, result.stdout)


if __name__ == "__main__":
    unittest.main()
