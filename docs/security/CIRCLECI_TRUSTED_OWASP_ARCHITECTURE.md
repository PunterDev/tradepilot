# Trusted CircleCI OWASP Architecture (Inactive Design)

This design preserves the existing draft-PR proposal, Gitleaks, Semgrep, and
frontend-audit jobs unchanged. No pull-request job receives an NVD key or a
GitHub approval-read token.

This bootstrap is deliberately credential-free and inactive on its own. A
protected execution fails closed until `master` also contains the separately
reviewed December candidate, its approval record, and the existing OWASP policy
validator. Those materials are not part of this bootstrap PR and the bootstrap
does not activate an exception.

```text
PR (all states): secretless proposal + Gitleaks + Semgrep + npm audit

protected master control pipeline (manual, parameterized):
  approval-read context -> validate PR #, exact head, owner comment, candidate
  NVD-refresh context  -> update-only, bundle + checksum/freshness manifest
  no context            -> fetch exact approved SHA, verify bundle, scan
  approval-read context -> revalidate PR/head/comment and accept exact result
```

The control pipeline must be committed to protected `master` before contexts
exist. It accepts a PR number, exact head SHA, and candidate SHA only as
inputs; the first approval job validates all three through GitHub before any
analysis starts, and a second protected approval job repeats that validation
before it accepts the analysis result. The December candidate remains inactive until its owner approval binds
PR #7, final PR head, SHA-256
`852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190`, all
sixteen tuples, expiry `2026-12-15T23:59:59Z`, and the three recorded
configuration limitations.

## Context requirements

Create neither context until owner authorization. When authorized, each context
must be project-restricted to `PunterDev/tradepilot`, contain only its named
credential, deny SSH reruns, and require the protected master route. Do not
place either value in project environment variables. Verify whether GitHub App
supports an additional security-group restriction before relying on it.

## Bundle contract

The NVD refresh job produces a gzip archive and `manifest.json` containing
only scanner version, UTC creation time, archive SHA-256, repository, and the
protected control commit. The analysis job rejects a missing, malformed,
future, older-than-24-hours, corrupt, unsafe, provenance-mismatched, or
checksum-mismatched archive. It must run Dependency-Check 12.2.2 with
`autoUpdate=false`, `failOnError=true`, `failBuildOnCVSS=8`, and unused-rule
failure enabled. It must never perform an unauthenticated update fallback.

The analysis checks out the exact approved commit only after copying the
master-controlled candidate into a temporary location. It receives neither
credential, so malicious PR source cannot read an NVD key or GitHub token.
It can still make its own build fail or produce malformed reports; the final
protected revalidation accepts only a successful result bound to the unchanged
approval target. Report provenance and any future report-signing requirement
remain operational review items rather than assumptions.

## Integration order

1. Create a separate, credential-free infrastructure PR that adds the trusted
   master control configuration and its tests. PR #7 cannot bootstrap its own
   pre-merge privileged scan.
2. Review and merge that control configuration to protected master with all
   activation parameters disabled and no contexts attached.
3. Configure and test a non-sensitive restricted-context canary.
4. Port the verified secretless POC jobs into PR #7 without changing its
   approved dependency scope, then bind a new owner decision to PR #7's final
   head and the December candidate.
5. Run one manual protected control pipeline against that exact PR #7 head.
   Do not use the historical
   eight-rule approval for the December scope.

## Deployment safeguard before merge

Render auto-deploys `master`, and Vercel production deployments also originate
from `master`. Publishing this branch as a draft PR does not update `master`.
Before any merge, the release owner must separately authorize production
deployment handling, verify the current Render and Vercel deployment settings,
pause or otherwise protect both deployment paths using their approved controls,
record that protection, merge, verify the resulting commit, and restore the
settings only after the release decision. This bootstrap changes no application
runtime behavior, but it must not be merged as a shortcut around that safeguard.

## Required canary and owner gates

Before a real credential is added, run a canary context with a non-sensitive
marker and prove that it is unavailable to every PR workflow, SSH rerun, and
API-configured pipeline. Then verify CircleCI's project restriction and the
supported expression restriction for the protected-master route. The owner
must separately authorize creation of each context, the December sixteen-tuple
approval bound to PR #7's final head, and the first protected manual pipeline.
