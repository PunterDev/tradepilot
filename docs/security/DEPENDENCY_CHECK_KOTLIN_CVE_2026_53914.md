# Dependency-Check Kotlin CVE-2026-53914 Assessment

## Scope

This assessment covers the `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25`
finding reported by OWASP Dependency-Check in GitHub Actions run `41558006082`.

## Resolved dependency path

`com.resend:resend-java:4.13.0` -> `com.squareup.okhttp3:okhttp:4.12.0` ->
`org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25` -> `org.jetbrains.kotlin:kotlin-stdlib:1.9.25`.

TradePilot is a Java application and does not apply the Kotlin Gradle plugin.

## Advisory applicability

The reviewed advisory is CVE-2026-53914 / GHSA-r937-wjx7-w2jp. It names only
`org.jetbrains.kotlin:kotlin-gradle-plugin` and describes unsafe deserialization
of Kotlin Gradle build-cache metadata. The affected package is not present in the
resolved Maven dependency tree. `kotlin-stdlib` does not provide the Gradle build
cache functionality described by the advisory.

Dependency-Check associates this CVE with `kotlin-stdlib` through the generic
Kotlin CPE. This is a scanner attribution issue, not evidence that the runtime
artifact is affected. The Dependency-Check project documents the same limitation
for this CVE in issue #8749.

## Decision

No Kotlin runtime version override, suppression, threshold change, or analyzer
change is active. A future exception requires separate security approval with an
exact package/CVE scope. Until then, this finding remains blocking under the
existing CVSS >= 8 policy.

## Sources

- GitHub Advisory Database: GHSA-r937-wjx7-w2jp
- Dependency-Check issue #8749
