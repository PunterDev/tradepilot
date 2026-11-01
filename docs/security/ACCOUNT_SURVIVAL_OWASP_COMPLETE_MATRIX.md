# Account Survival OWASP Complete Matrix

**Status: incomplete pending manual unsuppressed diagnostic run.** This checked-in file records only the 24 CVSS >= 8 associations known from prior reports and follow-up CI summaries. The manual [`owasp-complete-inventory`](../../.github/workflows/owasp-complete-inventory.yml) workflow is the authoritative generator for the complete current dependency graph matrix. Its JSON and generated Markdown artifact must be reviewed before any new exception decision.

## Known Associations Pending Complete Inventory

| CVE(s) | Maven PURL(s) | Status | Evidence boundary |
| --- | --- | --- | --- |
| `CVE-2026-53914` | `kotlin-stdlib:1.9.25`, `kotlin-stdlib-jdk7:1.9.25` | Previously approved exact combinations | Original/private report evidence retained and reviewed. |
| `CVE-2026-53914` | `kotlin-stdlib-jdk8:1.9.25` | New combination requiring owner review | Reported by run `41317447207`; full unsuppressed JSON not yet available locally. |
| `CVE-2026-59270` | `spring-security-core:6.5.11`, `spring-security-web:6.5.11` | Previously approved exact combinations | Original/private report evidence retained and reviewed. |
| `CVE-2026-59270` | `spring-security-config:6.5.11` | New combination requiring owner review | Reported by run `41317447207`; no exact PURL rule is active. |
| `CVE-2026-47884`, `CVE-2026-47890`, `CVE-2026-47891`, `CVE-2026-47892`, `CVE-2026-59313`, `CVE-2026-59283` | `spring-core:6.2.19`, `spring-tx:6.2.19` | Previously approved exact combinations | The 12 exact tuples are active only through `2026-11-30Z`. |
| Same six Spring Framework CVEs | `spring-aop:6.2.19` | New combinations requiring owner review | Reported by run `41317447207`; no exact PURL rule is active. |

## Classification Rules

- **Previously approved exact combination:** the exact `(CVE, Maven PURL, version)` appears in the active 16-rule scope test.
- **New combination requiring remediation or owner review:** no active suppression matches it, even if a sibling artifact has a reviewed finding.
- **Non-applicable finding with sufficient evidence:** may only be assigned after report CPE evidence, vendor advisory, resolved JAR, and application prerequisite review are all recorded.
- **Unresolved or potentially exploitable finding:** any high-severity association not yet fully assessed or shown to be configuration-inapplicable.

No row in this document expands a suppression or accepts residual risk. The document must be overwritten by the diagnostic generator artifact after an authorized run.
