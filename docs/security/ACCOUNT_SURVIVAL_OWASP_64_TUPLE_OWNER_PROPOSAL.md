# Account Survival V1 OWASP 64-Tuple Owner Proposal

**Status: PROPOSED ONLY. No new suppression is active.**

## Evidence and Scope

Authenticated GitHub Actions run `41336529191` compared complete unsuppressed Dependency-Check 13.0.0 reports. Bundling enabled reported 82 top-level and 36 related entries; bundling disabled reported 118 top-level and zero related entries. Both modes represented 115 distinct Maven PURLs and all eight CVSS >= 8 CVE identities. The unbundled report produced 80 actual `(CVE, PURL, version)` associations: 16 already have owner-approved, exact-scope exceptions and 64 do not.

This proposal contains **only** those 64 remaining tuples. It does not approve broad CPE, family, version, or future-release matching. Its expiry must be no later than `2026-11-30Z`; the existing scanner-compatible earlier expiry is retained. The source reports remain in private artifact `13095821500` for authorized review.

## Classification Summary

| Classification | Tuples | Decision |
| --- | ---: | --- |
| Safe compatible upgrade/remediation available | 0 | No public, BOM-aligned Boot 3 remediation is available. Do not override individual Spring modules. |
| Demonstrably non-applicable; eligible for temporary exception review | 64 | Listed below; eligibility is conditional on the stated runtime/configuration evidence remaining true. |
| Potentially exploitable; requires remediation | 0 | No reviewed prerequisite is present. A later prerequisite change immediately revokes eligibility. |
| Insufficient evidence | 0 | The private reports, resolved runtime tree, committed source, and owner-attested production configuration inventory cover the stated prerequisites. |

This classification is not a claim that shared product CPE attribution makes a JAR harmless. The Spring MVC and SpEL implementations are present in the application distribution; the assessment is limited to the absence of each vendor-required feature/configuration path.

## Verified Prerequisites

