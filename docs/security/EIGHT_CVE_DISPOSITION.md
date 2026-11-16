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
* Owner-supplied Render production inventory reviewed 2026-10-31 and again
  2026-11-04, with owner confirmation on 2026-11-14 that it is unchanged.
  The evidence concerns the `tradepilot` service in Render `My Workspace`
  (`tea-d800tatckfvc73f0vro0`) and is limited to configuration names and
  deployment metadata; no values or secrets were recorded.
* The trusted GitHub-hosted scanner independently validated its artifact,
  provenance, manifest, safe extraction, report upload, and exact-head check
  publication in [run 43681654206](https://github.com/PunterDev/tradepilot/actions/runs/43681654206).
  Its report artifact `14148448460` recorded the same eight CVSS >= 8 CVEs on
  this PR head. The intentional failure is evidence that the blocking gate is
  operating, not a successful release result.

The JSON report is authoritative: it contains **eight blocking instances,
eight unique CVEs, and three affected PURLs**. The HTML report also lists
lower-severity advisories and is not the CVSS >= 8 count.

The current Render deployment is commit
`c767b87e439f5bc95cfd8ff9f9bb58617b581163` and uses Spring Boot `3.3.5`.
It is production evidence for configuration names and the `production` startup
profile, but it is **not** the same dependency set as PR #16's proposed Spring
Boot `3.5.16` release. No conclusion about the PR #16 packages is inferred
from that difference.

## Owner-confirmed Production runtime attestation

On 2026-11-15, the owner performed an authorized runtime/configuration review
of the Production environment and confirmed all of the following as absent:

1. External XSLT or view-resolver configuration.
2. Compiled SpEL with `SimpleEvaluationContext`.
3. Unreviewed runtime modules or mounted configuration capable of introducing
   either behavior.

No secret values were requested, recorded, or reviewed in this disposition.
This is **owner-confirmed Production evidence**, not an independent runtime
reproduction. The connected Render inventory previously listed one `tradepilot`
service, but that observation does not prove that no other shared environment
can run the proposed backend. The owner must separately confirm that Production
is the only relevant release environment or provide the same value-free
attestation for every additional environment.

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
| `CVE-2026-47884` / `spring-core:6.2.19` / 9.8 | Requires `XsltView`, implicit `/**` MVC view rendering, and no explicit view name. No XSLT view, view resolver, `ModelAndView`, server-side view mapping, or XSLT configuration appears in the reviewed tree. The owner confirmed external XSLT/view-resolver configuration absent in Production on 2026-11-15. | Spring `6.2.20` is the affected-line fix but Enterprise Support Only; `7.0.9` is OSS and requires a Boot 4 migration. **CONDITIONALLY ELIGIBLE:** inapplicable in the reviewed Production configuration, pending shared-environment scope confirmation. Invalidated by XSLT/view customization, affected MVC routing, dependency change, or PR-head change. |
| `CVE-2026-47890` / `spring-core:6.2.19` / 9.8 | Requires MVC/WebFlux SSE with view fragments. No `SseEmitter`, `ServerSentEvent`, `text/event-stream`, WebFlux, or view fragments found. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE** in the reviewed application; revisit if SSE or view fragments are added. |
| `CVE-2026-47891` / `spring-core:6.2.19` / 9.8 | Requires WebFlux, Aalto XML, and `Jaxb2Decoder`. TradePilot uses servlet `spring-boot-starter-web`; no WebFlux dependency, Aalto, or XML decoder/configuration exists. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-47892` / `spring-core:6.2.19` / 9.8 | Requires WebFlux functional endpoints with `DispatcherServlet` and the affected header predicate. No WebFlux dependency or `RouterFunction` endpoint found. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-59313` / `spring-core:6.2.19` / 9.8 | Requires MVC functional `RouterFunction`, `ServerResponse.sse()`, SSE, plain-text events, and attacker-controlled streamed data. None exists in the reviewed application. | Same Spring upgrade limits. **VERIFIED INAPPLICABLE.** |
| `CVE-2026-59283` / `spring-core:6.2.19` / 9.1 | Requires `SimpleEvaluationContext` and SpEL compilation through `spring.expression.compiler.mode` or `SpelParserConfiguration`. No matching code or checked-in compiler mode found. The owner confirmed the affected runtime configuration absent in Production on 2026-11-15. | Same Spring upgrade limits. **CONDITIONALLY ELIGIBLE:** inapplicable in the reviewed Production configuration, pending shared-environment scope confirmation. Invalidated by a compiler property, programmatic parser, `SimpleEvaluationContext` use, external configuration, dependency change, or PR-head change. |
| `CVE-2026-59270` / `spring-security-core:6.5.11` / 9.1 | Requires `UnboundIdContainer` or `spring.ldap.embedded.*` and a reachable LDAP listener. No UnboundID/LDAP dependency, container, configuration, or listener is resolved or checked in. | Spring Security `6.5.12` is affected-line Enterprise Support Only; OSS remediation is a major framework upgrade. **VERIFIED INAPPLICABLE** in the reviewed application; owner must confirm no deployment-time LDAP profile exists. |

## Read-only runtime evidence matrix

The Render inventory did not identify `SPRING_EXPRESSION_COMPILER_MODE`, any
`SPRING_LDAP_EMBEDDED_*` name, XSLT/view-resolver configuration name, or
WebFlux/SSE-related configuration name. No Render Environment Group or Secret
File was attached. Startup logs previously identified the `production` profile;
the inventory's redacted or blank `SPRING_PROFILES_ACTIVE` value is not used as
profile evidence.

| CVE | Exploit prerequisite | Source/dependency evidence | Render inventory and owner confirmation | Remaining uncertainty | Final disposition | Eligible for separate owner approval |
| --- | --- | --- | --- | --- | --- | --- |
| `53914` | Kotlin Gradle build-cache metadata | Runtime stdlib only; no Kotlin Gradle plugin/cache | Not applicable to the runtime configuration; inventory unchanged 2026-11-14 | Future build tooling could change this | VERIFIED FALSE POSITIVE | Yes, exact PURL/CVE only |
| `47884` | XSLT view, implicit `/**` view rendering | No XSLT/view resolver/`ModelAndView` path | Owner-confirmed absent in Production on 2026-11-15 | Shared-environment coverage is not established | CONDITIONALLY ELIGIBLE | After shared scope confirmation and exact approval only |
| `47890` | SSE view fragments | No SSE, WebFlux, or view fragments | No WebFlux/SSE configuration name; inventory unchanged | Future feature/module changes | VERIFIED INAPPLICABLE | Yes, exact PURL/CVE only |
| `47891` | WebFlux, Aalto XML, `Jaxb2Decoder` | Servlet web starter; no WebFlux/Aalto/decoder | No contradictory configuration name; inventory unchanged | Runtime module/classpath additions outside review | VERIFIED INAPPLICABLE | Yes, exact PURL/CVE only |
| `47892` | WebFlux functional route with affected predicate | No WebFlux or `RouterFunction` | No contradictory configuration name; inventory unchanged | Runtime module/classpath additions outside review | VERIFIED INAPPLICABLE | Yes, exact PURL/CVE only |
| `59313` | MVC functional SSE and untrusted plain-text stream | No functional route, SSE, or streamed path | No WebFlux/SSE configuration name; inventory unchanged | Future feature/module changes | VERIFIED INAPPLICABLE | Yes, exact PURL/CVE only |
| `59283` | `SimpleEvaluationContext` plus compiled SpEL | No matching code or checked-in compiler mode | Owner-confirmed absent in Production on 2026-11-15 | Shared-environment coverage is not established | CONDITIONALLY ELIGIBLE | After shared scope confirmation and exact approval only |
| `59270` | UnboundID embedded LDAP listener | No UnboundID/LDAP dependency, container, or listener | No `SPRING_LDAP_EMBEDDED_*` name; no groups or secret files; inventory unchanged | Deployment-time profile/module outside reviewed inventory | VERIFIED INAPPLICABLE | Yes, exact PURL/CVE only |

The specific Spring prerequisites and fixed-version availability are documented
by [CVE-2026-59283](https://spring.io/security/cve-2026-59283/),
[CVE-2026-59313](https://spring.io/security/cve-2026-59313/), and
[CVE-2026-59270](https://spring.io/security/cve-2026-59270/). The remaining
Spring Core advisory links are: [47884](https://spring.io/security/cve-2026-47884/),
[47890](https://spring.io/security/cve-2026-47890/),
[47891](https://spring.io/security/cve-2026-47891/), and
[47892](https://spring.io/security/cve-2026-47892/).

## Remaining runtime evidence

The owner-confirmed review resolves the XSLT and compiled-SpEL questions for
Production only. Before an exception can cover the release, the owner must
make one narrow, value-free decision: Production is the only environment that
can run the proposed backend, or each additional shared environment has the
same confirmed-absent review. Repository review remains unable to prove values
injected through a provider, JVM system properties, untracked profiles, or
external Spring configuration.

## Inactive December candidate

The first eight entries in
`docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml`
have SHA-256
`852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190`
and exactly match the eight report tuples above. Its eight additional entries
are not instances in this report. The candidate is **inactive**, is not an
approval record, and expires 2026-12-15.

The owner-confirmed evidence does not approve activation. Kotlin `53914`; Spring
Core `47890`, `47891`, `47892`, and `59313`; and Spring Security `59270` are
technically eligible for separate owner review. Spring Core `47884` and `59283`
are conditionally eligible only after the shared-environment scope decision.
Every approval must remain exact-CVE/exact-PURL, bind the future reviewed
activation head and candidate hash, expire no later than 2026-12-15, and reject
unused or mismatched suppressions.

## Remediation plan

1. **Safe dependency fixes:** none identified. Do not add a Kotlin 2.4 override
   or Spring framework/security override outside Boot dependency management.
2. **Evidence-backed candidates:** only the exact entries above, after owner
   runtime verification and explicit approval.
3. **Deferred compatibility work:** a supported Boot/Spring/Kotlin upgrade in a
   dedicated release, tested with Java 17, email delivery, backend, PostgreSQL
   Flyway, and a fresh Dependency-Check scan.
4. **Scanner implementation:** hosted run `43681654206` verified trusted
   archive acquisition, provenance, report upload, and a failing
   `trusted-dependency-check` check bound to this exact PR head. It does not
   approve the eight findings or make PR #16 releasable.

## Final release recommendation

Do not merge PR #16 while the eight findings block Dependency-Check. The next
owner decision is whether Production is the only relevant release environment
or which additional environments require the same value-free review, followed
by explicit approval of each exact tuple. No broad suppression, CVSS reduction,
or scanner bypass is authorized.
