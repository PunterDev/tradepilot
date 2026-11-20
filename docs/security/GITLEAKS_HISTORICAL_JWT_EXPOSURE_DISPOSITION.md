# Historical JWT Exposure Disposition

**Status:** Owner-approved exact-fingerprint disposition. This document is
secret-free and preserves the historical exposure record.

## Owner-Approved Disposition

On November 8, 2026, the sole repository owner recorded an explicit approval
for exact-fingerprint exclusions covering the four findings in this document.
The approval separately identifies the two synthetic fixtures and accepts the
residual risk for the two historical exposed-key findings after confirming key
rotation, successful production redeployment/authentication, the
`HISTORICAL_KEY_MISMATCH` result, and controlled
`HISTORICAL_KEY_REJECTION_CONFIRMED` result.

The active `.gitleaksignore` is limited to those four exact fingerprints. This
approval does not authorize broad rules, file or directory exclusions, removal
of full-history scanning, suppression of future findings, or any OWASP policy
change. This exposure record and the inactive review artifacts remain retained.
The approval source is the owner-supplied release decision; this document does
not represent it as an independently signed or authenticated external record.

## Scope and Evidence

The historical JWT fallback was introduced in commit
`3dbd3bfe262835cc1080ae30f4f2634977482dcc`, remained in the repository
through `1036d813`, and was removed in `11f0da307ad0033a7e0665c93fab4bef11d51769`.
The repository owner supplied verified Render deployment evidence that code
containing the fallback was deployed between May 9 and June 7, 2026.

On November 8, 2026, the owner confirmed that production `JWT_SECRET` was
replaced and that Render deployment `dep-dboa3ds9v7es73emlh7g` completed
successfully. Application startup and an authenticated user login succeeded
after that deployment. These are owner-supplied production facts; this local
review did not inspect production secret values or a secret-manager audit log.

Also on November 8, 2026, the owner ran the local verifier with a freshly
issued low-privilege production access token. It returned
`HISTORICAL_KEY_MISMATCH`, demonstrating that the historical fallback cannot
verify that token's HS256 signature. The JWT and both signing secrets remained
terminal/process-memory inputs only and were not recorded by the verifier or
this review.

The current application configuration requires `JWT_SECRET` from the runtime
environment and has no literal fallback. Startup rejects a missing, malformed,
or insufficiently strong secret.

## Finding Disposition

| Fingerprint | Classification | Disposition |
| --- | --- | --- |
| `5f17bd651789341fb661c10518d269e506be0877:backend/src/test/java/com/tradepilot/AuthHardeningTests.java:generic-api-key:47` | Synthetic test fixture; test-only path and identifier | Eligible for a narrowly scoped historical fingerprint exception after explicit owner acceptance. |
| `5f17bd651789341fb661c10518d269e506be0877:backend/src/test/java/com/tradepilot/AuthHardeningTests.java:generic-api-key:184` | Synthetic negative-test fixture; test-only path and identifier | Eligible for a narrowly scoped historical fingerprint exception after explicit owner acceptance. |
| `11f0da307ad0033a7e0665c93fab4bef11d51769:backend/src/test/java/com/tradepilot/RuntimeSmokeTests.java:generic-api-key:51` | Genuine historical exposed-key finding; matches the historical fallback family | Owner-approved exact fingerprint exception after rotation and rejection verification; residual risk retained in this record. |
| `3dbd3bfe262835cc1080ae30f4f2634977482dcc:backend/src/test/resources/application-test.yml:generic-api-key:13` | Genuine historical exposed-key finding; matches the historical fallback family | Owner-approved exact fingerprint exception after rotation and rejection verification; residual risk retained in this record. |

## Rotation and Remaining Uncertainty

The owner-provided rotation, successful deployment, login, and cryptographic
mismatch establish that the fresh verified token was not signed by the
historical fallback. Neither the old nor new secret was read or compared, so
this review makes no claim about secret contents beyond that signature result.

Production rejection of a token signed with the historical key has not been
verified. Existing tests cover invalid-token handling, but they do not provide
evidence that a historical production-signed token was rejected after the
November 8 rotation. Retain this as a post-rotation verification item using a
controlled, non-production token fixture or an authorized security test.

## Guardrails

- Full-history Gitleaks scanning remains required and blocking.
- Default Gitleaks rules remain enabled.
- The inactive candidate contains only the two verified synthetic-fixture
  fingerprints; it does not include either historical exposed-key finding.
- Activation was authorized only for the four exact fingerprints documented
  here. Any additional fingerprint still requires explicit owner acceptance
  and a separate reviewed change.

## Final Four-Fingerprint Review Proposal

`docs/security/candidates/gitleaksignore-final-four-review.proposed` is an
inactive review artifact. It separates the two synthetic test fixtures from
the two historical exposed-key findings involving the now-rotated key. The
latter are not false positives. Their active exact-fingerprint disposition is
recorded above; the review artifact remains preserved as historical evidence of
the separate risk review.

## Local Rotation Check

Run the uncommitted local verifier from the repository root only when an owner
has a newly issued, low-privilege production JWT available in their terminal:

```sh
python3 scripts/security/verify_historical_jwt_rotation.py
```

It prompts for the token without echoing it, makes no network request, reads
the historical fallback directly from Git into process memory, and writes only
one disposition to standard output. `HISTORICAL_KEY_MISMATCH` verifies that the
historical fallback cannot validate that fresh token. It does not prove that
all historical tokens are rejected, so retain the controlled old-token check
as a separate outstanding item.

### Controlled Production Rejection Check

Only after an explicit owner approval for the two read-only production requests,
run this from a local terminal with a freshly issued low-privilege JWT:

```sh
TRADEPILOT_HISTORICAL_JWT_REJECTION_APPROVED=yes \
  python3 scripts/security/verify_historical_jwt_rejection.py
```

The utility calls only `GET /api/auth/me`. It first requires the fresh token to
return HTTP 200, then preserves its header and claim segments exactly while
re-signing the JWT with the historical fallback. It confirms rejection only
when the re-signed token returns HTTP 401 with `INVALID_OR_EXPIRED_TOKEN`.
Any malformed, expired, unsupported, unauthorized, or ambiguous result is
`INCONCLUSIVE`. Without the approval variable it makes no request and returns
`NOT_APPROVED`. Neither request body nor either token is written to disk.
Safe diagnostic codes are written only to standard error. In particular,
`HISTORICAL_TOKEN_ERROR_CODE_MISMATCH` distinguishes an unexpected structured
401 response from `RESPONSE_FORMAT_UNEXPECTED`; `REQUEST_TIMEOUT` and
`NETWORK_REQUEST_FAILED` do not include raw network errors.
