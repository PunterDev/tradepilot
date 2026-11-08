# Account Survival Full-Stack Launcher Gitleaks Disposition

**Status:** Owner-approved exact-fingerprint disposition. This is a
secret-free review record. The two fingerprints in this document are active
only as part of the six-entry exact scope in `.gitleaksignore`.

## Owner Approval Provenance

On November 8, 2026, the sole repository owner supplied an explicit approval
covering only the two exact fingerprints in the table below. The owner
acknowledged the local E2E-fixture evidence, the absence of an identified
production deployment path, the value's distinction from the rotated
production fallback, and the remaining uncertainty about use outside the
reviewed repository and deployment evidence.

This is an owner-supplied risk decision. It is not represented as an
independently signed or authenticated external approval. It does not authorize
broad exclusions, changes to the four previously approved fingerprints,
reduced Git-history coverage, altered Gitleaks rules, or changes to OWASP
Dependency-Check policy.

## Reviewed Findings

| Fingerprint | Historical ref | File | Rule | Classification |
| --- | --- | --- | --- | --- |
| `81d6b5cb75910fb9e895233b9f7cdd9aab6e59c5:scripts/run-account-survival-fullstack.sh:generic-api-key:43` | `codex/account-survival-v1-consolidated` | `scripts/run-account-survival-fullstack.sh` | `generic-api-key` | Local Account Survival full-stack test fixture, pending approval. |
| `1ac1ef61943a2b2185025bc17b0291c9e4cd352d:scripts/run-account-survival-fullstack.sh:generic-api-key:43` | `feature/v1-account-survival-coach` | `scripts/run-account-survival-fullstack.sh` | `generic-api-key` | The same local test fixture in a parallel historical commit, pending approval. |

The two matched values are byte-identical. A value-free comparison confirmed
that they do not match the previously reviewed historical production JWT
fallback.

## Repository Evidence

The launcher was introduced as the Account Survival full-stack acceptance
test. Static review of the historical script confirms that it:

- sets `SPRING_PROFILES_ACTIVE=e2e`;
- enables development-only authentication and tools;
- starts a local backend on a loopback test port and runs Playwright;
- uses a local PostgreSQL test setup; and
- disables production integrations including market data and Search Console.

The launcher is absent from current `master`. It is present only in the
Account Survival feature/integration refs searched during this review. No
Docker build input or production build configuration references it. Current
master's backend Docker build copies only `backend/pom.xml` and `backend/src`,
then packages and runs the backend JAR.

The owner has separately attested that Render deploys `master` with `backend`
as the service root. This review treats that deployment-path statement as
owner-attested configuration evidence, not an independent inspection of
Render's deployment history or runtime filesystem.

## Residual Risk and Limits

The evidence supports classifying both findings as synthetic local-test
material rather than production credentials. It does not prove that the value
was never copied outside repository history or used in an unobserved external
environment. No production secret, JWT, or matched value was read, stored, or
recorded during this review.

Because the value is detected by Gitleaks and the two historical commits are
distinct, this disposition authorizes only the two exact fingerprint entries
listed above. The existing four approved historical fingerprints are
unchanged.

## Required Owner Decision

The owner approval above is limited to the two exact fingerprints in
`docs/security/candidates/gitleaksignore-account-survival-fullstack.proposed`.
That proposal remains a retained review artifact; it is not read by CI.

## Guardrails

- Gitleaks continues to scan complete available Git history and exits nonzero
  on every unapproved finding.
- Default Gitleaks rules remain enabled.
- This record does not alter the existing four active exact fingerprints.
- This record does not change OWASP Dependency-Check policy or CircleCI
  activation controls.
