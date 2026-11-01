#!/usr/bin/env python3
"""Fail closed when Dependency-Check bundling comparison reports are incomplete."""

import json
import sys
from pathlib import Path


def load(path):
    try:
        value = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise SystemExit(f"invalid Dependency-Check report {path}: {error}")
    if not isinstance(value, dict) or not isinstance(value.get("dependencies"), list) or not value["dependencies"]:
        raise SystemExit(f"incomplete Dependency-Check report {path}: nonempty dependencies list is required")
    return value


def score(vulnerability):
    values = [
        value.get("baseScore")
        for key in ("cvssv4", "cvssv3", "cvssv2")
        if isinstance((value := vulnerability.get(key) or {}), dict)
    ]
    scores = [float(value) for value in values if isinstance(value, (float, int))]
    return max(scores) if scores else None


def dependency_purls(dependency):
    # Bundled relatedDependencies store Maven PURLs in packageIds,
    # whereas top-level Dependency-Check records use packages.
    packages = (dependency.get("packages") or []) + (dependency.get("packageIds") or [])
    return {
        item["id"]
        for item in packages
        if isinstance(item, dict) and isinstance(item.get("id"), str)
        and item["id"].startswith("pkg:maven/")
    }


def walk_dependencies(dependencies):
    for dependency in dependencies:
        yield dependency
        yield from walk_dependencies(dependency.get("relatedDependencies") or [])


def inventory(report):
    dependencies = list(walk_dependencies(report["dependencies"]))
    purls = set().union(*(dependency_purls(dependency) for dependency in dependencies)) if dependencies else set()
    high = set()
    for dependency in dependencies:
        for vulnerability in dependency.get("vulnerabilities") or []:
            if (base_score := score(vulnerability)) is not None and base_score >= 8:
                for purl in dependency_purls(dependency):
                    high.add((purl, vulnerability.get("name", "UNKNOWN"), base_score))
    related = sum(len(dependency.get("relatedDependencies") or []) for dependency in dependencies)
    return purls, high, len(report["dependencies"]), related


def main(bundled_path, unbundled_path, output_path):
    bundled = inventory(load(bundled_path))
    unbundled = inventory(load(unbundled_path))
    missing = bundled[0] - unbundled[0]
    if missing:
        raise SystemExit("unbundled scan lost Maven artifacts: " + ", ".join(sorted(missing)))
    # CVEs are attached to whichever dependency is the bundle representative.
    # Compare CVE identity, not PURL/CVE tuples, when checking detection loss.
    bundled_cves = {cve for _, cve, _ in bundled[1]}
    unbundled_cves = {cve for _, cve, _ in unbundled[1]}
    lost_cves = bundled_cves - unbundled_cves
    if lost_cves:
        raise SystemExit("unbundled scan lost severe CVE detection: " + ", ".join(sorted(lost_cves)))
    if bundled[3] and unbundled[3] >= bundled[3]:
        raise SystemExit("bundling-disabled setting not demonstrated: related dependency count did not decrease")

    lines = [
        "# Dependency-Check Bundling Comparison",
        "",
        "This diagnostic compares two unsuppressed reports produced by Dependency-Check 13.0.0.",
        "It does not approve a suppression or replace the release CVSS gate.",
        "",
        "| Measure | Bundling enabled | Bundling disabled |",
        "| --- | ---: | ---: |",
        f"| Top-level dependency records | {bundled[2]} | {unbundled[2]} |",
        f"| Related dependency records | {bundled[3]} | {unbundled[3]} |",
        f"| Represented Maven PURLs | {len(bundled[0])} | {len(unbundled[0])} |",
        f"| CVSS >= 8 PURL/CVE/score tuples | {len(bundled[1])} | {len(unbundled[1])} |",
        f"| Distinct high-severity CVEs | {len(bundled_cves)} | {len(unbundled_cves)} |",
        "",
        "## Review Requirements",
        "",
        "- All bundled Maven PURLs (including relatedDependencies.packageIds) were represented with bundling disabled.",
        "- All CVSS >= 8 CVE identities observed with bundling enabled were retained when disabled.",
        "- Bundling-disabled mode demonstrably reduced the number of related dependency records.",
        "- Compare the tuple sets before changing the required workflow: added tuples need remediation or explicit approval; removed tuples need vendor and scanner evidence review.",
        "- A successful comparison does not make either scan pass; the scanner's own `failBuildOnCVSS=8` status remains authoritative.",
    ]
    Path(output_path).write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit("usage: compare-owasp-bundling-reports.py BUNDLED.json UNBUNDLED.json OUTPUT.md")
    main(*sys.argv[1:])
