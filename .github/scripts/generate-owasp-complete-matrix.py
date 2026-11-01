#!/usr/bin/env python3
"""Render the CVSS >= 8 portion of a Dependency-Check JSON report for review."""

import json
import sys
from datetime import datetime, timezone
from pathlib import Path


APPROVED = {
    ("pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25", "CVE-2026-53914"),
    ("pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25", "CVE-2026-53914"),
    ("pkg:maven/org.springframework.security/spring-security-core@6.5.11", "CVE-2026-59270"),
    ("pkg:maven/org.springframework.security/spring-security-web@6.5.11", "CVE-2026-59270"),
}
for artifact in ("spring-core", "spring-tx"):
    for cve in (
        "CVE-2026-47884",
        "CVE-2026-47890",
        "CVE-2026-47891",
        "CVE-2026-47892",
        "CVE-2026-59313",
        "CVE-2026-59283",
    ):
        APPROVED.add((f"pkg:maven/org.springframework/{artifact}@6.2.19", cve))


def cvss_score(vulnerability):
    for key in ("cvssv4", "cvssv3", "cvssv2"):
        score = vulnerability.get(key, {}).get("baseScore")
        if isinstance(score, (int, float)):
            return float(score)
    return None


def escape(value):
    return str(value or "").replace("|", "\\|").replace("\n", " ")


def main(report_path, output_path):
    report = json.loads(Path(report_path).read_text(encoding="utf-8"))
    rows = []
    for dependency in report.get("dependencies", []):
        packages = dependency.get("packages") or []
        purls = [entry.get("id") for entry in packages if entry.get("id", "").startswith("pkg:maven/")]
        for vulnerability in dependency.get("vulnerabilities") or []:
            score = cvss_score(vulnerability)
            if score is None or score < 8:
                continue
            cve = vulnerability.get("name", "UNKNOWN")
            cpes = "; ".join(entry.get("value", "") for entry in dependency.get("vulnerableSoftware") or [])
            source = vulnerability.get("source", "UNKNOWN")
            for purl in purls or ["UNKNOWN"]:
                classification = (
                    "Previously approved exact combination"
                    if (purl, cve) in APPROVED
                    else "New combination requiring remediation or owner review"
                )
                rows.append((purl, cve, score, source, cpes, classification))

    rows.sort(key=lambda row: (row[0], row[1]))
    timestamp = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    lines = [
        "# Account Survival OWASP Complete Matrix",
        "",
        f"Generated from an unsuppressed Dependency-Check JSON report at `{timestamp}`.",
        "This is a review artifact, not an approval or a suppression change.",
        "",
        f"CVSS >= 8 associations: **{len(rows)}**.",
        "",
        "| Maven PURL | CVE | CVSS | Source | Scanner CPE evidence | Classification |",
        "| --- | --- | ---: | --- | --- | --- |",
    ]
    for purl, cve, score, source, cpes, classification in rows:
        lines.append(
            f"| `{escape(purl)}` | `{escape(cve)}` | {score:.1f} | {escape(source)} | {escape(cpes) or 'NONE'} | {classification} |"
        )
    if not rows:
        lines.append("| No CVSS >= 8 associations found | | | | | |")
    lines.extend(
        [
            "",
            "## Review Rules",
            "",
            "- A shared CPE is scanner evidence, not evidence that a vulnerability is non-applicable.",
            "- A new tuple remains unapproved until the owner records a tuple-level decision backed by this report.",
            "- The active release security workflow retains exact PURL/version/CVE suppressions and `failBuildOnCVSS=8`.",
        ]
    )
    Path(output_path).write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: generate-owasp-complete-matrix.py REPORT.json OUTPUT.md")
    main(sys.argv[1], sys.argv[2])
