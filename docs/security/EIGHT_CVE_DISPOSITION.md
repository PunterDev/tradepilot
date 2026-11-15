# Dependency-Check Eight-CVE Disposition

## Decision

**NO-GO for PR #16 until either a compatible upgrade clears these findings or
the owner approves exact, time-bounded exceptions through the existing
fail-closed policy.** This document does not activate a suppression or change
the CVSS >= 8 threshold.

## Evidence and scope

This review is limited to PR #16 commit
`bfa203b7938b3a15ac0babb9ab78b87c2e004e3c`.

* Dependency-Check `12.2.2` JSON, HTML, and SARIF reports generated
  `2026-11-14T21:34:50Z`; the verified NVD data was last checked
  `2026-11-14T18:44:09Z`.
* Maven dependency tree for Java 17 and Spring Boot `3.5.16`.
* Full-repository search of source, checked-in configuration, and Maven
  dependencies for each advisory prerequisite.
* Official Kotlin and Spring advisories linked below.

The JSON report is authoritative: it contains **eight blocking instances,
eight unique CVEs, and three affected PURLs**. The HTML report also lists
lower-severity advisories and is not the CVSS >= 8 count.

## Provenance

| Exact PURL | Scope and introduction |
| --- | --- |
| `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25` | Runtime transitive: `resend-java:4.13.0` -> `okhttp:4.12.0` -> `kotlin-stdlib-jdk8:1.9.25` -> stdlib. Resend is used by `ResendEmailClient` for outbound email. |
| `pkg:maven/org.springframework/spring-core@6.2.19` | Runtime, managed by Boot `3.5.16` and introduced through web, JPA, security, validation, and actuator starters. |
| `pkg:maven/org.springframework.security/spring-security-core@6.5.11` | Runtime, managed by Boot `3.5.16` through `spring-boot-starter-security`; it is also present through test support. |

## Individual findings

