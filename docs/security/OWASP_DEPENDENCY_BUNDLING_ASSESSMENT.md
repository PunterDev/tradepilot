# OWASP Dependency Bundling Assessment

**Status: diagnostic only.** No active release workflow setting has been changed by this assessment.

## Dependency-Check 13.0.0 Support

Dependency-Check core `13.0.0` includes `DependencyBundlingAnalyzer`. Inspection of the installed class confirms that its `getAnalyzerEnabledSettingKey()` returns:

```
analyzer.dependencybundling.enabled
```

This is a Dependency-Check core setting. It is distinct from the unrelated Ruby `bundleAuditAnalyzerEnabled` Maven parameter.

`DependencyBundlingAnalyzer.evaluateDependencies` merges matching dependencies into `relatedDependencies` when their hashes, shading relationship, WebJar relationship, or matching CPE/vulnerability/base-path signals warrant it. Vulnerability data therefore attaches to a representative dependency while the other JARs appear as related members. The representative can change as Maven input ordering or analyzer matching changes.

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
