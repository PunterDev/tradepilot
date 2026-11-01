# Account Survival V1 Dependency-Check Risk Acceptance Proposal

## Additional 64-Tuple Owner Approval and Activation — 2026-11-01

The sole repository owner explicitly approved temporary residual risk for the **64 additional exact CVE/PURL/version combinations** in [the full proposal](ACCOUNT_SURVIVAL_OWASP_64_TUPLE_OWNER_PROPOSAL.md) at `9a6ddabca62e93d618e30f4898d8ea5488901d7a`. The owner decision is recorded in [PR #1 comment 6562291838](https://github.com/PunterDev/tradepilot/pull/1#issuecomment-6562291838), in addition to the original 16 independently documented exact owner-approved combinations. This is a **self-review** risk acceptance, not independent security assurance.

The bounded 64-rule implementation was committed at `9f1b483961af3728aba5020900f75d88ccc5eb47`; mandatory unbundled scanning (preserving CVSS 8 and strict unused-suppression rules) at `552f6855467d00d92bb7b9f2ad0152adbe5f35de`; strict 80-tuple Java scope testing at `8e5d87177b44d43a3f05ba169d241aaf73fa5025`. Every exception remains individually anchored to one Maven package, version and CVE and uses the XSD-compatible `until="2026-11-30Z"` (expires `2026-11-30T00:00:00Z`). No wildcard CPE/family suppression is authorized.

[Hosted bundled/unbundled evidence](https://github.com/PunterDev/tradepilot/actions/runs/41336529191) established 115 preserved Maven PURLs and eight preserved high-severity CVE identities; the unbundled report identified the precise 80 associations. Conditional absence of vendor exploit prerequisites and all baseline/configuration drift revocation triggers in the proposal remain material to this risk decision. Any new high-severity CVE, runtime dependency change, activation of a reviewed vulnerable feature, or change in relevant production configuration requires fresh assessment and, where appropriate, immediate revocation.

**Activation on this feature branch does not authorize merge or production deployment.** Security CI, unrelated critical-CVE negative control, regression tests and required release approval must pass before reconsidering NO-GO status.

---


**Status:** OWNER-APPROVED FOR EXACT-SCOPE IMPLEMENTATION - not merged, deployed, or active in production
**Repository / branch:** `PunterDev/tradepilot` / `feature/v1-account-survival-coach`
**Dependency baseline reviewed:** `8d202d2f3aa23fe45a672723bc57f33b149ea424`
**Evidence document baseline:** `79956266daa845ae4b3a847e812ef69f769edd7c`
**Scope:** the original eight exact combinations plus eight additional exact artifact combinations approved by the sole owner; no other dependencies, CVEs, branches, or releases.
**Owner-approved latest expiry:** 2026-11-30T23:59:59Z
**Applied scanner expiry (XSD date):** `2026-11-30Z` (becomes inactive at 2026-11-30T00:00:00Z UTC, earlier than approved).
**Reason for earlier cutoff:** OWASP Dependency-Check 13.0.0 requires an `xs:date` for the suppression `until` attribute. A timestamp was rejected in GitHub Actions run `41259471064`; do not move the expiry later than the owner's approval without reauthorization.
**Security gate:** remains `failBuildOnCVSS=8`; the implementation below may suppress only the approved exact scope and does not disable the workflow.

## Owner Decision and Evidence Retention

The sole repository owner recorded a solo-owner governance adoption and temporary acceptance decision on [PR #1 comment 6551147842](https://github.com/PunterDev/tradepilot/pull/1#issuecomment-6551147842). Per the owner attestation, the decision accepts only this document's eight exact CVE/PURL/version tuples through `2026-11-30T23:59:59Z`, identifies no applicable independent-approval requirement, and acknowledges that the review is self-review rather than independent assurance.

The original reports remain in private GitHub Actions artifacts for authorized reviewers:

| GitHub Actions run | Artifact ID | Retention expiry | Verification source |
| --- | --- | --- | --- |
| `41232107045` | `13053903962` | 2027-01-29 | Owner attestation; original artifact ZIP supplied to this review |
| `41233630140` | `13054407734` | 2027-01-29 | Owner attestation; original artifact ZIP supplied to this review |

The artifact retention expiry is after the approved exception expiry. This environment could not use authenticated GitHub API access to retrieve the private PR comment or artifacts directly; the decision and reviewer access are therefore recorded as **owner-verified**, while the supplied original artifact ZIP and both HTML report hashes were independently revalidated locally. No artifact report is committed to the repository.

The exact-scope suppression implementation is present only on this feature branch and remains subject to the scope tests, Dependency-Check run, and full CI below. It does not authorize merging or deployment.

## Governance Assessment and Proposed Solo-Owner Amendment

### What the repository currently requires

The drafted [Risk Management Policy](../compliance/policies/11_RISK_MANAGEMENT_POLICY.md) and [Risk Assessment Procedure](../compliance/procedures/RISK_ASSESSMENT_PROCEDURE.md) require **documented management approval** and residual-risk documentation for accepted risks. They assign risk acceptance to TradePilot Management. Neither document requires a second approver, independent security reviewer, or separation between repository owner and risk owner. Both documents remain pending management approval, so they are project governance drafts rather than evidence of an already-operated external certification control.

No repository documentation identifies a contractual, regulatory, customer, or certification obligation that independently requires two people to approve this exception. This is a repository-only conclusion: legal, customer-contract, insurer, or future certification obligations have not been independently reviewed here and remain **UNKNOWN**.

### Existing two-person proposal

Earlier revisions of this document proposed separate approval by the repository owner and a designated security risk owner. That remains the preferred option when an independent qualified reviewer is available, but it is a self-imposed proposal in this document, not a binding requirement found in the repository. It must not be silently treated as satisfied by one person.

### Proposed solo-owner approval path

Because TradePilot is currently solo-owned and solo-operated, the repository owner may be the accountable management risk decision-maker **only if the owner explicitly adopts this amendment on PR #1** and confirms that no outside obligation requires independent approval. This is self-review, not independent assurance.

The solo-owner path is limited to the exact eight CVE/PURL/version tuples in this document and requires all of the following before any later suppression activation change:

1. The owner records their name, UTC decision time, the reviewed dependency baseline, the document revision, scope, and expiry on PR #1.
2. The owner attaches or links the retained scan evidence and Render configuration attestation without including environment values or credentials.
3. The owner explicitly acknowledges the absence of independent security approval and accepts the residual risk only through `2026-11-30T23:59:59Z`.
4. The owner confirms that no listed revocation trigger is present and that no binding external approval requirement applies.
5. A separate, reviewed implementation change later activates only the exact suppression scope and passes every activation gate below.

This proposed amendment records no approval, does not activate an exception, and does not remove the option to obtain independent review.

| Proposed role | Name | Decision | Date | Reviewed baseline |
| --- | --- | --- | --- | --- |
| Repository owner / accountable management risk decision-maker | _Unassigned - record in PR #1_ | UNAPPROVED | _TBD_ | `8d202d2f3aa23fe45a672723bc57f33b149ea424` |
| Independent reviewer (recommended, not assigned) | _Unassigned_ | NOT REQUIRED BY REPOSITORY / NOT APPROVED | _TBD_ | `8d202d2f3aa23fe45a672723bc57f33b149ea424` |

## Scanner Evidence and Reconciliation

The latest GitHub Actions evidence is [run `41233630140`](https://github.com/PunterDev/tradepilot/actions/runs/41233630140); the earlier comparison point is [run `41232107045`](https://github.com/PunterDev/tradepilot/actions/runs/41232107045). The repository owner supplied the original `TradePilot_V1_OWASP_Approval_Evidence.zip` package directly to this review. It was not committed to the repository.

Codex verified the archive integrity, extracted it only to a temporary directory outside the repository, and parsed both original `dependency-check-report.html` files without executing archive content. The nested artifact ZIPs contained only one non-executable, regular HTML file each; no path traversal or symbolic links were present. The reports were generated by Dependency-Check 13.0.0 and had the following verified identity:

| GitHub Actions run | Artifact ID | Report generated (UTC) | Verified HTML SHA-256 |
| --- | --- | --- | --- |
| `41232107045` | `13053903962` | 2026-10-31T19:06:05Z | `ad7d92bf1197c2077bdacc3813d692cfb7a908e687e08cd1065193ec87c181d2` |
| `41233630140` | `13054407734` | 2026-10-31T19:23:14Z | `3b262631cff1371aebe3c8681a92016b5e743997b2a09ab5ac61844981fee78b` |

Those hashes exactly match the hashes claimed by the included `TRADEPILOT_V1_OWASP_ARTIFACT_RECONCILIATION.md`. A programmatic comparison of `(CVE, PURL, CVSS)` for every finding with CVSS >= 8 found eight tuples in each report, with zero additions, removals, or changes. The reports contain lower-scored findings outside this proposal's scope.

This review did not access GitHub's private artifact API or download the reports from GitHub. Vendor severity is the classification published by the vendor advisory, and is intentionally not conflated with the scanner score. The owner attests that the private GitHub artifacts above are retained for authorized reviewers; do not commit raw report files to the repository.

## Reviewed Findings

| CVE | Exact reviewed package URL | Scanner severity | Vendor severity | Vendor evidence and exploit prerequisites | TradePilot evidence and disposition |
| --- | --- | --- | --- | --- | --- |
| `CVE-2026-53914` | `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25` (runtime transitive) | 9.8 (owner-reconciled reports) | Moderate | [GitHub advisory](https://github.com/advisories/GHSA-r937-wjx7-w2jp) affects **`org.jetbrains.kotlin:kotlin-gradle-plugin`** before `2.4.20-Beta1`: unsafe deserialization in Kotlin **build-cache metadata**. | **Artifact false positive.** `kotlin-stdlib` arrives through `resend-java:4.13.0 -> okhttp:4.12.0 -> okio -> kotlin-stdlib-jdk8/stdlib`; this is not the Kotlin Gradle plugin and the backend has no Gradle build/cache execution. Do not characterize this as a vulnerable runtime feature. |
| `CVE-2026-47884` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.8 (owner-reconciled reports) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47884/) requires `XsltView`, an implicit `/**` MVC mapping that renders a view, and no explicitly specified view name. Boot 3.5's line is fixed by `6.2.20` (Enterprise-only); `7.0.9` is the public OSS fix. | **Configuration-inapplicable, not an artifact false positive.** All application controllers are REST controllers; source/configuration contain no `XsltView`, server-rendered view, or `/**` view mapping. |
| `CVE-2026-47890` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.8 (owner-reconciled reports) | Low | [Spring advisory](https://spring.io/security/cve-2026-47890/) requires SSE carrying MVC/WebFlux view fragments and attacker-controlled content sent to other users. Same fixed-version availability as above. | **Configuration-inapplicable.** No SSE emitter, WebFlux, or view-fragment implementation exists. |
| `CVE-2026-47891` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.8 (owner-reconciled reports) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47891/) requires WebFlux using the Aalto XML processor and `Jaxb2Decoder`. Same fixed-version availability as above. | **Configuration-inapplicable.** The backend uses `spring-boot-starter-web` (servlet MVC); no WebFlux, Aalto, XML decoder, or `maxInMemorySize` configuration exists. |
| `CVE-2026-47892` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.8 (owner-reconciled reports) | Medium | [Spring advisory](https://spring.io/security/cve-2026-47892/) requires WebFlux functional endpoints deployed through `DispatcherServlet` and a header predicate affected by pre-flight requests. Same fixed-version availability as above. | **Configuration-inapplicable.** No WebFlux or `RouterFunction` endpoint exists. |
| `CVE-2026-59313` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.8 (owner-reconciled reports) | Low | [Spring advisory](https://spring.io/security/cve-2026-59313/) requires MVC functional `RouterFunction` plus `ServerResponse.sse()`, plain-text SSE, and attacker-controlled data streamed to other users. Same fixed-version availability as above. | **Configuration-inapplicable.** No `RouterFunction`, `ServerResponse.sse()`, SSE endpoint, or streamed user-controlled payload exists. |
| `CVE-2026-59283` | `pkg:maven/org.springframework/spring-core@6.2.19` | 9.1 (owner-reconciled reports) | Medium | [Spring advisory](https://spring.io/security/cve-2026-59283/) requires `SimpleEvaluationContext` and SpEL compiler mode `IMMEDIATE` or `MIXED`, configured by `spring.expression.compiler.mode` or a compiler-enabled `SpelParserConfiguration`. Same fixed-version availability as above. | **Configuration-inapplicable.** There is no `SimpleEvaluationContext`, `SpelExpressionParser`, `SpelParserConfiguration`, compiler-mode property, or user-controlled SpEL evaluation. |
| `CVE-2026-59270` | `pkg:maven/org.springframework.security/spring-security-core@6.5.11` | 9.1 (owner-reconciled reports) | Critical | [Spring advisory](https://spring.io/security/cve-2026-59270/) requires embedded `UnboundIdContainer` (directly or through `spring.ldap.embedded.*`) and a reachable LDAP listener. Spring Security `6.5.12` is Enterprise-only; `7.0.7` and `7.1.1` are public OSS fixes. | **Configuration-inapplicable.** There is no UnboundID/LDAP dependency, `UnboundIdContainer`, `spring.ldap.embedded.*` setting, or LDAP listener. The configured authentication path is stateless JWT plus `DaoAuthenticationProvider` and BCrypt. |

## Solo-Owner Decision Worksheet

This worksheet is a review aid only. Leave every owner-decision cell unsigned until the owner explicitly decides on PR #1. "Accept temporarily" means accepting residual risk for the exact reviewed baseline only; it does not activate a Dependency-Check suppression.

| CVE | Reviewed artifact | Applicability evidence | Residual risk | Recommended decision | Owner decision |
| --- | --- | --- | --- | --- | --- |
| `CVE-2026-53914` | `kotlin-stdlib:1.9.25` | The advisory targets `kotlin-gradle-plugin` build-cache metadata; the report PURL is runtime `kotlin-stdlib`, and this Maven backend has no Gradle build/cache use. | Scanner matching could change if a Kotlin build plugin or another affected module is introduced. | ACCEPT TEMPORARILY as an artifact mismatch; re-review on Kotlin/Resend dependency changes. | _UNSIGNED_ |
| `CVE-2026-47884` | `spring-core:6.2.19` | Requires XSLT MVC view rendering and an implicit view mapping. Reviewed source has REST controllers only and no XSLT/view configuration. | A later MVC view/XSLT addition could make the affected artifact exploitable. | ACCEPT TEMPORARILY only if the owner confirms no external configuration adds these prerequisites. | _UNSIGNED_ |
| `CVE-2026-47890` | `spring-core:6.2.19` | Requires SSE with attacker-controlled MVC/WebFlux view fragments sent to other users. No SSE, WebFlux, or view fragments were found. | A later SSE/view-fragment feature could introduce the prerequisite. | ACCEPT TEMPORARILY with immediate revocation on SSE/WebFlux/view changes. | _UNSIGNED_ |
| `CVE-2026-47891` | `spring-core:6.2.19` | Requires WebFlux, Aalto XML, and `Jaxb2Decoder`. The application is servlet MVC and source/dependencies are negative for those components. | A WebFlux/XML decoder dependency or configuration change could introduce the prerequisite. | ACCEPT TEMPORARILY with immediate revocation on WebFlux/XML changes. | _UNSIGNED_ |
| `CVE-2026-47892` | `spring-core:6.2.19` | Requires a WebFlux functional endpoint deployed through `DispatcherServlet`. No WebFlux or `RouterFunction` endpoint was found. | A functional WebFlux routing change could introduce the prerequisite. | ACCEPT TEMPORARILY with immediate revocation on routing-stack changes. | _UNSIGNED_ |
| `CVE-2026-59313` | `spring-core:6.2.19` | Requires MVC `RouterFunction`, `ServerResponse.sse()`, and plain-text SSE with user-controlled content. None were found. | A future functional MVC/SSE feature could introduce the prerequisite. | ACCEPT TEMPORARILY with immediate revocation on functional routing/SSE changes. | _UNSIGNED_ |
| `CVE-2026-59283` | `spring-core:6.2.19` | Requires `SimpleEvaluationContext` and SpEL compiler mode `IMMEDIATE` or `MIXED`. No matching source/property was found; owner attestation reports no direct compiler-mode override. | A host-level override or new compiler-enabled SpEL code could introduce the prerequisite. | NEEDS REVIEW if any external configuration source cannot be accounted for; otherwise ACCEPT TEMPORARILY. | _UNSIGNED_ |
| `CVE-2026-59270` | `spring-security-core:6.5.11` | Requires reachable embedded UnboundID LDAP. No LDAP/UnboundID dependency or configuration was found; owner attestation reports no embedded-LDAP override. | A dependency or host configuration introducing embedded LDAP could introduce the prerequisite. | NEEDS REVIEW if external configuration cannot be accounted for; otherwise ACCEPT TEMPORARILY. | _UNSIGNED_ |

## Source and Effective-Configuration Evidence

The reviewed production configuration is the base configuration in [application.yml](../../backend/src/main/resources/application.yml), together with Spring's normal environment-property override behavior:

- The backend declares `spring-boot-starter-web`, not `spring-boot-starter-webflux`, in [pom.xml](../../backend/pom.xml). The source contains REST controllers and no `RouterFunction`, `ServerResponse.sse`, `SseEmitter`, `XsltView`, `SimpleEvaluationContext`, `SpelExpressionParser`, `SpelParserConfiguration`, `UnboundIdContainer`, or LDAP configuration.
- [SecurityConfig.java](../../backend/src/main/java/com/tradepilot/security/SecurityConfig.java) configures stateless JWT authentication, `DaoAuthenticationProvider`, and BCrypt. It does not configure LDAP authentication, embedded LDAP, MVC views, SSE, or SpEL.
- [SecurityProductionConfigValidator.java](../../backend/src/main/java/com/tradepilot/security/SecurityProductionConfigValidator.java) applies when `SPRING_PROFILES_ACTIVE` selects `prod` or `production`; it rejects enabled dev auth/tools and requires shared database rate limiting plus documented admin protection. There is no committed `application-prod.yml`.
- `application-dev.yml` is local/ignored and is outside this production review. It must not be used as production configuration evidence.
- Relevant possible overrides that must remain absent in production are `SPRING_LDAP_EMBEDDED_*`, `SPRING_EXPRESSION_COMPILER_MODE`, and any custom configuration introducing `XsltView`, functional WebFlux/MVC SSE handlers, Aalto/JAXB WebFlux decoders, or `UnboundIdContainer`. `SPRING_PROFILES_ACTIVE` must be `prod` or `production` for the production validator to run.

### Committed Build and Runtime Audit

| Control | Evidence source and verification | Result | Remaining uncertainty |
| --- | --- | --- | --- |
| Docker build/runtime | `backend/tradepilot_backend_dockerfile.Dockerfile` builds with Maven, copies only `target/*.jar` into the runtime image, and starts only `java -Dserver.port=$PORT -jar app.jar`. | PASS for committed source | The live container filesystem was not inspected. |
| Spring configuration imports | Repository search found no `spring.config.import`, `SPRING_CONFIG_IMPORT`, `spring.config.location`, `SPRING_CONFIG_LOCATION`, additional-location, `configtree:`, `SPRING_APPLICATION_JSON`, `JAVA_TOOL_OPTIONS`, or `JDK_JAVA_OPTIONS` setting in committed backend runtime configuration. | PASS for committed source | A hosting platform could still inject a process-level option outside committed source. |
| Profile resources | The branch contains `application.yml`; there is no committed production-specific YAML/properties resource. `application-dev.yml` is ignored and excluded from packaging. | PASS for committed source | The live container filesystem was not inspected. |
| Embedded LDAP / UnboundID | Source and dependency-tree searches found no `UnboundIdContainer`, `unboundid-ldapsdk`, `spring-ldap-core`, `spring.ldap.embedded.*`, or LDAP configuration. | PASS for reviewed branch | Runtime configuration is addressed separately by owner attestation below. |
| SpEL compiler | Source/configuration searches found no `SimpleEvaluationContext`, `SpelExpressionParser`, `SpelParserConfiguration`, `spring.expression.compiler.mode`, or `SPRING_EXPRESSION_COMPILER_MODE`. | PASS for reviewed branch | Runtime configuration is addressed separately by owner attestation below. |
| XSLT / MVC views | All discovered application controllers use `@RestController`; source/configuration searches found no `XsltView`, view resolver, `ModelAndView`, or server-rendered MVC view mapping. | PASS for reviewed branch | A future dependency or code/configuration change requires re-review. |
| WebFlux, functional routing, and SSE | The POM declares `spring-boot-starter-web`, not WebFlux. The dependency tree contains no `spring-webflux` or `spring-boot-starter-webflux`; source searches found no `RouterFunction`, `ServerResponse.sse()`, `SseEmitter`, `Jaxb2Decoder`, or Aalto XML processor. | PASS for reviewed branch | The live service is master, not this feature branch. |
| Feature branch versus live master configuration | `7a8c2442ff44779aeff06dcb682dcf96a8527b4f` is an ancestor of this branch. Comparison found no feature-branch change to the Dockerfile, `application.yml`, `SecurityConfig`, or `SecurityProductionConfigValidator`; the branch changes dependency/workflow versions and Account Survival migration/code. | PASS for reviewed configuration sources | This does not prove the feature branch is deployed. |

### Read-only Render Production Evidence

The authorized Render connector identifies the live backend as service `tradepilot` (`srv-d801975b910c73deagqg`): Docker runtime, root directory `backend`, source branch `master`. Its latest live deployment is `dep-dbg0e9nb9u1s73foms60`, deployed from `master` commit `7a8c2442ff44779aeff06dcb682dcf96a8527b4f` on 2026-10-27. Startup logs record the active profile as `production` and show a successful application startup. This is **not** the feature-branch commit reviewed above, so it is evidence for the live service/profile only, not proof that the feature-branch dependency tree is deployed.

The following are **repository-owner attestations received for this review**, not data exposed by the Render connector: the service has no attached Environment Groups, no configured Secret Files, and its redacted direct environment-variable name inventory contains neither `SPRING_LDAP_EMBEDDED_*` nor `SPRING_EXPRESSION_COMPILER_MODE`. No environment value, credential, connection string, or private token is recorded in this document. Redaction is not interpreted as an empty runtime value.

| Control | Verification | Result |
| --- | --- | --- |
| Production profile | Render startup log on 2026-10-27: `The following 1 profile is active: "production"`. The supplied direct service-variable inventory intentionally redacted the profile entry and must not be interpreted as a runtime value. | PASS |
| Production security validator | The service started successfully with the `production` profile. In the reviewed master configuration, the validator would abort startup for enabled dev auth/tools, a non-database rate-limit store, or absent documented admin protection. | PASS for the deployed master runtime | This is not feature-branch deployment evidence. |
| Embedded LDAP | Owner attestation: no Environment Groups, no Secret Files, and no direct `SPRING_LDAP_EMBEDDED_*` name. Connector evidence and source/dependency review are negative. | PASS, subject to owner attestation | Connector cannot independently enumerate environment values, groups, Secret Files, or the container filesystem. |
| SpEL compiler | Owner attestation: no Environment Groups, no Secret Files, and no direct `SPRING_EXPRESSION_COMPILER_MODE` name. Source review is negative. | PASS, subject to owner attestation | Connector cannot independently enumerate environment values, groups, Secret Files, or the container filesystem. |
| XSLT MVC | Source review is negative; no custom view/XSLT control was identified in the direct inventory. | PASS for reviewed source/configuration | Deployed source revision is master; no container filesystem inspection exists. |
| WebFlux/SSE/Aalto/JAXB | Source/dependency review is negative; no related direct configuration control was identified. | PASS for reviewed source/configuration | Deployed source revision is master; no container filesystem inspection exists. |
| Kotlin Gradle plugin | Render's deployed Docker build log shows Maven compilation/package activity, not Gradle. The reviewed backend is Maven-based and has no Kotlin Gradle plugin dependency. | PASS |
| External configuration | Connector cannot enumerate environment groups, Secret Files, or container files. Repository-owner attestation confirms no Environment Groups and no Secret Files; committed source has no import/location mechanism. | PASS, subject to owner attestation | A future Render or image configuration change requires re-review. |

This is a source-and-config review, not a substitute for access to all deployed configuration sources. The owner attestation and redacted direct service-variable inventory must not be replaced with values in this repository. Before approval, attach the owner attestation to PR #1 or otherwise preserve it in the release record. Never attach environment values or secrets.

## Scope and Residual Risk

## Additional Artifact Associations from Run 41260116739 — OWNER-APPROVED TEMPORARILY

**Decision:** The sole owner explicitly **ACCEPTED TEMPORARILY** the additional eight exact CVE/PURL/version combinations in [PR #1 comment 6559383184](https://github.com/PunterDev/tradepilot/pull/1#issuecomment-6559383184). The owner reviewed the candidate false-positive assessments and acknowledged they are not conclusive. The approval authorizes only these exact additional suppressions; it does not attest that vulnerabilities have been patched or authorize merging/deployment. The scanner-compatible `until="2026-11-30Z"` deadline is earlier than the approved latest expiry of `2026-11-30T23:59:59Z`.

**Independent scanner evidence:** Original private GitHub Actions `dependency-check-report` artifact from [run `41260116739`](https://github.com/PunterDev/tradepilot/actions/runs/41260116739), artifact ID `13064971618` (GitHub retention through `2027-01-30T01:12:44Z`), has been retrieved and its eight additional CVE/PURL associations confirmed in [PR evidence verification comment 6559352354](https://github.com/PunterDev/tradepilot/pull/1#issuecomment-6559352354). Artifact ZIP SHA-256: `bb3e25dcaee64c107cecc85cf66cb44c3e94f093cef1aabdd30463142be6b92d`. This evidence verifies scanner attribution, not application exploitability. Maven tree, installed JARs, and vendor advisories were independently reviewed as described below.

| CVE | Reported PURL / score | Root dependency path and JAR evidence | Vendor-identified affected component and application exposure | Proposed disposition |
| --- | --- | --- | --- | --- |
| `CVE-2026-53914` | `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25` / 9.8 | `resend-java:4.13.0 -> okhttp:4.12.0 -> kotlin-stdlib-jdk8:1.9.25 -> kotlin-stdlib-jdk7:1.9.25` at runtime. Its POM identifies it as JDK 7 standard-library extensions. | The [vendor/GitHub advisory](https://github.com/advisories/GHSA-r937-wjx7-w2jp) names `kotlin-gradle-plugin`, build-cache metadata, and a `< 2.4.20-Beta1` affected range; the Maven backend has no Gradle or Kotlin build plugin. | **FALSE-POSITIVE CANDIDATE.** This is a different Kotlin artifact from the advisory package. Do not remove or override the facade without validating Resend/OkHttp compatibility. |
| `CVE-2026-59270` | `pkg:maven/org.springframework.security/spring-security-web@6.5.11` / 9.1 | Compile dependency from `spring-boot-starter-security:3.5.16`. Its installed JAR contains no LDAP or UnboundID classes. | The [Spring advisory](https://spring.io/security/cve-2026-59270/) requires `UnboundIdContainer` and a reachable embedded LDAP listener. Source, dependency tree, and the reviewed production configuration are negative for those prerequisites. | **FALSE-POSITIVE CANDIDATE.** The reported web-module PURL does not contain the identified component. Retain the existing `spring-security-core` review separately; neither PURL is newly approved. |
| `CVE-2026-47884` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Compile dependency through `spring-boot-starter-data-jpa:3.5.16 -> spring-data-jpa:3.5.13 -> spring-tx:6.2.19`. Its JAR contains no `XsltView` or MVC view classes. | The [Spring advisory](https://spring.io/security/cve-2026-47884/) identifies `XsltView` with an implicit `/**` MVC view-rendering mapping. The application has REST controllers and no XSLT/view configuration. | **FALSE-POSITIVE CANDIDATE for this PURL.** The app still carries Spring MVC through the web starter, so the original Framework configuration review remains relevant. |
| `CVE-2026-47890` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Same `spring-tx` path; no SSE, view-fragment, WebFlux, or MVC rendering classes occur in its JAR. | The [Spring advisory](https://spring.io/security/cve-2026-47890/) requires attacker-controlled data sent to other users via SSE view fragments. Source/configuration are negative for every prerequisite. | **FALSE-POSITIVE CANDIDATE for this PURL.** Do not treat the general Framework version range as proof that the transaction module implements this vulnerable path. |
| `CVE-2026-47891` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Same `spring-tx` path; no WebFlux, Aalto, or `Jaxb2Decoder` class occurs in its JAR. | The [Spring advisory](https://spring.io/security/cve-2026-47891/) requires WebFlux using Aalto XML input parsing. The backend uses servlet MVC and has no WebFlux/Aalto/JAXB decoder dependency or configuration. | **FALSE-POSITIVE CANDIDATE for this PURL.** |
| `CVE-2026-47892` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Same `spring-tx` path; no functional-routing classes occur in its JAR. | The [Spring advisory](https://spring.io/security/cve-2026-47892/) requires WebFlux functional endpoints deployed through `DispatcherServlet`. No `RouterFunction` or WebFlux dependency/configuration exists. | **FALSE-POSITIVE CANDIDATE for this PURL.** |
| `CVE-2026-59313` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Same `spring-tx` path; no `RouterFunction`, `ServerResponse.sse()`, or SSE classes occur in its JAR. | The [Spring advisory](https://spring.io/security/cve-2026-59313/) requires MVC functional routing, plain-text SSE, and attacker-controlled data sent to other users. Source/configuration are negative. | **FALSE-POSITIVE CANDIDATE for this PURL.** |
| `CVE-2026-59283` | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.1 | Same `spring-tx` path; no SpEL parser or `SimpleEvaluationContext` class occurs in its JAR. | The [Spring advisory](https://spring.io/security/cve-2026-59283/) requires `SimpleEvaluationContext` plus active `IMMEDIATE` or `MIXED` SpEL compilation. Source/configuration and owner-attested production variable names are negative. | **FALSE-POSITIVE CANDIDATE for this PURL.** The original `spring-core` association remains a distinct configuration-inapplicability assessment. |

### Remediation Options and Decision Gate

1. **Only the eight newly signed exact tuples may be suppressed.** The owner has now signed a separate temporary acceptance for these matches. The original eight exact rules remain unchanged. No wider artifact, CVE, version, or future release is approved.
2. **Evidence retained.** Run `41260116739` has a private, access-controlled GitHub Actions HTML artifact, identified above. Continue to distinguish shared CPE associations from module-specific exploitability.
3. **Prefer an aligned platform upgrade when a publicly available, compatible Spring Boot 3.x BOM selects vendor-fixed Framework and Security modules.** Spring lists `6.2.20` and Security `6.5.12` as Enterprise-only fixes, while public OSS fixes are on the Spring 7 / Security 7 lines. Direct module overrides would break BOM alignment and are not approved for V1.
4. **Keep Spring Boot 4 migration isolated.** It is the public OSS remediation route but changes the platform line and must not be folded into this release branch without a separately approved compatibility plan.

No action in this addendum changes the earlier approved scope, expiry, CVSS threshold, scanner behavior, or production configuration.

The additional eight matches are now accepted as *temporary risk exceptions*, not labeled conclusively benign. Revoke them if the documented prerequisites, packages, or risk evidence change. The XML and scope tests must keep all 16 exact combinations, and the unrelated-CVSS-8 negative-control CI must stay effective.

## Repeated CPE-to-Artifact Attribution Investigation — PROPOSED, NOT APPROVED

**Purpose:** explain why Dependency-Check continues to identify the same eight CVEs on different Maven modules without treating that explanation as approval for another exception. This investigation adds no suppression and changes no existing rule.

### Evidence Boundary

The original private HTML report for [run `41317447207`](https://github.com/PunterDev/tradepilot/actions/runs/41317447207) has now been independently downloaded through authenticated GitHub access and inspected. [Original report artifact `13087737945`](https://github.com/PunterDev/tradepilot/actions/runs/41317447207/artifacts/13087737945) is access-controlled and retained until `2027-01-30T14:41:35Z`. ZIP SHA-256: `be1cb642dce9a6f79f4ed13b42bef2b2f9a6f17e13fdb0d66ed6a046a4990776`; extracted HTML SHA-256: `2141d8e1e855788163b07be5350dd4868b6533da729247c3a1ebf02a693b17ff`. The HTML dependency detail sections include vendor/product/version evidence tables, PURLs, product-level CPE identifiers and vulnerability findings, and confirm all eight previously reported additional CVE/PURL associations. This report does **not** provide a JSON `evidenceCollected` field; its HTML evidence tables were used instead. These entries establish scanner identification, not application exploitability or proof of vendor-module applicability. No additional exception is authorized by this verification.

### Resolved Dependency Inventory and Association Matrix

| CVE family | Vendor-identified implementation | Resolved relevant modules | Reported/approved association history | Exact CPE binding in run `41317447207` | Application assessment |
| --- | --- | --- | --- | --- | --- |
| `CVE-2026-53914` | `org.jetbrains.kotlin:kotlin-gradle-plugin` build-cache metadata, not the standard library. | `kotlin-stdlib`, `kotlin-stdlib-jdk7`, `kotlin-stdlib-jdk8`, and `kotlin-stdlib-common` at `1.9.25`, all from `resend-java -> okhttp`. | Approved: `kotlin-stdlib`; separately approved: `kotlin-stdlib-jdk7`; newly reported: `kotlin-stdlib-jdk8`. | **Verified:** `cpe:2.3:a:jetbrains:kotlin:1.9.25:*:*:*:*:*:*:*` assigned to the runtime JDK8 stdlib facade; HTML product evidence shows Kotlin Stdlib Jdk8, not a Gradle plugin. | No Gradle/Kotlin plugin or build-cache execution exists. This is a module-attribution mismatch candidate, but `jdk8` is not approved. |
| `CVE-2026-59270` | Spring Security embedded `UnboundIdContainer` LDAP server. | `spring-security-config`, `spring-security-core`, `spring-security-crypto`, `spring-security-web`, and test support at `6.5.11`. | Approved: `spring-security-core`, `spring-security-web`; newly reported: `spring-security-config`. | **Verified:** `cpe:2.3:a:pivotal_software:spring_security:6.5.11:*:*:*:*:*:*:*` and `cpe:2.3:a:vmware:spring_security:6.5.11:*:*:*:*:*:*:*` assigned to `spring-security-config`. | `spring-security-config` contains LDAP configuration helpers but no `UnboundIdContainer`; no `spring-security-ldap`/UnboundID dependency, LDAP listener, or `spring.ldap.embedded.*` configuration was found. The only vendor path is absent. |
| `CVE-2026-47884`, `CVE-2026-47890`, `CVE-2026-59313` | `XsltView`, MVC SSE/view fragments, or MVC functional SSE are implemented in `spring-webmvc`. | Framework `6.2.19`: `spring-aop`, `spring-aspects`, `spring-beans`, `spring-context`, `spring-core`, `spring-expression`, `spring-jcl`, `spring-jdbc`, `spring-orm`, `spring-test`, `spring-tx`, `spring-web`, `spring-webmvc`. | Approved: `spring-core`, `spring-tx`; newly reported: `spring-aop`. | **Verified:** `spring-aop` is assigned the same Spring Framework 6.2.19 product-level CPE identifiers (`pivotal_software`, `springsource`, `vmware` vendors). The report's evidence tables identify the artifact as Spring AOP. | `spring-webmvc` is present, so the vulnerable feature classes exist in the application distribution. Source/configuration remain negative for `XsltView`, functional routing, SSE, view fragments, plain-text streaming, and attacker-controlled cross-user streams. |
| `CVE-2026-47891`, `CVE-2026-47892` | WebFlux Aalto/JAXB decoding or WebFlux functional endpoints. | Same Framework inventory; `spring-webflux` is not resolved. | Approved: `spring-core`, `spring-tx`; newly reported: `spring-aop`. | **Verified:** same `spring-aop` product-level Spring Framework CPE association; this does not imply WebFlux code exists in `spring-aop`. | Servlet MVC is used. No WebFlux dependency, Aalto XML processor, `Jaxb2Decoder`, or `RouterFunction` implementation is resolved or configured. |
| `CVE-2026-59283` | `SimpleEvaluationContext` plus active SpEL compilation. | `spring-expression:6.2.19` is resolved; `spring-aop`, `spring-core`, and `spring-tx` are also present. | Approved: `spring-core`, `spring-tx`; newly reported: `spring-aop`. | **Verified:** same `spring-aop` product-level Spring Framework CPE association; this does not prove the SpEL implementation resides there. | The SpEL classes exist in `spring-expression`, but no evaluated `SimpleEvaluationContext`, compiler-enabled parser, or `IMMEDIATE`/`MIXED` compiler setting exists in reviewed code or production variable-name inventory. |

### Root-Cause Assessment

The retrieved HTML report **confirms product-level CPE assignments to the newly reported sibling modules**: `jetbrains:kotlin` on `kotlin-stdlib-jdk8`, `spring_framework` on `spring-aop`, and `spring_security` on `spring-security-config`. The HTML evidence tables show these JARs' own artifact names and matching product-family hints. This supports the repeated CVE associations being caused by broad product-CPE matching rather than a finding that the advisory-specific implementation is located in each sibling JAR. It does **not** establish that the entire application is unaffected: `spring-webmvc` and `spring-expression` are resolved and contain some advisory-relevant APIs. Exact PURL suppressions preserve owner-approved scope, but do not automatically suppress sibling-module matches.

Do not replace exact PURL rules with a Spring/Kotlin CPE suppression or a CVE-wide suppression. Either would hide future CVEs on the same product family, violate the existing approval boundary, and reduce the negative-control value of Dependency-Check.

### Sustainable Remediation Strategy

1. **Preferred long term: an isolated, BOM-aligned Spring Boot 4 migration.** Spring's public OSS fixes for the Framework and Security findings are on the Spring 7/Security 7 lines. This must remain a separately reviewed migration because it changes platform, Jakarta/Hibernate/Jackson compatibility, and release risk.
2. **Near term: retain only report-proven, owner-approved exact PURL/version tuples.** The private HTML report and authenticated evidence inspection are recorded above. Before any future approval, assess each reported CPE association alongside vendor-specific prerequisites and record an explicit owner decision. Do not pre-approve every currently resolved sibling module.
3. **Do not add direct Spring module overrides.** `6.2.20` and `6.5.12` are listed by Spring as Enterprise-only; overriding individual modules breaks Boot BOM alignment and is not a V1 remediation.
4. **Kotlin/Resend:** keep the runtime chain unless an upstream Resend/OkHttp release removes it compatibly. The reported advisory targets the Gradle plugin, so removing Kotlin facades solely to silence Dependency-Check is not justified.

### Required Next Evidence and Decision

The minimal next owner decision, if the scan reports another sibling module, is a newly signed tuple-level decision for that exact `(CVE, PURL, version)` only, supported by the authenticated HTML CPE evidence above and the vendor-specific applicability analysis. No currently unresolved module is approved by this section. Immediate review is required on any Spring/Kotlin/Resend version change, addition of WebFlux, SSE, XSLT views, functional routing, SpEL compilation, LDAP/UnboundID, or a new CVE on an approved dependency.

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
  <suppress until="2026-11-30Z">
    <packageUrl regex="true">^pkg:maven/org\.jetbrains\.kotlin/kotlin-stdlib@1\.9\.25$</packageUrl>
    <cve>CVE-2026-53914</cve>
  </suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47884</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47890</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47891</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-47892</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-59313</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework/spring-core@6\.2\.19$</packageUrl><cve>CVE-2026-59283</cve></suppress>
  <suppress until="2026-11-30Z"><packageUrl regex="true">^pkg:maven/org\.springframework\.security/spring-security-core@6\.5\.11$</packageUrl><cve>CVE-2026-59270</cve></suppress>
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

- [x] Original HTML reports for runs `41233630140` and `41232107045` independently verified from the owner-supplied evidence package; hashes and exact CVSS/PURL comparison recorded above.
- [ ] Evidence package or access-controlled retention link added to PR #1 for authorized reviewers.
- [ ] Repository-owner Render attestation (no Environment Groups, no Secret Files, and no reviewed override names) attached to the PR/release record.
- [ ] Repository owner explicitly adopts or rejects the proposed solo-owner governance amendment.
- [ ] Repository owner records a decision using the solo-owner statement below.
- [ ] Owner confirms that no external or contractual requirement for independent approval applies, or records that requirement as a blocker.
- [ ] Independent reviewer decision recorded, if one becomes available (recommended but not required by the reviewed repository policy).
- [ ] Expiry accepted and calendar review scheduled before 2026-11-30.
- [ ] Proposed suppression scope and activation tests reviewed.
- [ ] Release owner confirms no prerequisite feature/configuration has been introduced.

## PR #1 Evidence Verification Comment Template

Post this factual verification comment only after the evidence package or an authorized retention link is available to PR #1 reviewers. Do not claim the raw reports are attached until they actually are.

```text
OWASP Dependency-Check evidence verification for Account Survival V1

Source: owner-supplied TradePilot_V1_OWASP_Approval_Evidence.zip, independently verified locally without executing archive content.
Runs: 41232107045 and 41233630140
Report SHA-256:
- 41232107045: ad7d92bf1197c2077bdacc3813d692cfb7a908e687e08cd1065193ec87c181d2
- 41233630140: 3b262631cff1371aebe3c8681a92016b5e743997b2a09ab5ac61844981fee78b
Blocking CVSS >= 8 findings: 8 in each report.
Comparison: identical `(CVE, PURL, CVSS)` tuples; 0 added, 0 removed, 0 changed.
Evidence location for authorized reviewers: <private PR attachment or approved retention link>

This verifies scan evidence only. It does not approve a risk exception, activate a suppression, or change the CVSS >= 8 gate.
```

## Historical Two-Person Approval Template

The original two-person proposal remains available when an independent reviewer is assigned. It is not silently removed by the proposed solo-owner amendment.

Approvers must post their own comments. Do not post this template on their behalf.

```text
Security risk-acceptance decision for Account Survival V1

Role: Repository owner | Independent security risk owner
Name: <name>
Decision: APPROVE | REJECT
Date (UTC): <timestamp>

I reviewed:
- dependency baseline: 8d202d2f3aa23fe45a672723bc57f33b149ea424
- risk-acceptance document revision: <commit SHA containing this document>
- scope: only the eight exact CVE/PURL/version tuples in docs/security/ACCOUNT_SURVIVAL_V1_DEPENDENCY_CHECK_RISK_ACCEPTANCE.md
- Dependency-Check evidence: GitHub Actions runs 41233630140 and 41232107045, attached or linked in this PR
- production configuration attestation: no Environment Groups, no Secret Files, and no reviewed direct override names

I accept the documented residual risk only until 2026-11-30T23:59:59Z. I understand the exception is revoked immediately by any listed dependency, framework, configuration, or prerequisite-feature change. I confirm that the CVSS >= 8 gate remains unchanged and that no exception is active until the approved, exact-scope activation change and its required regression checks are reviewed.
```

## Proposed Solo-Owner Decision Statement

The repository owner must post and sign their own statement after deciding whether to adopt the proposed governance amendment. This template does not assign a security risk owner, claim independent review, approve an exception, or activate a suppression by itself.

```text
Solo-owner security risk decision for Account Survival V1

Name: <repository owner name>
Role: Repository owner and accountable TradePilot Management risk decision-maker
Decision on proposed solo-owner governance amendment: ADOPT | REJECT
Risk decision: ACCEPT TEMPORARILY | REMEDIATE | NEEDS REVIEW
Date (UTC): <timestamp>

I reviewed:
- dependency baseline: 8d202d2f3aa23fe45a672723bc57f33b149ea424
- risk-acceptance document revision: <commit SHA containing this document>
- scope: only the eight exact CVE/PURL/version tuples in docs/security/ACCOUNT_SURVIVAL_V1_DEPENDENCY_CHECK_RISK_ACCEPTANCE.md
- Dependency-Check evidence: GitHub Actions runs 41233630140 and 41232107045, attached or linked for authorized review
- production configuration attestation: no Environment Groups, no Secret Files, and no reviewed direct override names

I confirm that TradePilot is currently solo-owned and solo-operated, that no independent security reviewer is assigned, and that this is self-review rather than independent assurance. To my knowledge, no contractual, regulatory, insurer, or certification requirement requires a separate approver for this decision. If that is not true, this decision is void and the exception remains inactive.

If I select ACCEPT TEMPORARILY, I accept the documented residual risk only through 2026-11-30T23:59:59Z. I acknowledge every listed dependency, framework, configuration, prerequisite-feature, scan-scope, and expiry revocation trigger. I confirm the CVSS >= 8 gate remains unchanged. No security exception is active until a separate exact-scope implementation change and its required regression checks are reviewed and merged.
```
