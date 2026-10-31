# Account Survival V1 Dependency-Check Risk Acceptance Proposal

**Status:** PROPOSED ONLY - not approved and not active
**Repository / branch:** `PunterDev/tradepilot` / `feature/v1-account-survival-coach`
**Dependency baseline reviewed:** `8d202d2f3aa23fe45a672723bc57f33b149ea424`
**Evidence document baseline:** `79956266daa845ae4b3a847e812ef69f769edd7c`
**Scope:** the eight findings listed below, and no other dependencies, CVEs, branches, or releases.
**Proposed expiry:** 2026-11-30T23:59:59Z
**Security gate:** remains `failBuildOnCVSS=8`; this document does not change the workflow or activate a suppression.

## Approval Required Before Any Exception Is Activated

Approval must be recorded on PR #1 by both of the following named people:

1. The current `PunterDev/tradepilot` repository owner.
2. The organization's designated security risk owner.

Each approval must name the approver, record the date, confirm the dependency baseline and document version, and accept the proposed expiry. The repository does not identify a security risk owner by name; that identity is an approval evidence gap and must be filled before activation. No applicable policy identifying whether one person may fill both roles was found in the repository. Until that policy is produced, the approvals must be from two different people. The feature author alone cannot approve the exception.

| Required role | Name | Decision | Date | Reviewed baseline |
| --- | --- | --- | --- | --- |
| Repository owner | _Unassigned - record in PR #1_ | UNAPPROVED | _TBD_ | `8d202d2f3aa23fe45a672723bc57f33b149ea424` |
| Security risk owner | _Unassigned - record in PR #1_ | UNAPPROVED | _TBD_ | `8d202d2f3aa23fe45a672723bc57f33b149ea424` |

## Scanner Evidence Gap

