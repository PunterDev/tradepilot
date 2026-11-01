#!/usr/bin/env python3
"""Inventory actual Dependency-Check findings AND bundled Maven artifacts.

Bundled members are candidates, not independent vulnerability findings.
"""
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
    for cve in ("CVE-2026-47884", "CVE-2026-47890", "CVE-2026-47891",
                "CVE-2026-47892", "CVE-2026-59313", "CVE-2026-59283"):
        APPROVED.add((f"pkg:maven/org.springframework/{artifact}@6.2.19", cve))


def cvss_score(vulnerability):
    scores = []
    for key in ("cvssv4", "cvssv3", "cvssv2"):
        value = vulnerability.get(key) or {}
        if isinstance(value, dict) and isinstance(value.get("baseScore"), (float, int)):
            scores.append(float(value["baseScore"]))
    return max(scores) if scores else None


def escape(value):
    return str(value or "").replace("|", r"\|").replace("\n", " ")


def purls(entries):
    return sorted({
        item["id"] for item in (entries or [])
        if isinstance(item.get("id"), str) and item["id"].startswith("pkg:maven/")
    })


def main(report_path, output_path):
    data = json.loads(Path(report_path).read_text(encoding="utf-8"))
    dependencies = data.get("dependencies")
    if not isinstance(dependencies, list):
        raise ValueError("Invalid Dependency-Check JSON: dependencies[] missing")

    actual = {}
    bundled = {}
    for dep in dependencies:
        representative = dep.get("fileName", "UNKNOWN")
        rep_purls = purls(dep.get("packages"))
        related = [
            (member.get("fileName", "UNKNOWN"), purl)
            for member in dep.get("relatedDependencies") or []
            for purl in purls(member.get("packageIds") or member.get("packages"))
        ]
        cpes = "; ".join(
            item.get("id", "")
            for item in dep.get("vulnerabilityIds") or []
            if item.get("id", "").startswith("cpe:")
        )
        for finding in dep.get("vulnerabilities") or []:
            score = cvss_score(finding)
            if score is None or score < 8:
                continue
            cve = finding.get("name", "UNKNOWN")
            if not cve.startswith("CVE-"):
                continue
            for purl in rep_purls:
                actual[(purl, cve)] = (score, representative, cpes)
            for filename, purl in related:
                # A related/bundled artifact did NOT independently receive this
                # CVE in this scan. Do not call it a verified vulnerability.
                key = (purl, cve)
                if key not in actual:
                    bundled[key] = (score, representative, filename, cpes)

    for key in actual:
        bundled.pop(key, None)

    potential = {**bundled, **actual}
    approved = {key for key in potential if key in APPROVED}
    awaiting = {key for key in potential if key not in APPROVED}
    timestamp = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    represented = len(dependencies) + sum(len(dep.get("relatedDependencies") or []) for dep in dependencies)
    lines = [
        "# Account Survival OWASP Complete Matrix",
        "",
        f"Generated from a full unsuppressed Dependency-Check JSON scan at \`{timestamp}\`.",
        "",
        "**Important:** Dependency-Check's bundling analyzer groups related Maven artifacts.",
        "Actual scanner findings appear on one representative; sibling PURLs inside",
        "\`relatedDependencies\` are **potential future report representatives**, not",
        "independently verified vulnerable modules. Every candidate requires vendor",
        "applicability review before any additional owner approval.",
        "",
        f"- Top-level report dependencies: **{len(dependencies)}**",
        f"- Related/bundled dependency entries: **{represented - len(dependencies)}**",
        f"- Total represented entries: **{represented}**",
        f"- Actual CVSS >= 8 CVE/PURL associations: **{len(actual)}**",
        f"- Additional bundled candidate CVE/PURL associations: **{len(bundled)}**",
        f"- Combined potential CVE/PURL pairings: **{len(potential)}**",
        f"- Existing owner-approved potential pairings: **{len(approved)}**",
        f"- Potential pairings NOT owner-approved: **{len(awaiting)}**",
        "",
        "| Maven PURL | CVE | CVSS | Role in this scan | Source representative | Product CPE evidence | Approval |",
        "| --- | --- | ---: | --- | --- | --- | --- |",
    ]
    for key in sorted(potential):
        purl, cve = key
        is_actual = key in actual
        row = actual[key] if is_actual else bundled[key]
        score, source, *tail = row
        cpes = tail[-1]
        role = "ACTUAL SCANNER FINDING" if is_actual else "BUNDLED MEMBER CANDIDATE"
        approval = "EXACT TUPLE APPROVED" if key in APPROVED else "NOT APPROVED"
        lines.append(
            f"| \`{escape(purl)}\` | \`{escape(cve)}\` | {score:.1f} | "
            f"{role} | {escape(source)} | {escape(cpes) or 'NONE'} | {approval} |"
        )
    lines.extend([
        "",
        "## Boundaries and Next Decision",
        "",
        "The candidate expansion is deliberately conservative: CPE attribution to",
        "the representative does not establish CVE applicability to every bundled JAR.",
        "Do not suppress all candidate pairs merely because they appear in this matrix.",
        "First validate vendor prerequisites, resolve what can be fixed, and obtain",
        "an explicit, separately scoped owner decision for any remaining exceptions.",
        "",
        "Preserve \`failBuildOnCVSS=8\`, exact version/CVE matching, expiry, and",
        "the unrelated-CVSS-8 negative control. No approval is granted by this report.",
    ])
    Path(output_path).write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: generate-owasp-complete-matrix.py REPORT.json OUTPUT.md")
    main(sys.argv[1], sys.argv[2])
