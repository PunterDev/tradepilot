# Expanded Sixteen-Tuple OWASP Exception Renewal - December 2026

**Status:** PENDING OWNER APPROVAL. This document does not activate a
suppression, authorize a merge, or authorize deployment.

**Prepared on:** 2026-11-03
**Consolidated branch / prepared baseline:** `codex/security-consolidated` /
`666ce192bc8191586409fea2f2afe090637e5ce0`
**Candidate:**
`candidates/dependency-check-expanded-sixteen-dec15-candidate.xml`
**Candidate SHA-256:**
`852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190`
**XML expiry:** `2026-12-15`
**Required approval deadline:** `2026-12-15T23:59:59Z`

## Historical Scope Preservation

The active original-eight candidate and its authenticated approval remain
unchanged. The historical sixteen-tuple candidate with SHA-256
`fd40e63b366dc3ba04b4c30738cbef6c38dc1fc971b9d8e2cf264e791d203070`
and its 2026-11-30 expiry are retained as evidence only. Neither historical
approval authorizes this renewal.

The December candidate is not referenced by Maven or GitHub Actions. The
workflow remains configured with the original-eight candidate until a separate
exact-scope activation change passes approval validation and CI. A new approval
must bind to the new consolidated PR number and its exact head SHA; the PR #6
comment cannot authorize this renewal.

## Post-Reset Approval Sequence

1. Push the consolidated branch and open a **draft** PR targeting `master`.
   The lightweight `security-policy` job validates this pending template and
   candidate in proposal mode. Dependency-Check, Gitleaks, Semgrep, and the
   frontend audit are intentionally skipped while the PR remains draft; this
   is not a security pass or an activation, and the skipped jobs cannot satisfy
   a ready-for-review release gate.
2. Record the new PR number and its current head SHA in the owner comment
   below. The comment must be posted by the authenticated repository owner.
3. In one narrow activation commit, replace the pending template with an
   approved record that binds `pullRequest` and `reviewedPullRequestHeadSha` to that
   exact SHA, switch the workflow to this December candidate, and preserve all
   existing scan controls.
4. Mark the PR ready for review. The strict activation validator re-fetches
   the GitHub PR and comment, requires base branch `master`, requires the
   reviewed SHA to be the recorded PR head at approval time and an ancestor of
   the final activation head, and only then permits the full Dependency-Check
   scan.

The workflow listens only for PR `opened`, `synchronize`, `reopened`, and
`ready_for_review` events, plus protected `master`/`main` pushes. It uses a
PR-specific concurrency group with cancellation of superseded runs. A
draft-to-ready transition therefore starts one complete current-PR scan; later
pushes cancel only that PR's obsolete run.

Any candidate change, approval-record drift, PR-head change, or comment from a
different PR fails closed and requires a fresh approval/activation commit.

## Current Remediation Review

The public vendor-advisory review on 2026-11-03 found no safe, publicly
available Spring Boot 3.5 / Java 17 patch line for the reported Spring tuples:

- Spring Framework advisories list `6.2.20` as Enterprise Support Only; the
  public fix is on Framework `7.0.9`. Moving to Framework 7 is a Spring Boot 4
  migration and is not a compatible point upgrade for this release branch.
- Spring Security advisory CVE-2026-59270 lists `6.5.12` as Enterprise Support
  Only; public fixes are on Security 7.0.7 and 7.1.1. Those are likewise not a
  safe Boot 3.5 point upgrade.
- The Kotlin advisory's affected Maven package is
  `org.jetbrains.kotlin:kotlin-gradle-plugin`, fixed at `2.4.20-Beta1`; the
  reported `kotlin-stdlib` and `kotlin-stdlib-jdk7` artifacts are runtime
  compatibility artifacts and do not contain Kotlin Gradle build-cache code.

Sources: [Spring CVE-2026-47890 advisory](https://spring.io/security/cve-2026-47890/),
[Spring CVE-2026-59283 advisory](https://spring.io/security/cve-2026-59283/),
[Spring Security CVE-2026-59270 advisory](https://spring.io/security/cve-2026-59270/),
and [OSV GHSA-r937-wjx7-w2jp](https://osv.dev/vulnerability/GHSA-r937-wjx7-w2jp).

The source-level prerequisite assessment in
[`OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md`](OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md)
remains unchanged: no reviewed XSLT views, SSE fragments, WebFlux/Aalto/JAXB
decoder path, functional RouterFunction route, untrusted SimpleEvaluationContext
with compiler enabled, or embedded UnboundID LDAP listener exists in the
reviewed source/configuration. Production configuration is owner-attested with
the documented limitations below; it is not independently verified.

## Exact Pending Scope

The approval must cover exactly these tuples and no others:

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

## Required Owner Comment

The sole owner must post the following from their authenticated GitHub account
on the eventual security PR, replacing the PR number, reviewed head SHA, and
timestamp placeholders with the values from that newly created PR. The
validator requires the exact tuple lines, candidate hash, expiry, owner
association, three configuration limitations, a `master` PR base, and a GitHub
PR head SHA that exactly equals the reviewed commit.

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

Reviewed commit: <NEW_CONSOLIDATED_PR_HEAD_SHA>
Reviewed PR: #<NEW_CONSOLIDATED_PR_NUMBER> targeting master
Candidate SHA-256: 852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190
Exception expiry: 2026-12-15T23:59:59Z
Decision timestamp (UTC): <UTC timestamp>

I reviewed GitHub Actions run 41757589036, artifact 13288697637,
the report HTML SHA-256 3bcaeaf6affd93518e9217c329332eea232f4593fc3d1a4b8a082929af1dc272,
the original-eight approval, and docs/security/OWASP_FINAL_EIGHT_CVE_ASSESSMENT.md.
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

The eventual activation must preserve `failBuildOnCVSS=8`, `failOnError=true`,
unused-suppression rejection, exact package-version matching, NVD ingestion
verification, report upload, Gitleaks, Semgrep, and the independent frontend
audit. Any changed tuple, candidate hash, dependency version, runtime
prerequisite, or unrelated CVSS >= 8 finding must fail closed.