The latest GitHub Actions evidence is [run `41233630140`](https://github.com/PunterDev/tradepilot/actions/runs/41233630140); the earlier comparison point is [run `41232107045`](https://github.com/PunterDev/tradepilot/actions/runs/41232107045). Both the web and API endpoints return HTTP 404 in this review environment without an authorized GitHub token, so their artifacts were not independently downloaded here.

The exact scanner scores below are evidence supplied with the release brief for the latest run. They must still be reconciled against the attached Dependency-Check HTML/JSON before approval. Vendor severity is the classification published by the vendor advisory, and is intentionally not conflated with the scanner score.

The release brief reports the same eight findings as the earlier scan. Because neither artifact is accessible here, that comparison is documented as **reported, not independently verified**. The approval package must include both reports or a reviewer-produced diff showing no additional CVSS >= 8 finding.

## Reviewed Findings

| CVE | Reviewed package coordinate and version | Scanner severity | Vendor severity | Vendor evidence and exploit prerequisites | TradePilot evidence and disposition |
| --- | --- | --- | --- | --- | --- |
| `CVE-2026-53914` | `org.jetbrains.kotlin:kotlin-stdlib:1.9.25` (runtime transitive) | 9.8 (latest-run brief; artifact reconciliation pending) | Moderate | [GitHub advisory](https://github.com/advisories/GHSA-r937-wjx7-w2jp) affects **`org.jetbrains.kotlin:kotlin-gradle-plugin`** before `2.4.20-Beta1`: unsafe deserialization in Kotlin **build-cache metadata**. | **Artifact false positive.** `kotlin-stdlib` arrives through `resend-java:4.13.0 -> okhttp:4.12.0 -> okio -> kotlin-stdlib-jdk8/stdlib`; this is not the Kotlin Gradle plugin and the backend has no Gradle build/cache execution. Do not characterize this as a vulnerable runtime feature. |
| `CVE-2026-47884` | `org.springframework:spring-core:6.2.19` | 9.8 (latest-run brief; artifact reconciliation pending) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47884/) requires `XsltView`, an implicit `/**` MVC mapping that renders a view, and no explicitly specified view name. Boot 3.5's line is fixed by `6.2.20` (Enterprise-only); `7.0.9` is the public OSS fix. | **Configuration-inapplicable, not an artifact false positive.** All application controllers are REST controllers; source/configuration contain no `XsltView`, server-rendered view, or `/**` view mapping. |
| `CVE-2026-47890` | `org.springframework:spring-core:6.2.19` | 9.8 (latest-run brief; artifact reconciliation pending) | Low | [Spring advisory](https://spring.io/security/cve-2026-47890/) requires SSE carrying MVC/WebFlux view fragments and attacker-controlled content sent to other users. Same fixed-version availability as above. | **Configuration-inapplicable.** No SSE emitter, WebFlux, or view-fragment implementation exists. |
| `CVE-2026-47891` | `org.springframework:spring-core:6.2.19` | 9.8 (latest-run brief; artifact reconciliation pending) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47891/) requires WebFlux using the Aalto XML processor and `Jaxb2Decoder`. Same fixed-version availability as above. | **Configuration-inapplicable.** The backend uses `spring-boot-starter-web` (servlet MVC); no WebFlux, Aalto, XML decoder, or `maxInMemorySize` configuration exists. |
| `CVE-2026-47892` | `org.springframework:spring-core:6.2.19` | 9.8 (latest-run brief; artifact reconciliation pending) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47892/) requires WebFlux functional endpoints deployed through `DispatcherServlet` and a header predicate affected by pre-flight requests. Same fixed-version availability as above. | **Configuration-inapplicable.** No WebFlux or `RouterFunction` endpoint exists. |
| `CVE-2026-59313` | `org.springframework:spring-core:6.2.19` | 9.8 (latest-run brief; artifact reconciliation pending) | Low | [Spring advisory](https://spring.io/security/cve-2026-59313/) requires MVC functional `RouterFunction` plus `ServerResponse.sse()`, plain-text SSE, and attacker-controlled data streamed to other users. Same fixed-version availability as above. | **Configuration-inapplicable.** No `RouterFunction`, `ServerResponse.sse()`, SSE endpoint, or streamed user-controlled payload exists. |
| `CVE-2026-59283` | `org.springframework:spring-core:6.2.19` | 9.1 (latest-run brief; artifact reconciliation pending) | Medium | [Spring advisory](https://spring.io/security/cve-2026-59283/) requires `SimpleEvaluationContext` and SpEL compiler mode `IMMEDIATE` or `MIXED`, configured by `spring.expression.compiler.mode` or a compiler-enabled `SpelParserConfiguration`. Same fixed-version availability as above. | **Configuration-inapplicable.** There is no `SimpleEvaluationContext`, `SpelExpressionParser`, `SpelParserConfiguration`, compiler-mode property, or user-controlled SpEL evaluation. |
| `CVE-2026-59270` | `org.springframework.security:spring-security-core:6.5.11` | 9.1 (latest-run brief; artifact reconciliation pending) | Critical | [Spring advisory](https://spring.io/security/cve-2026-59270/) requires embedded `UnboundIdContainer` (directly or through `spring.ldap.embedded.*`) and a reachable LDAP listener. Spring Security `6.5.12` is Enterprise-only; `7.0.7` and `7.1.1` are public OSS fixes. | **Configuration-inapplicable.** There is no UnboundID/LDAP dependency, `UnboundIdContainer`, `spring.ldap.embedded.*` setting, or LDAP listener. The configured authentication path is stateless JWT plus `DaoAuthenticationProvider` and BCrypt. |

## Source and Effective-Configuration Evidence

The reviewed production configuration is the base configuration in [application.yml](../../backend/src/main/resources/application.yml), together with Spring's normal environment-property override behavior:

- The backend declares `spring-boot-starter-web`, not `spring-boot-starter-webflux`, in [pom.xml](../../backend/pom.xml). The source contains REST controllers and no `RouterFunction`, `ServerResponse.sse`, `SseEmitter`, `XsltView`, `SimpleEvaluationContext`, `SpelExpressionParser`, `SpelParserConfiguration`, `UnboundIdContainer`, or LDAP configuration.
- [SecurityConfig.java](../../backend/src/main/java/com/tradepilot/security/SecurityConfig.java) configures stateless JWT authentication, `DaoAuthenticationProvider`, and BCrypt. It does not configure LDAP authentication, embedded LDAP, MVC views, SSE, or SpEL.
- [SecurityProductionConfigValidator.java](../../backend/src/main/java/com/tradepilot/security/SecurityProductionConfigValidator.java) applies when `SPRING_PROFILES_ACTIVE` selects `prod` or `production`; it rejects enabled dev auth/tools and requires shared database rate limiting plus documented admin protection. There is no committed `application-prod.yml`.
- `application-dev.yml` is local/ignored and is outside this production review. It must not be used as production configuration evidence.
- Relevant possible overrides that must remain absent in production are `SPRING_LDAP_EMBEDDED_*`, `SPRING_EXPRESSION_COMPILER_MODE`, and any custom configuration introducing `XsltView`, functional WebFlux/MVC SSE handlers, Aalto/JAXB WebFlux decoders, or `UnboundIdContainer`. `SPRING_PROFILES_ACTIVE` must be `prod` or `production` for the production validator to run.

### Read-only Render Production Evidence

The authorized Render account identifies the live backend as service `tradepilot` (`srv-d801975b910c73deagqg`): Docker runtime, root directory `backend`, source branch `master`. Its latest live deployment is `dep-dbg0e9nb9u1s73foms60`, deployed from `master` commit `7a8c2442ff44779aeff06dcb682dcf96a8527b4f` on 2026-10-27. Startup logs record the active profile as `production` and show a successful application startup. This is **not** the feature-branch commit reviewed above, so it is evidence for the live service/profile only, not proof that the feature-branch dependency tree is deployed.

| Control | Verification | Result |
| --- | --- | --- |
| Production profile | Render startup log on 2026-10-27: `The following 1 profile is active: "production"`. The supplied direct service-variable inventory shows `SPRING_PROFILES_ACTIVE` blank; neither the committed Dockerfile nor committed application resources set it. The activation source is therefore not yet identified. | PASS for the active runtime profile; profile-source evidence remains required |
| Production security validator | The `production` service started successfully; the reviewed validator would reject enabled dev auth/tools, a non-database rate-limit store, or absent documented admin protection. The deployed source revision was not independently compared to this branch. | UNKNOWN |
| Embedded LDAP | The supplied direct service-variable inventory contains no `SPRING_LDAP_EMBEDDED_*` name and source/dependency review is negative. Environment groups, mounted files, and another configuration source remain unreadable. | UNKNOWN |
| SpEL compiler | The supplied direct service-variable inventory contains no `SPRING_EXPRESSION_COMPILER_MODE` name and source review is negative. Environment groups, mounted files, and another configuration source remain unreadable. | UNKNOWN |
| XSLT MVC | Source review is negative; the supplied direct service-variable inventory contains no custom XSLT/view-resolution control. Deployed source revision and any external configuration source were not fully inspected. | UNKNOWN |
| WebFlux/SSE/Aalto/JAXB | Source/dependency review is negative; the supplied direct service-variable inventory contains no related control. Deployed source revision and external configuration sources were not fully inspected. | UNKNOWN |
| Kotlin Gradle plugin | Render's deployed Docker build log shows Maven compilation/package activity, not Gradle. The reviewed backend is Maven-based and has no Kotlin Gradle plugin dependency. | PASS |
| External configuration | Service metadata and read-only logs were inspected. Environment-variable names, attached environment groups, mounted configuration, and the deployed container file system are not exposed by the authorized connector. | UNKNOWN |

This is a source-and-config review, not a substitute for access to all deployed configuration sources. A redacted direct service-variable inventory was supplied and reviewed; it must not be committed. Before approval, the release owner must attach the environment-group membership/name inventory and any mounted/config-file source inventory, and identify the source that activates the observed `production` profile despite the blank direct `SPRING_PROFILES_ACTIVE` entry. Never attach environment values or secrets.

## Scope and Residual Risk

The Kotlin entry is a scanner artifact/PURL mismatch: the vendor advisory names `kotlin-gradle-plugin`, while the reviewed dependency is runtime `kotlin-stdlib`. The Spring entries are different: `spring-core:6.2.19` and `spring-security-core:6.5.11` are genuinely within the vendor's affected version ranges, but the application does not expose each advisory's required optional feature/configuration.

Residual risk remains if a future code or configuration change introduces any prerequisite above before upgrading the framework. The exception must be revoked immediately if any of these occur:

1. Spring Boot, Spring Framework, Spring Security, Kotlin/Resend, or related BOM changes.
2. Addition of WebFlux, `RouterFunction`, SSE, MVC view rendering, XSLT, SpEL compilation, Aalto/JAXB decoders, LDAP, or embedded UnboundID.
3. A production override for the reviewed property names.
4. New Dependency-Check results for any package, version, or CVE outside this table.
5. Expiry on 2026-11-30T23:59:59Z.

The preferred remediation is a separately planned Spring Boot 4 migration, after confirming its BOM selects Spring Framework >= `7.0.9` and Spring Security >= `7.0.7`, followed by the full PostgreSQL/Flyway and Account Survival regression suite. A Boot 4 migration is not approved or implemented by this document. The alternative is licensed Spring Enterprise artifacts, subject to separate procurement and compatibility review.

## Proposed Narrow Dependency-Check Suppression

**Do not create this file or add it to Maven until the approvals above are recorded.** The following is a proposed `dependency-check-suppressions.xml` fragment. Before activation, replace only the `until` timestamp if a shorter approved expiry is selected and validate the PURLs against the uploaded report.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
  <!-- Proposed only. Each entry is exact-CVE, exact-PURL, and expires 2026-11-30. -->
  <suppress until="2026-11-30T23:59:59Z">
    <packageUrl regex="true">^pkg:maven/org\.jetbrains\.kotlin/kotlin-stdlib@1\.9\.25$</packageUrl>
    <cve>CVE-2026-53914</cve>
  </suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47884</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47890</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47891</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47892</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-59313</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-59283</cve></suppress>
  <suppress until="2026-11-30T23:59:59Z"><packageUrl regex="true">^pkg:maven/org\.springframework\.security/spring-security-core@6\.5\.11$</packageUrl><cve>CVE-2026-59270</cve></suppress>
</suppressions>
```

No wildcard package, CPE, CVE family, severity, or global suppression is proposed. This proposal must not suppress an additional Kotlin module such as `kotlin-stdlib-jdk8` unless the artifact proves that exact module is itself reported and it receives separate approval.

## Required Activation and Regression Gates

No active exception exists today, so the current workflow already proves that all CVSS >= 8 findings block: [security-scans.yml](../../.github/workflows/security-scans.yml) invokes Dependency-Check with `-DfailBuildOnCVSS=8` and no suppression path.

If this proposal is approved and later wired into Maven, the PR must add and pass these CI checks before it can merge:

1. A suppression-scope test that parses the suppression XML and allows only the eight `(CVE, exact PURL, exact version, expiry)` tuples above. It must fail on wildcard PURLs, a missing expiry, an expiry after the approved date, an additional CVE, or an additional dependency.
2. A Dependency-Check integration run proving an unrelated CVSS >= 8 finding is still reported and fails the job. The test fixture must not use one of the eight reviewed PURLs/CVEs.
3. A date-boundary test using a clock after `2026-11-30T23:59:59Z`, proving every proposed entry no longer suppresses its finding and the unchanged `failBuildOnCVSS=8` gate fails.
4. Full backend, PostgreSQL/Flyway, Account Survival, frontend, Playwright, Gitleaks, Semgrep, and `npm audit --audit-level=high` results attached to the approval PR.

Any attempt to reduce the CVSS threshold, disable Dependency-Check, or allow the job to continue on error invalidates this proposal.

## Approval Checklist

- [ ] Dependency-Check HTML/JSON reports from runs `41233630140` and `41232107045` attached; exact CVSS scores/PURLs reconciled and comparison recorded.
- [ ] Direct service-variable inventory reviewed; environment-group, mounted/config-file source inventories, and the active-profile source reconciled.
- [ ] Repository owner named and approves.
- [ ] Security risk owner named and approves.
- [ ] Expiry accepted and calendar review scheduled before 2026-11-30.
- [ ] Proposed suppression scope and activation tests reviewed.
- [ ] Release owner confirms no prerequisite feature/configuration has been introduced.
