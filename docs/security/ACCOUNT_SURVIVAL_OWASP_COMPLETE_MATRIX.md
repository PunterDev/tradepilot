# Account Survival OWASP Complete Matrix

**Status: hosted inventory complete; no additional exception is active.** Authenticated run `41336529191` compared the complete bundled and unbundled Dependency-Check 13.0.0 reports: 115 distinct Maven PURLs were represented in both modes, and unbundling produced 80 actual CVSS >= 8 PURL/CVE associations. Sixteen are already exact-scope approved; the remaining 64 are enumerated in [the inactive final owner proposal](ACCOUNT_SURVIVAL_OWASP_64_TUPLE_OWNER_PROPOSAL.md).

## Complete Inventory Outcome

| CVE(s) | Maven PURL(s) | Status | Evidence boundary |
| --- | --- | --- | --- |
| `CVE-2026-53914` | `kotlin-stdlib:1.9.25`, `kotlin-stdlib-jdk7:1.9.25` | Previously approved exact combinations | Original/private report evidence retained and reviewed. |
| `CVE-2026-53914` | `kotlin-stdlib-jdk8:1.9.25`, `kotlin-stdlib-common:1.9.25` | New combinations requiring owner review | Both are actual unbundled-scan associations. |
| `CVE-2026-59270` | `spring-security-core:6.5.11`, `spring-security-web:6.5.11` | Previously approved exact combinations | Original/private report evidence retained and reviewed. |
| `CVE-2026-59270` | `spring-security-config:6.5.11`, `spring-security-crypto:6.5.11` | New combinations requiring owner review | Both are actual unbundled-scan associations. |
| `CVE-2026-47884`, `CVE-2026-47890`, `CVE-2026-47891`, `CVE-2026-47892`, `CVE-2026-59313`, `CVE-2026-59283` | `spring-core:6.2.19`, `spring-tx:6.2.19` | Previously approved exact combinations | The 12 exact tuples are active only through `2026-11-30Z`. |
| Same six Spring Framework CVEs | `spring-aop`, `spring-aspects`, `spring-beans`, `spring-context`, `spring-expression`, `spring-jcl`, `spring-jdbc`, `spring-orm`, `spring-web`, `spring-webmvc`, each at `6.2.19` | New combinations requiring owner review | 60 actual unbundled-scan associations. |

## Classification Rules

- **Previously approved exact combination:** the exact `(CVE, Maven PURL, version)` appears in the active 16-rule scope test.
- **New combination requiring remediation or owner review:** no active suppression matches it, even if a sibling artifact has a reviewed finding.
- **Non-applicable finding with sufficient evidence:** may only be assigned after report CPE evidence, vendor advisory, resolved JAR, and application prerequisite review are all recorded.
- **Unresolved or potentially exploitable finding:** any high-severity association not yet fully assessed or shown to be configuration-inapplicable.

No row in this document expands a suppression or accepts residual risk. The document must be overwritten by the diagnostic generator artifact after an authorized run.