| CVE / package / CVSS | Prerequisites and observed evidence | Remediation and disposition |
| --- | --- | --- |
| `CVE-2026-53914` / `kotlin-stdlib:1.9.25` / 9.8 | The [official GHSA](https://github.com/advisories/GHSA-r937-wjx7-w2jp) identifies `kotlin-gradle-plugin`, unsafe **Gradle build-cache** metadata deserialization, and does not identify `kotlin-stdlib`. TradePilot does not use the Kotlin Gradle plugin or a Kotlin build cache; the stdlib is a runtime transitive only. Dependency-Check inherits the issue through the broad `jetbrains:kotlin` CPE. | The fixed Kotlin line is `2.4.20+`, which is a major 1.9-to-2.4 change. **VERIFIED FALSE POSITIVE** for this exact PURL/CVE pair; an exact exception still requires owner approval. |
| `CVE-2026-47884` / `spring-core:6.2.19` / 9.8 | Requires `XsltView`, implicit `/**` MVC view rendering, and no explicit view name. No XSLT view, view resolver, `ModelAndView`, server-side view mapping, or XSLT configuration appears in the reviewed tree. | Spring `6.2.20` is the affected-line fix but Enterprise Support Only; `7.0.9` is OSS and requires a Boot 4 migration. **REQUIRES ADDITIONAL EVIDENCE:** owner must confirm no external deployed view-resolver/XSLT configuration. |
| `CVE-2026-47890` / `spring-core:6.2.19` / 9.8 | Requires MVC/WebFlux SSE with view fragments. No `SseEmitter`, `ServerSentEvent`, `text/event-stream`, WebFlux, or view fragments found. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE** in the reviewed application; revisit if SSE or view fragments are added. |
| `CVE-2026-47891` / `spring-core:6.2.19` / 9.8 | Requires WebFlux, Aalto XML, and `Jaxb2Decoder`. TradePilot uses servlet `spring-boot-starter-web`; no WebFlux dependency, Aalto, or XML decoder/configuration exists. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-47892` / `spring-core:6.2.19` / 9.8 | Requires WebFlux functional endpoints with `DispatcherServlet` and the affected header predicate. No WebFlux dependency or `RouterFunction` endpoint found. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-59313` / `spring-core:6.2.19` / 9.8 | Requires MVC functional `RouterFunction`, `ServerResponse.sse()`, SSE, plain-text events, and attacker-controlled streamed data. None exists in the reviewed application. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-59283` / `spring-core:6.2.19` / 9.1 | Requires `SimpleEvaluationContext` and SpEL compilation through `spring.expression.compiler.mode` or `SpelParserConfiguration`. No matching code or checked-in compiler mode found. Runtime system properties and externally injected configuration cannot be proved from source. | Same Spring upgrade limits. **REQUIRES ADDITIONAL EVIDENCE:** owner must verify production and shared environments do not enable compiled SpEL with `SimpleEvaluationContext`. |
| `CVE-2026-59270` / `spring-security-core:6.5.11` / 9.1 | Requires `UnboundIdContainer` or `spring.ldap.embedded.*` and a reachable LDAP listener. No UnboundID/LDAP dependency, container, configuration, or listener is resolved or checked in. | Spring Security `6.5.12` is affected-line Enterprise Support Only; OSS remediation is a major framework upgrade. **VERIFIED INAPPLICABLE** in the reviewed application; owner must confirm no deployment-time LDAP profile exists. |

The specific Spring prerequisites and fixed-version availability are documented
by [CVE-2026-59283](https://spring.io/security/cve-2026-59283/),
[CVE-2026-59313](https://spring.io/security/cve-2026-59313/), and
[CVE-2026-59270](https://spring.io/security/cve-2026-59270/). The remaining
Spring Core advisory links are: [47884](https://spring.io/security/cve-2026-47884/),
[47890](https://spring.io/security/cve-2026-47890/),
[47891](https://spring.io/security/cve-2026-47891/), and
[47892](https://spring.io/security/cve-2026-47892/).

## Runtime evidence still required

Repository review cannot prove values injected through a provider, JVM system
properties, untracked profiles, or external Spring configuration. Before an
exception is approved, the owner must record a value-free review for production
and each shared environment that serves this code:

1. No XSLT view resolver or implicit `/**` view rendering.
2. No `spring.expression.compiler.mode=IMMEDIATE` or `MIXED`, and no runtime
   `SimpleEvaluationContext` customization.
3. No `spring.ldap.embedded.*`, LDAP profile, or UnboundID listener.
4. No runtime module adds WebFlux, functional SSE, or the related components.

## Inactive December candidate

The first eight entries in
`docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml`
have SHA-256
`852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190`
and exactly match the eight report tuples above. Its eight additional entries
are not instances in this report. The candidate is **inactive**, is not an
approval record, and expires 2026-12-15.

After the runtime review, the following exact entries may be submitted for
separate owner approval: Kotlin `53914`; Spring Core `47890`, `47891`, `47892`,
and `59313`; and Spring Security `59270`. Their compensating controls are the
documented absence of the prerequisite application paths. `47884` and `59283`
remain blocked pending the external runtime review. Every approval must remain
exact-CVE/exact-PURL, SHA-bound to the candidate, expire no later than
2026-12-15, and reject unused or mismatched suppressions.

## Remediation plan

1. **Safe dependency fixes:** none identified. Do not add a Kotlin 2.4 override
   or Spring framework/security override outside Boot dependency management.
2. **Evidence-backed candidates:** only the exact entries above, after owner
   runtime verification and explicit approval.
3. **Deferred compatibility work:** a supported Boot/Spring/Kotlin upgrade in a
   dedicated release, tested with Java 17, email delivery, backend, PostgreSQL
   Flyway, and a fresh Dependency-Check scan.
4. **Scanner implementation:** PR #21 still needs hosted verification of
   artifact upload and GitHub check-run publication. Its local tests cover
   archive acquisition, report generation, and exact-head revalidation.

## Final release recommendation

Do not merge PR #16 while the eight findings block Dependency-Check. The next
approval is a value-free production/shared-environment review for the three
unproven runtime configurations, followed by explicit owner decision on the
exact inactive candidate. No broad suppression, CVSS reduction, or scanner
bypass is authorized.
