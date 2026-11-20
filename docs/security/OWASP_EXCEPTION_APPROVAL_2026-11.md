# OWASP Exception Approval Record - November 2026

**Original-eight status:** APPROVED FOR EXACT-SCOPE ACTIVATION. The authenticated owner
authorization is recorded in PR #6 comment
[`6620183351`](https://github.com/PunterDev/tradepilot/pull/6#issuecomment-6620183351).
This record activates no broad exception and does not authorize a merge or deployment.

**Scope:** only the eight exact CVE/PURL/version tuples in
[`OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md`](OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md) and
the inactive candidate XML.
**Proposed expiry:** `2026-11-30T23:59:59Z`
**Activation status:** NOT ACTIVE

Any future approval must bind to candidate XML SHA-256
`f9b964c787c906e6ba780ce675f00a409021b5efd54785e0430cf35872bfa3eb`, reviewed
commit `5d6b05af6a2c7234ec3d666d8cabc3d45f020a35`, and the proposed expiry. A
changed candidate, dependency version, CVE, or reviewed configuration requires
a new review.

## Pending Sixteen-Tuple Amendment

Run `41757589036`, artifact `13288697637`, exposed eight additional exact
CVE/PURL/version associations that were not covered by the historical comment.
The report HTML obtained through authenticated GitHub access has SHA-256
`3bcaeaf6affd93518e9217c329332eea232f4593fc3d1a4b8a082929af1dc272`.

The original eight exceptions stay active and unchanged. The expanded candidate
at [`candidates/dependency-check-final-sixteen-candidate.xml`](candidates/dependency-check-final-sixteen-candidate.xml)
is **PENDING** and is not referenced by the workflow. Its pending approval
record is [`candidates/owasp-final-sixteen-amendment.template.json`](candidates/owasp-final-sixteen-amendment.template.json).
It is a fresh approval request based on activation baseline
`db2ace78d080b86bd1520c7b027398557235c933`, not an edit of comment `6620183351`.

The amendment adds only:

- CVE-2026-53914 | pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25
- CVE-2026-59270 | pkg:maven/org.springframework.security/spring-security-web@6.5.11
- CVE-2026-47884 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47890 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47891 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47892 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-59313 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-59283 | pkg:maven/org.springframework/spring-tx@6.2.19

The validator now accepts a CVE on multiple approved PURLs only when every
CVE/PURL pair is exact and complete. It rejects a third PURL for the same CVE,
a swapped CVE/PURL pair, missing or duplicate entries, changed versions,
expired entries, broad matchers, and comments whose tuple lines do not match
the full exact set.

No amended exception may be activated until the owner posts a new authenticated
comment binding the sixteen tuples, candidate SHA-256, and the unchanged expiry
`2026-11-30T23:59:59Z`.

## Governance Paths

The standard path uses distinct authenticated evidence for the repository owner,
security-risk owner, and production-configuration verifier. That remains the
preferred path where independent review is available or externally required.

For this sole-owner project, a narrowly documented solo-founder path is also
available. It permits one authenticated GitHub identity to supply those three
separate evidence roles only when all of the following are true:

1. The owner explicitly adopts the solo-founder policy in the PR #6 comment.
2. The comment says that the owner is the sole owner, developer, security-risk
   decision-maker, and administrator.
3. The comment explicitly says no independent security review occurred.
4. The owner has determined that no applicable external independent-review
   obligation exists. If that cannot be stated accurately, this path is not
   available.
5. The comment records the exact scope, evidence, expiry, temporary nature, and
   residual-risk acceptance below.

This is owner self-review. It is not, and must never be described as,
independent security assurance.

## Existing Evidence

| Requirement | Status | Evidence location |
| --- | --- | --- |
| Dependency-Check report for the reviewed current package | VERIFIED | GitHub Actions run `41737155331`, artifact `13279572975`; retained report SHA-256 is recorded in the assessment. |
| Historical eight-tuple evidence | VERIFIED AS OWNER-SUPPLIED | Untracked local evidence ZIP; do not commit it. |
| Reviewed source, Docker/runtime command, committed config, and dependency graph | VERIFIED | `OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md`. |
| Render October 31 production-profile/environment attestation | OWNER-ATTESTED | Production profile, no Environment Groups, no Secret Files, and no reviewed LDAP/SpEL variable names. No values recorded. |
| Live container filesystem and future hosted configuration drift | UNKNOWN OUTSIDE SOURCE REVIEW | Revalidate if deployment/configuration changes. |
| Candidate exact-scope XML | VERIFIED, INACTIVE | `candidates/dependency-check-final-eight-candidate.xml`. |
| Candidate policy tests | VERIFIED LOCALLY | `scripts/security/tests/test_validate_owasp_exception_candidates.py`. |
| Owner authorization | APPROVED | PR #6 comment `6620183351`, author `PunterDev`, association `OWNER`. |
| Security-risk acceptance | APPROVED | Same authenticated solo-founder comment; this is owner self-review, not independent review. |
| Production-configuration attestation | OWNER_ATTESTED_WITH_LIMITATIONS | Same comment cites the October 31 Render attestation and the three explicit limitations. |
| Activation validation and post-activation scan | PENDING | Must pass in the exact-scope activation change. |

## Exact PR #6 Comment Required for the Solo-Founder Path

The sole owner posted the following completed text as GitHub comment `6620183351`
on PR #6. It is retained as the exact machine-validated authorization scope.

```text
SOLO_FOUNDER_GOVERNANCE: ADOPT
SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER
NO_INDEPENDENT_SECURITY_REVIEW
EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED

OWNER_AUTHORIZATION: APPROVE
SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY
PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS
OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK
TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER

Reviewed commit: 5d6b05af6a2c7234ec3d666d8cabc3d45f020a35
Candidate SHA-256: f9b964c787c906e6ba780ce675f00a409021b5efd54785e0430cf35872bfa3eb
Exception expiry: 2026-11-30T23:59:59Z
Decision timestamp (UTC): 2026-11-04T01:40:18Z

I am the sole owner, developer, security-risk decision-maker, and administrator
of TradePilot Coach. I have reviewed the evidence in GitHub Actions run
41737155331, artifact 13279572975, the owner-supplied historical evidence, and
docs/security/OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md. No independent security
review occurred. I identified no applicable external independent-review
obligation. I accept the documented residual risk temporarily through the
expiry above. This is not a permanent vulnerability waiver and does not itself
activate a scanner suppression, authorize a merge, or authorize deployment.

The only approved candidate tuples are:
- CVE-2026-53914 | pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25
- CVE-2026-47884 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47890 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47891 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47892 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59313 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59283 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59270 | pkg:maven/org.springframework.security/spring-security-core@6.5.11

The October 31 owner-attested Render review confirmed production profile
`production`, no Environment Groups, no Secret Files, and no environment
variable names matching `SPRING_LDAP_EMBEDDED_*` or
`SPRING_EXPRESSION_COMPILER_MODE`. I understand this is owner-attested
configuration evidence, not an independently authenticated Render audit.
The attestation limitations are:
- LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED
- HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED
- FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED
```

The activation validator fetches the comment through the GitHub API, verifies
the commenter identity and that it belongs to PR #6, and checks the exact
commit, candidate hash, expiry, scope, owner authorization, risk acceptance,
configuration attestation, its three stated limitations, and solo-founder
disclosures. A local JSON record alone cannot authorize activation.

## Required Comment for the Pending Sixteen-Tuple Amendment

The owner must post a fresh authenticated PR #6 comment using this form. The
historical comment `6620183351` is not reusable for this expanded scope.

```text
SOLO_FOUNDER_GOVERNANCE: ADOPT
SOLE_OWNER_DEVELOPER_SECURITY_RISK_DECISION_MAKER
NO_INDEPENDENT_SECURITY_REVIEW
EXTERNAL_INDEPENDENT_REVIEW_OBLIGATIONS: NONE_IDENTIFIED

OWNER_AUTHORIZATION: APPROVE
SECURITY_RISK_ACCEPTANCE: ACCEPT_TEMPORARILY
PRODUCTION_CONFIGURATION_ATTESTATION: OWNER_ATTESTED_WITH_LIMITATIONS
OWNER_ACCEPTS_RESIDUAL_CONFIGURATION_RISK
TEMPORARY_EXCEPTION_NOT_PERMANENT_WAIVER

Reviewed commit: db2ace78d080b86bd1520c7b027398557235c933
Candidate SHA-256: fd40e63b366dc3ba04b4c30738cbef6c38dc1fc971b9d8e2cf264e791d203070
Exception expiry: 2026-11-30T23:59:59Z
Decision timestamp (UTC): <UTC timestamp>

I reviewed GitHub Actions run 41757589036, artifact 13288697637,
the report HTML SHA-256 3bcaeaf6affd93518e9217c329332eea232f4593fc3d1a4b8a082929af1dc272,
the historical original-eight approval, and docs/security/OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md.
No independent security review occurred. I identified no applicable external
independent-review obligation. I accept the documented residual risk temporarily
through the expiry above. This is not a permanent vulnerability waiver and does
not itself activate a scanner suppression, authorize a merge, or authorize deployment.

The only approved candidate tuples are:
- CVE-2026-53914 | pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25
- CVE-2026-47884 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47890 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47891 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-47892 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59313 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59283 | pkg:maven/org.springframework/spring-core@6.2.19
- CVE-2026-59270 | pkg:maven/org.springframework.security/spring-security-core@6.5.11
- CVE-2026-53914 | pkg:maven/org.jetbrains.kotlin/kotlin-stdlib-jdk7@1.9.25
- CVE-2026-59270 | pkg:maven/org.springframework.security/spring-security-web@6.5.11
- CVE-2026-47884 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47890 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47891 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-47892 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-59313 | pkg:maven/org.springframework/spring-tx@6.2.19
- CVE-2026-59283 | pkg:maven/org.springframework/spring-tx@6.2.19

The October 31 owner-attested Render review confirmed production profile
`production`, no Environment Groups, no Secret Files, and no environment
variable names matching `SPRING_LDAP_EMBEDDED_*` or
`SPRING_EXPRESSION_COMPILER_MODE`. I understand this is owner-attested
configuration evidence, not an independently authenticated Render audit.
The attestation limitations are:
- LIVE_CONTAINER_FILES_NOT_INDEPENDENTLY_VERIFIED
- HOSTED_JVM_PROPERTIES_NOT_INDEPENDENTLY_VERIFIED
- FUTURE_CONFIGURATION_DRIFT_NOT_INDEPENDENTLY_VERIFIED
```

## Non-Negotiable Controls

- `failBuildOnCVSS=8` and `failOnError=true` remain unchanged.
- Gitleaks, Semgrep, the independent frontend npm audit, NVD update checks,
  report upload, expiry validation, unused-suppression checks, and unrelated
  CVSS >= 8 negative controls remain required.
- No suppression is active until a separate exact-scope implementation wires
  the candidate into Dependency-Check and passes the activation validator.
- Any change in the eight tuples, their versions, a relevant runtime
  prerequisite, Render configuration source, or a new CVSS >= 8 finding revokes
  this proposal and requires a fresh review.