Vendor advisory evidence for the eight CVEs is recorded in the canonical [risk-acceptance review](ACCOUNT_SURVIVAL_V1_DEPENDENCY_CHECK_RISK_ACCEPTANCE.md): [Kotlin](https://github.com/advisories/GHSA-r937-wjx7-w2jp), [Spring 47884](https://spring.io/security/cve-2026-47884/), [47890](https://spring.io/security/cve-2026-47890/), [47891](https://spring.io/security/cve-2026-47891/), [47892](https://spring.io/security/cve-2026-47892/), [59313](https://spring.io/security/cve-2026-59313/), [59283](https://spring.io/security/cve-2026-59283/), and [Spring Security 59270](https://spring.io/security/cve-2026-59270/). The advisory-to-PURL mapping below is exact only because the hosted unbundled report recorded each listed tuple.

| CVE | Vendor-required path | TradePilot evidence |
| --- | --- | --- |
| `CVE-2026-53914` | Kotlin **Gradle plugin** build-cache metadata deserialization | Runtime Kotlin standard-library facades arrive through `resend-java -> okhttp`; no Gradle plugin/build-cache execution exists. |
| `CVE-2026-47884` | `XsltView` plus implicit MVC `/**` view mapping | REST controllers only; no `XsltView`, server-rendered view, or implicit view mapping. |
| `CVE-2026-47890` | SSE view fragments with attacker-controlled cross-user content | No `SseEmitter`, WebFlux, view fragments, or SSE endpoint. |
| `CVE-2026-47891` | WebFlux + Aalto XML + `Jaxb2Decoder` | Servlet MVC starter only; no WebFlux, Aalto, or JAXB decoder. |
| `CVE-2026-47892` | WebFlux functional endpoint through `DispatcherServlet` | No WebFlux or `RouterFunction`. |
| `CVE-2026-59313` | MVC `RouterFunction` + `ServerResponse.sse()` + plaintext SSE | No functional MVC routing, SSE response, or streamed user-controlled payload. |
| `CVE-2026-59283` | `SimpleEvaluationContext` plus SpEL compiler mode `IMMEDIATE`/`MIXED` | No matching source API, parser configuration, or reviewed production override name. |
| `CVE-2026-59270` | Reachable embedded `UnboundIdContainer` LDAP server | No UnboundID/LDAP dependency, listener, `spring.ldap.embedded.*` setting, or LDAP authentication; auth is stateless JWT plus `DaoAuthenticationProvider`. |

The production evidence boundary is the committed configuration plus owner-attested Render inventory: active `production` profile, no Environment Groups, no Secret Files, and no `SPRING_LDAP_EMBEDDED_*` or `SPRING_EXPRESSION_COMPILER_MODE` variable names. Redacted values were not treated as evidence.

## Exact Additional Tuples

Every PURL/CVE entry below is an individual proposed exception. A group is only shorthand for its explicitly listed Cartesian pairs.

### Kotlin runtime facades: 2 tuples

| PURL | CVE | Version | Classification |
| --- | --- | --- | --- |
| `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk8@1.9.25` | `CVE-2026-53914` | `1.9.25` | Non-applicable Kotlin Gradle-plugin mismatch; eligible for review. |
| `pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-common@1.9.25` | `CVE-2026-53914` | `1.9.25` | Non-applicable Kotlin Gradle-plugin mismatch; eligible for review. |

### Spring Security runtime modules: 2 tuples

| PURL | CVE | Version | Classification |
| --- | --- | --- | --- |
| `pkg:maven/org.springframework.security/spring-security-config@6.5.11` | `CVE-2026-59270` | `6.5.11` | Embedded-LDAP prerequisite absent; eligible for review. |
| `pkg:maven/org.springframework.security/spring-security-crypto@6.5.11` | `CVE-2026-59270` | `6.5.11` | Embedded-LDAP prerequisite absent; eligible for review. |

### Spring Framework runtime modules: 60 tuples

The following six CVEs apply to **each** of the ten exact PURLs in this table, creating 60 explicitly scoped tuples: `CVE-2026-47884`, `CVE-2026-47890`, `CVE-2026-47891`, `CVE-2026-47892`, `CVE-2026-59313`, and `CVE-2026-59283`. Each tuple is configuration-inapplicable only while its corresponding prerequisite in the table above remains absent.

| Exact PURL (all version `6.2.19`) | Six exact proposed CVEs |
| --- | --- |
| `pkg:maven/org.springframework/spring-aop@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-aspects@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-beans@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-context@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-expression@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-jcl@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-jdbc@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-orm@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-web@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |
| `pkg:maven/org.springframework/spring-webmvc@6.2.19` | `47884`, `47890`, `47891`, `47892`, `59313`, `59283` |

## Required Owner Decision

The owner must explicitly approve or reject this exact 64-tuple scope, confirm the reviewed baseline and private evidence, accept residual risk only through `2026-11-30Z`, and confirm no external obligation requires independent review. Approval does **not** activate XML rules, enable unbundling in the mandatory workflow, merge the PR, or deploy.

```text
OWASP 64-tuple temporary risk decision for Account Survival V1

Decision: APPROVE / REJECT
Owner: <full name>
UTC timestamp: <timestamp>
Reviewed branch baseline: cc5b45d6f5a1a3a3a25789bd4547bab25c6fcbd5 or later with no dependency/configuration drift
Evidence: GitHub Actions run 41336529191, private artifact 13095821500
Scope: only the 64 exact CVE/PURL/version pairs in docs/security/ACCOUNT_SURVIVAL_OWASP_64_TUPLE_OWNER_PROPOSAL.md
Expiry: 2026-11-30Z

I acknowledge this is a temporary, exact-scope risk acceptance; it does not make a broad Spring/Kotlin CPE or CVE exception, and it is immediately revoked by dependency, configuration, prerequisite-feature, scan-scope, or expiry changes.
```

## Revocation and Implementation Gates

Immediately revoke the proposed scope if any Spring Boot/Framework/Security/Kotlin/Resend version changes; WebFlux, XSLT, MVC views, SSE, functional routing, SpEL compilation, LDAP, or UnboundID is added; relevant production environment names/configuration sources change; a new CVE appears; or the expiry passes.

After explicit approval, a single implementation may add the 64 exact XML rules and extend the scope tests, then enable the verified unbundled mandatory scan. It must pass full backend, PostgreSQL/Flyway, Account Survival, frontend, Playwright, Gitleaks, Semgrep, npm audit, actual Dependency-Check, and Log4j negative-control gates. Until then, the mandatory workflow remains fail-closed and the release is NO-GO.
