#!/usr/bin/env python3
"""Fail-closed validation for required JUnit test cases in a Surefire XML report."""

from __future__ import annotations

import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


class ReportValidationError(ValueError):
    """Raised when a Surefire report is missing, malformed, incomplete, or unsuccessful."""


def _count(root: ET.Element, attribute: str) -> int:
    raw = root.attrib.get(attribute)
    if raw is None:
        raise ReportValidationError(f"Surefire report is missing the {attribute!r} count")
    try:
        value = int(raw)
    except ValueError as exc:
        raise ReportValidationError(f"Surefire report has an invalid {attribute!r} count: {raw!r}") from exc
    if value < 0:
        raise ReportValidationError(f"Surefire report has a negative {attribute!r} count: {value}")
    return value


def validate_report(
    report: Path,
    required_tests: set[str],
    expected_suite: str,
    expected_class: str,
) -> dict[str, int]:
    if not required_tests:
        raise ReportValidationError("At least one required test identity must be configured")
    if not expected_suite:
        raise ReportValidationError("An expected Surefire suite identity must be configured")
    if not expected_class:
        raise ReportValidationError("An expected testcase class identity must be configured")
    if not report.is_file():
        raise ReportValidationError(f"Missing Surefire report: {report}")

    try:
        root = ET.parse(report).getroot()
    except (ET.ParseError, OSError) as exc:
        raise ReportValidationError(f"Malformed Surefire report {report}: {exc}") from exc

    if root.tag.rsplit("}", 1)[-1] != "testsuite":
        raise ReportValidationError(f"Unexpected Surefire root element: {root.tag!r}")
    actual_suite = root.attrib.get("name")
    if actual_suite != expected_suite:
        raise ReportValidationError(
            f"Unexpected Surefire suite identity: expected {expected_suite!r}, got {actual_suite!r}"
        )

    counts = {name: _count(root, name) for name in ("tests", "failures", "errors", "skipped")}
    test_cases = [element for element in root if element.tag.rsplit("}", 1)[-1] == "testcase"]
    if not test_cases or counts["tests"] == 0:
        raise ReportValidationError("Surefire report contains no executed test cases")
    if counts["tests"] != len(test_cases):
        raise ReportValidationError(
            f"Surefire test count does not match testcase evidence: {counts['tests']} != {len(test_cases)}"
        )

    identities: list[tuple[str, str]] = []
    for case in test_cases:
        actual_class = case.attrib.get("classname")
        if actual_class != expected_class:
            raise ReportValidationError(
                f"Unexpected testcase class identity: expected {expected_class!r}, got {actual_class!r}"
            )
        name = case.attrib.get("name")
        if not name:
            raise ReportValidationError("Surefire testcase is missing its name identity")
        identities.append((actual_class, name))

    unique_identities = set(identities)
    if len(unique_identities) != len(identities):
        duplicates = sorted(identity for identity in unique_identities if identities.count(identity) > 1)
        raise ReportValidationError(f"Duplicate testcase identities are not allowed: {duplicates}")

    names = {name for _, name in identities}
    missing = sorted(required_tests - names)
    if missing:
        raise ReportValidationError(f"Required test cases are missing: {', '.join(missing)}")

    child_failures = sum(
        1
        for case in test_cases
        for child in case
        if child.tag.rsplit("}", 1)[-1] in {"failure", "error", "skipped"}
    )
    if any(counts[name] for name in ("failures", "errors", "skipped")) or child_failures:
        raise ReportValidationError(f"Required tests did not complete cleanly: {counts}")

    return counts


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--required-test", action="append", required=True)
    parser.add_argument("--expected-suite", required=True)
    parser.add_argument("--expected-class", required=True)
    args = parser.parse_args()
    try:
        counts = validate_report(
            args.report,
            set(args.required_test),
            args.expected_suite,
            args.expected_class,
        )
    except ReportValidationError as exc:
        parser.exit(1, f"{exc}\n")
    print(
        "PostgreSQL Flyway CI gate verified: "
        f"{counts['tests']} tests, 0 failures, 0 errors, 0 skipped"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
