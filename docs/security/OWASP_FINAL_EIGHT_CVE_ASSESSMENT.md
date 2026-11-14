# Final OWASP Eight-CVE Assessment

**Status:** The original eight exact tuples are active under the historical
approval. The expanded sixteen-tuple amendment below is **PENDING** and is not
referenced by Maven or GitHub Actions.

**Repository / branch / PR:** `PunterDev/tradepilot` / `fix/backend-dependency-security` / PR #6
**Original reviewed commit:** `5d6b05af6a2c7234ec3d666d8cabc3d45f020a35`
**Amendment baseline:** `db2ace78d080b86bd1520c7b027398557235c933`
**Candidate expiry:** `2026-11-30T23:59:59Z`
**Scanner gate:** `failBuildOnCVSS=8` and `failOnError=true` remain required.

## Evidence and scope

The original eight-tuple scope was activated separately and is bound to PR #6
comment `6620183351` and candidate SHA-256
`f9b964c787c906e6ba780ce675f00a409021b5efd54785e0430cf35872bfa3eb`.
That authorization is historical and remains limited to those eight exact
tuples. It does not authorize the amendment.

GitHub Actions run
[`41757589036`](https://github.com/PunterDev/tradepilot/actions/runs/41757589036),
artifact `13288697637` (`dependency-check-report`), was downloaded through an
authenticated GitHub session on 2026-11-03. The extracted HTML SHA-256 is
`3bcaeaf6affd93518e9217c329332eea232f4593fc3d1a4b8a082929af1dc272`.
It records eight additional CVE/PURL/version associations at CVSS >= 8 while
the original eight suppressions remain in effect. This document treats that
report as direct artifact evidence; artifact ID retention is owner-supplied
metadata, not an independently re-read GitHub retention setting.

The authoritative current report is GitHub Actions run
[`41737155331`](https://github.com/PunterDev/tradepilot/actions/runs/41737155331),
artifact `13279572975` (`dependency-check-report`). The HTML was downloaded from
the authenticated GitHub artifact API on 2026-11-03 and its SHA-256 was verified
as `b654c0f44b8db1a907b05a9bf942562535ac5780d13087bdec3fadc20c73a849`.
This equals the supplied report-HTML hash. The supplied artifact-ZIP hash is
`ed0ff535d5aba572893b91fd32c97dcb0ed0999fe782e045e2d8683d3fd60704`; it is
recorded as supplied evidence because the GitHub client download extracted the
artifact before this review could hash the ZIP container directly.

Run `41737155331` successfully completed the Dependency-Check 12.2.2 NVD update
and cache save, plus Gitleaks, Semgrep, frontend npm audit, and the PostgreSQL
Flyway baseline. `dependency-scan` then failed only at the unchanged CVSS 8 gate.
The report contains the exact eight tuples below. No lower-severity finding is
proposed for exception.

The earlier owner-supplied evidence ZIP remains untracked by design. It
reconciles the historical eight-tuple evidence from runs `41232107045` and
`41233630140`; it is evidence only, not a risk decision.

## Effective dependency and configuration evidence

The reviewed backend is Java 17 and Maven-based. Its Spring Boot 3.5.16 BOM
resolves Spring Framework 6.2.19 and Spring Security 6.5.11. A dependency-tree
query resolved none of Spring WebFlux, Aalto XML, UnboundID LDAP, Spring LDAP,
or the Kotlin Gradle plugin. The resolved Kotlin path is:

```text
resend-java:4.13.0 -> okhttp:4.12.0 -> kotlin-stdlib-jdk8:1.9.25 -> kotlin-stdlib:1.9.25
```

The source/configuration audit resolved no `XsltView`, view resolver,
`ModelAndView`, `RouterFunction`, `ServerResponse.sse`, `SseEmitter`, WebFlux,
Aalto, `Jaxb2Decoder`, `SimpleEvaluationContext`, SpEL compiler configuration,
UnboundID, LDAP, or `spring.ldap.embedded.*` setting. All discovered HTTP
controllers are `@RestController`; `SecurityConfig` uses stateless JWT,
`DaoAuthenticationProvider`, and BCrypt.

Committed runtime configuration has no `spring.config.import`, additional
configuration location, `configtree:`, `SPRING_APPLICATION_JSON`,
`JAVA_TOOL_OPTIONS`, or `JDK_JAVA_OPTIONS` reference. The Dockerfile copies only
the packaged JAR and starts `java -Dserver.port=$PORT -jar app.jar`. This proves
the reviewed source does not introduce an external configuration location; it
does **not** inspect a live container filesystem.

The repository owner previously supplied a redacted, name-only Render inventory
and attested that production uses profile `production`, has no attached
Environment Groups or Secret Files, and has no `SPRING_LDAP_EMBEDDED_*` or
`SPRING_EXPRESSION_COMPILER_MODE` variable name. No values are recorded here.
This is owner attestation, not independently authenticated Render evidence. The
deployed service is on `master`, not this feature branch. Production
configuration is therefore **OWNER_ATTESTED_WITH_LIMITATIONS**, not fully or
independently verified. Live container files, hosted JVM properties, and future
configuration drift remain unverified. A solo-founder may explicitly accept
those configuration residual risks only through the authenticated policy path
documented below and only if no applicable external obligation requires
independent verification.

## Original eight-tuple matrix

| CVE | Exact PURL / scanner CVSS | Vendor severity and prerequisite | Classification and disposition |
| --- | --- | --- | --- |
| [CVE-2026-53914](https://github.com/advisories/GHSA-r937-wjx7-w2jp) | `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25` / 9.8 | GitHub advisory: Moderate. Affects `kotlin-gradle-plugin` before 2.4.20-Beta1 and build-cache metadata deserialization. | **Incorrect artifact attribution.** Maven does not apply Kotlin Gradle or execute its cache. Candidate exact exception only; approval pending. |
| [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.8 | Spring: Medium. Requires `XsltView`, implicit `/**` MVC view rendering, and no explicit view name. | **Affected artifact; configuration-inapplicable in reviewed source.** No XSLT view, view resolver, `ModelAndView`, or server-rendered mapping. Candidate only; production verification and approvals pending. |
| [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.8 | Spring: Low. Requires MVC/WebFlux SSE view fragments and attacker-controlled content delivered to other users. | **Affected artifact; configuration-inapplicable in reviewed source.** No SSE endpoint/emitter, WebFlux, or view fragments. Candidate only; production verification and approvals pending. |
| [CVE-2026-47891](https://spring.io/security/cve-2026-47891/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.8 | Spring: Medium. Requires WebFlux, Aalto XML, and `Jaxb2Decoder`. | **Affected artifact; configuration-inapplicable in reviewed source.** Servlet `spring-boot-starter-web`; no WebFlux, Aalto, or decoder dependency/configuration. Candidate only; production verification and approvals pending. |
| [CVE-2026-47892](https://spring.io/security/cve-2026-47892/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.8 | Spring: Medium. Requires WebFlux functional endpoints through `DispatcherServlet` and an affected header predicate. | **Affected artifact; configuration-inapplicable in reviewed source.** No WebFlux or `RouterFunction` endpoint. Candidate only; production verification and approvals pending. |
| [CVE-2026-59313](https://spring.io/security/cve-2026-59313/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.8 | Spring: Low. Requires MVC functional `RouterFunction`, `ServerResponse.sse()`, plain-text SSE, and attacker-controlled streamed content. | **Affected artifact; configuration-inapplicable in reviewed source.** No functional MVC route, SSE response, or streamed attacker-controlled payload. Candidate only; production verification and approvals pending. |
| [CVE-2026-59283](https://spring.io/security/cve-2026-59283/) | `pkg:maven/org.springframework/spring-core@6.2.19` / 9.1 | Spring: Medium. Requires `SimpleEvaluationContext` and SpEL compiler mode `IMMEDIATE` or `MIXED`. | **Affected artifact; configuration-inapplicable in reviewed source.** No matching context, compiler configuration, or user-controlled SpEL evaluation. Production property/system-property verification is still required. |
| [CVE-2026-59270](https://spring.io/security/cve-2026-59270/) | `pkg:maven/org.springframework.security/spring-security-core@6.5.11` / 9.1 | Spring: Critical. Requires embedded `UnboundIdContainer` directly or through `spring.ldap.embedded.*`, with a reachable LDAP listener. | **Affected artifact; configuration-inapplicable in reviewed source.** No UnboundID/LDAP dependency, configuration, container, or listener. Production verification is still required. |

The Spring Framework 6.2.20 and Spring Security 6.5.12 fixes cited by the
advisories are not publicly consumable in the reviewed build without authorized
commercial support. Their public OSS fixes are on the Spring Framework/Security
7 line, which implies an unapproved Spring Boot 4 migration. No unsupported,
beta, or enterprise-only dependency has been added.

## Pending sixteen-tuple amendment

Dependency-Check 12.2.2 reported these additional associations in run
`41757589036`. They map to the same eight CVEs but are distinct artifact
coordinates and must receive a new exact-scope approval rather than inheriting
the original approval.

| CVE | New exact PURL / scanner CVSS | Applicability evidence | Pending disposition |
| --- | --- | --- | --- |
| CVE-2026-53914 | `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25` / 9.8 | The report describes Kotlin build-cache metadata deserialization; reviewed Maven/runtime source has no `kotlin-gradle-plugin` or Gradle build cache. `kotlin-stdlib-jdk7` is a compatibility runtime artifact, not the plugin. | Pending exact tuple only; wrong-artifact assessment matches the original Kotlin association. |
| CVE-2026-47884 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | The advisory requires `XsltView` plus unsafe implicit MVC view mapping. Reviewed source/configuration has neither. | Pending exact tuple only; generic Spring Framework attribution is not proof of the prerequisite. |
| CVE-2026-47890 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Requires MVC/WebFlux SSE view fragments and attacker-controlled content. Reviewed source has no SSE, WebFlux, or view fragments. | Pending exact tuple only. |
| CVE-2026-47891 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Requires WebFlux, Aalto XML, and `Jaxb2Decoder`. The reviewed application is servlet MVC and resolves none of those prerequisites. | Pending exact tuple only. |
| CVE-2026-47892 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Requires WebFlux functional endpoints deployed through `DispatcherServlet`; no `RouterFunction` or WebFlux route exists in reviewed source. | Pending exact tuple only. |
| CVE-2026-59313 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.8 | Requires functional MVC `RouterFunction` SSE and streamed attacker-controlled content; neither is present in reviewed source. | Pending exact tuple only. |
| CVE-2026-59283 | `pkg:maven/org.springframework/spring-tx@6.2.19` / 9.1 | Requires `SimpleEvaluationContext` with active SpEL compilation. Source has neither; the owner-attested production review found no `SPRING_EXPRESSION_COMPILER_MODE` name. Hosted JVM properties remain unverified. | Pending exact tuple only, with the same attestation limitation as the original scope. |
| CVE-2026-59270 | `pkg:maven/org.springframework.security/spring-security-web@6.5.11` / 9.1 | Requires a reachable embedded `UnboundIdContainer` LDAP listener. Reviewed dependencies/source have no UnboundID or LDAP configuration; owner attestation found no `SPRING_LDAP_EMBEDDED_*` name. | Pending exact tuple only, with the same attestation limitation as the original scope. |

The new candidate is
[`candidates/dependency-check-final-sixteen-candidate.xml`](candidates/dependency-check-final-sixteen-candidate.xml).
It contains the original eight entries unchanged plus exactly the eight rows
above. It is deliberately not wired into the active workflow. Its SHA-256 is
`fd40e63b366dc3ba04b4c30738cbef6c38dc1fc971b9d8e2cf264e791d203070`; a new
authenticated owner comment must bind to that hash before activation can be
considered.

## Candidate exception package and controls

The active original candidate file is
[`candidates/dependency-check-final-eight-candidate.xml`](candidates/dependency-check-final-eight-candidate.xml).
It contains exactly eight one-CVE, one-PURL, exact-version entries. Dependency-
Check 12.2.2's schema requires a date-only `until`, so each uses the conservative
machine expiry `2026-11-30` (no later than the human approval deadline of
`2026-11-30T23:59:59Z`). It has no CPE, CVSS, wildcard, or package-family matcher.
It is referenced by the guarded active workflow only after the historical
approval and does not apply to the pending sixteen-tuple amendment.
Its current SHA-256 is
`f9b964c787c906e6ba780ce675f00a409021b5efd54785e0430cf35872bfa3eb`.
Dependency-Check 12.2.2's own `SuppressionParser` parsed exactly eight rules from
this file in local validation.

[`scripts/security/validate-owasp-exception-candidates.py`](../../scripts/security/validate-owasp-exception-candidates.py)
is independent of the scanner. It rejects malformed XML, non-exact PURLs,
unapproved CVEs, changed versions, duplicate/missing scope, expired entries,
broad matcher elements, missing approvals, missing production verification, and
unrelated CVSS >= 8 findings. Its tests include those negative controls.

Any future activation must additionally pass the validator with
`--verify-github-approvals`. That mode uses the GitHub API and a CI-provided
`GITHUB_TOKEN` to verify the author and content of recorded PR comments. An
editable JSON field alone is deliberately insufficient.

The default governance path requires distinct repository-owner,
security-risk-owner, and production-configuration-verifier evidence. TradePilot
may instead use the narrowly defined **solo-founder path** only when the sole
owner explicitly adopts that policy in an authenticated PR #6 comment and no
applicable external obligation requires independent review. Owner
authorization, temporary risk acceptance, and production-configuration
verification remain separate evidence requirements. Under this path, one
verified GitHub identity may supply all three only when the comment states that
no independent security review occurred, identifies the exact eight tuples,
binds to the reviewed commit and candidate SHA-256, accepts both the dependency
and configuration residual risks through the expiry, and confirms that the
exception is temporary rather than a permanent vulnerability waiver. The
configuration evidence remains `OWNER_ATTESTED_WITH_LIMITATIONS`; it is never
promoted to independent verification. This is owner self-review, not
independent security assurance.

The current approval record is only a PENDING template at
[`candidates/owasp-final-eight-approval.template.json`](candidates/owasp-final-eight-approval.template.json).
It cannot satisfy activation mode.

## Approval, residual risk, and revocation

PR #6 comment `6620183351`, authored by `PunterDev` with GitHub association
`OWNER`, records authenticated solo-founder authorization for this exact scope.
The October 31 Render evidence remains owner-supplied production-configuration
evidence, not independently authenticated connector evidence. It covers the
`production` profile, absence of Environment Groups and Secret Files, and
absence of the reviewed LDAP and SpEL variable names. The reviewed Dockerfile,
runtime command, committed Spring configuration, and dependency graph were
separately audited; live container filesystem, hosted JVM system properties,
and future dashboard configuration changes remain outside that source review.
The approved policy requires those limitations to be explicit in both the
record and comment; it does not promote them to independent verification.

Before activation, the approved governance path must identify the reviewed
commit, document revision, all eight exact tuples, expiry, evidence location,
and an explicit residual-risk decision. For the solo-founder path, the owner
must also state that no applicable independent-review obligation was identified;
if that statement cannot be made accurately, the exception remains blocked.
Approval is immediately revoked by a change to reviewed dependency versions,
Spring stack, Kotlin/Resend path, any listed vulnerable feature/configuration,
production configuration source, a new CVSS >= 8 finding, or expiry.

## Human approval template

Do not complete this on another person's behalf.

```text
Security exception decision for TradePilot Coach PR #6

Role: Repository owner | Independent security-risk owner
Name: <name>
Decision: APPROVE | REJECT
UTC timestamp: <timestamp>

I reviewed commit 5d6b05af6a2c7234ec3d666d8cabc3d45f020a35,
docs/security/OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md, the retained Dependency-Check
artifact 13279572975 from run 41737155331, and the exact eight CVE/PURL/version
tuples in the inactive candidate XML. I accept/reject the documented residual
risk only through 2026-11-30T23:59:59Z. I understand that activation requires
verified production configuration, exact-scope validation, unchanged CVSS >= 8
and fail-on-error gates, and a separate reviewed implementation change.
```

The executable solo-founder comment template and activation sequence are in
[`OWASP_EXCEPTION_APPROVAL_2026-11.md`](OWASP_EXCEPTION_APPROVAL_2026-11.md).
