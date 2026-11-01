# OWASP Dependency Bundling Assessment

**Status: diagnostic only.** No active release workflow setting has been changed by this assessment.

## Dependency-Check 13.0.0 Support

Dependency-Check core `13.0.0` includes `DependencyBundlingAnalyzer`. Inspection of the installed class confirms that its `getAnalyzerEnabledSettingKey()` returns:

```
analyzer.dependencybundling.enabled
```

This is a Dependency-Check core setting. It is distinct from the unrelated Ruby `bundleAuditAnalyzerEnabled` Maven parameter.

`DependencyBundlingAnalyzer.evaluateDependencies` merges matching dependencies into `relatedDependencies` when their hashes, shading relationship, WebJar relationship, or matching CPE/vulnerability/base-path signals warrant it. Vulnerability data therefore attaches to a representative dependency while the other JARs appear as related members. The representative can change as Maven input ordering or analyzer matching changes.

## Hosted Two-Mode Experiment — VERIFIED 2026-11-01

The prior diagnostic blocker has been resolved using the Dependency-Check Maven plugin user property `-Dodc.outputDirectory`. The authenticated [GitHub Actions diagnostic run 41336529191](https://github.com/PunterDev/tradepilot/actions/runs/41336529191) generated and uploaded both complete JSON reports and the comparison. [Private artifact 13095821500](https://github.com/PunterDev/tradepilot/actions/runs/41336529191/artifacts/13095821500); ZIP SHA-256 `90db67b59a9d8d3c7dede80f5e0245c35fd3b147f06876a1e868a2248ca926a9`.

| Actual hosted report measurement | Bundling enabled | Bundling disabled |
| --- | ---: | ---: |
| Top-level dependency records | 82 | 118 |
| Related dependency entries | 36 | 0 |
| Distinct Maven PURLs represented | 115 | 115 |
| CVSS 8+ CVE/PURL associations | 8 | 80 |
| Distinct CVSS 8+ CVEs | 8 | 8 |

The independent report comparison completed successfully: the supported core property demonstrably disabled bundling, no Maven PURL disappeared, and no distinct high-severity CVE identity disappeared. Both **unsuppressed** scanner invocations exited 1 as intended because they detected CVSS 8+ findings, so the diagnostic workflow's final status is **FAILURE BY DESIGN**, not a report-path failure or evidence of CVE loss.

The unbundled scan assigns the same eight high-severity CVEs to all related sibling modules individually, resulting in **80 actual scanner CVE/PURL associations**, of which **16 are presently owner-approved** and **64 are not**. This is independent scanner attribution evidence, **not a determination that all 80 JAR/CVE combinations are exploitable**.

**Decision: do not enable `-Danalyzer.dependencybundling.enabled=false` in the mandatory release workflow as an isolated fix.** Unbundling stabilizes attribution but leaves 64 unapproved CVE/PURL findings blocking. Enabling it without vulnerability remediation or a separate signed risk decision would not produce a green release scan, and it must not be paired with blanket suppressions.

The [mandatory security run 41336529139](https://github.com/PunterDev/tradepilot/actions/runs/41336529139) still failed on the eight known, unapproved associations attached to `kotlin-stdlib-jdk8`, `spring-aop`, and `spring-security-config`. Gitleaks, Semgrep, frontend audit, and the unrelated high-severity Log4j negative control passed. No release policy or suppressions have changed as a consequence of this experiment. **Release status: NO-GO.**

**Practical next path:** choose a supported patched dependency platform (separately reviewed migration) or document an application-reachability assessment of the eight CVEs and request **one explicit, time-bounded owner approval for precisely enumerated additional CVE/PURL/version pairs** from the unbundled matrix. No existing owner decision grants approval for those 64; do not add exceptions automatically or misrepresent product-level CPE associations as proof of safety.

---

## Required Experiment Before Activation

The manual [`owasp-complete-inventory`](../../.github/workflows/owasp-complete-inventory.yml) workflow runs two unsuppressed Dependency-Check `13.0.0` scans against the same graph:

1. Bundling enabled.
2. Bundling disabled with `-Danalyzer.dependencybundling.enabled=false`.

Both retain JSON reporting, `failBuildOnCVSS=8`, `failOnError=true`, and a valid temporary empty suppression XML. Both are expected to fail if CVSS >= 8 findings exist. The workflow uploads both reports and a fail-closed comparison artifact that rejects missing JSON or Maven artifacts.

Do not enable this property in `security-scans.yml` until an authorized reviewer compares the artifacts and confirms:

- every represented Maven PURL remains present;
- no vulnerability detection is lost;
- the original 16 exact suppressions still behave as intended;
- the Log4j negative control still fails; and
- any additional stable artifact-level tuple receives remediation or explicit owner approval.

Disabling bundling may produce more distinct artifact/CVE tuples. That is useful for stable attribution, but it is not authority to suppress those tuples.

## Direct Remediation Assessment

No direct dependency upgrade is activated by this assessment.

- The application uses the Spring Boot `3.5.16` BOM, which resolves Spring Framework `6.2.19` and Spring Security `6.5.11`. The vendor-published fixes discussed in the risk record are not available as a compatible public Boot 3 BOM update; individual Spring module overrides would break managed-version alignment.
- Maven Central lists `resend-java:4.29.0`, but it replaces OkHttp 4 with `okhttp-jvm:5.4.0` and Kotlin stdlib `2.1.21`. This neither proves that Dependency-Check stops associating the Kotlin product CVE nor qualifies as a minimal V1 update without API, Jackson, and runtime regression testing.
- Removing the Resend client is not acceptable because it is a production email integration. No unnecessary direct dependency was found that can be removed without changing product behavior.

The safe next step is the authenticated comparison experiment, followed by a single explicit owner decision for any newly stable exact tuples. A Boot 4/Spring 7 migration is outside this V1 branch.
