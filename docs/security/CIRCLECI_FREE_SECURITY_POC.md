# CircleCI Free Security Scanner Proof of Concept

## Status and scope

This is an **isolated proof of concept** on branch
`codex/circleci-security-poc`, published as draft
[PR #10](https://github.com/PunterDev/tradepilot/pull/10) against
`codex/security-consolidated`. The CircleCI GitHub App pull-request trigger is
configured for the POC. Hosted proposal-job execution and GitHub status
reporting remain verification steps; configuration alone is not evidence that
the trigger, metadata contract, or status integration worked. This POC does
not modify PR #7, activate the pending December sixteen-tuple candidate, or
replace the GitHub Actions release gate.

The configuration mirrors the current strict policy:

- Dependency-Check Maven plugin `12.2.2`, `failBuildOnCVSS=8`, and
  `failOnError=true`.
- The currently active original-eight exact-match suppression file and
  `failBuildOnUnusedSuppressionRule=true` for full scans.
- The existing custom approval validator; proposal mode uses the pending
  December sixteen-tuple candidate only to prove it is exact and inactive.
- Blocking Gitleaks, Semgrep, and `npm audit --audit-level=high` jobs.
- No broad suppression, CVSS threshold change, or error-tolerant scan path.

## Proposal versus activation

A GitHub App draft-PR event runs only the pending-candidate validator. It never
receives NVD credentials and never runs a vulnerability scan. A non-draft
GitHub App pull-request event runs `validate_security_activation.py` first.
That adapter fails unless all of the following are true:

1. A protected CircleCI secret named `TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN`
   exists.
2. CircleCI supplies an immutable `CIRCLE_SHA1`, a GitHub PR URL in
   `CIRCLE_PULL_REQUEST`, and project identity `PunterDev/tradepilot`.
3. The GitHub API reports a non-draft PR targeting `master` whose head is
   exactly `CIRCLE_SHA1`.
4. The repository's existing activation validator confirms the GitHub comment,
   candidate hash, PURL/CVE tuples, expiry, approval identity, and ancestry.

The current activation references are deliberately the active original-eight
scope. A future 16-tuple activation must be an explicit, reviewed source
change that updates **all** activation references together only after a fresh
owner approval. The historical PR #6 approval cannot be reused.

## CircleCI connection and remaining hosted-validation steps

1. The private `PunterDev/tradepilot` repository is connected through the
   CircleCI GitHub App, and its pull-request trigger is configured. The first
   hosted proposal-only execution on draft PR #10 must confirm that CircleCI
   can post a commit/PR status without receiving protected secrets.
2. Keep proposal mode on a GitHub App draft-PR trigger and activation on a
   separate `PR marked ready for review` trigger. Keep the default all-push
   trigger disabled to avoid duplicate spending. Verify on draft PR #10 that
   CircleCI exposes `CIRCLE_PULL_REQUEST`,
   `CIRCLE_SHA1`, `CIRCLE_PROJECT_USERNAME`, and `CIRCLE_PROJECT_REPONAME` as
   assumed by the gate. This is an integration prerequisite, not established
   by local tests.
3. Create a least-privilege GitHub App installation token (preferred) or
   fine-grained token, restricted to `PunterDev/tradepilot`, with only
   **Contents: read** (commit comparison) and **Pull requests: read** (PR plus
   issue-comment evidence). Metadata read is implicit for GitHub App tokens.
   Do **not** grant write permissions or store it as a project-level variable.
4. Create a `tradepilot-security-activation` context restricted to this
   project, the owner's CircleCI/GitHub security group, and the expression:

   ```text
   pipeline.event.name == "pull_request"
   and pipeline.event.github.pull_request.draft == false
   and pipeline.event.github.pull_request.base.ref == "master"
   and not job.ssh.enabled
   ```

   Put only `TRADEPILOT_GITHUB_APPROVAL_READ_TOKEN` and `NVD_API_KEY` in this
   restricted context. The activation-policy and Dependency-Check jobs are the
   only POC jobs that attach it; proposal, Gitleaks, Semgrep, and npm-audit are
   secretless. A missing or non-matching context restriction must make the job
   `Unauthorized`, not fall back to project variables.
5. Confirm the CircleCI organization type supports **security-group** context
   restrictions for the GitHub App pipeline. CircleCI documents that this
   requires a `github` organization and GitHub OAuth authorization for the
   restriction setup. If an owner-only group cannot be enforced, do not run
   this POC on pull requests containing untrusted code.
6. Do not put either secret in `.circleci/config.yml`.
7. Reconfirm the pinned Linux amd64 image/release digests immediately before
   adoption: Java `cimg/openjdk:17.0.13` is pinned to
   `sha256:402f246164077f437f8a19d586137038e83beb63d21bedbc89cd5d11ee20b3e6`,
   Node `cimg/node:22.15.1` to
   `sha256:2914f60f3ffaa8427a9038d5ade497d5e01e3baa974eb78d690c3b3d669c70f6`,
   and Semgrep `1.100.0` to
   `sha256:62c5c71d6da15096bdde79035782d30be8b89a6e2b5fb9b3228d37c3316fa9d4`.
   The committed Gitleaks `v8.18.4` Linux x64 archive SHA-256 is
   `ba6dbb656933921c775ee5a2d1c13a91046e7952e9d919f9bac4cec61d628e7d`.
   These are public release-integrity values, not credentials.
8. Make the CircleCI check required in GitHub branch protection only after the
   proposal and activation paths have both passed a controlled test.

CircleCI's GitHub integration/status behavior and availability of the listed
built-in variables for this private project remain **UNVERIFIED** until step 2.
CircleCI documents `CIRCLE_PULL_REQUEST`, `CIRCLE_SHA1`,
`CIRCLE_PROJECT_USERNAME`, and `CIRCLE_PROJECT_REPONAME` for GitHub projects;
its GitHub App pipeline values additionally document PR number, draft state,
base ref, and head SHA. The adapter intentionally fails closed if the runtime
values are absent or differ from the GitHub API response.

## Secret exposure rule

Do not configure `NVD_API_KEY` or the GitHub approval-read token at project
level. Project variables are available to jobs broadly; a pull request can
change CI configuration, so project variables cannot be relied upon to
separate proposal-only untrusted code from the activation path. The restricted
context above is mandatory. The POC is **not safe to publish** until its
project restriction, owner-group restriction, and expression restriction have
been verified in the CircleCI UI/API.

## NVD cache design

The Dependency-Check job restores a version/schema-specific prefix and runs
`update-only` before analysis. It rejects the known NVD import-error patterns.
Only after that command has completed does it save an immutable, per-build
cache with one-day retention; a later CVSS failure cannot prevent a completed
NVD database from being cached, and a failed update never reaches
`save_cache`. The CVSS command records its exact exit status, uploads its
report, and only then re-raises that status; report retention does not convert
a scanner failure into success. A per-build key is intentional here: NVD data is mutable and an
immutable cache keyed only by source revision would prevent a later successful
refresh from replacing its stale snapshot.

The first scan starts without a cache and may be slow or fail due to NVD rate
limits, download size, transient network failure, or CircleCI disk/cache quota.
Those are failures, not reasons to continue with stale or incomplete data.
Before adoption, record the measured data-directory size and confirm it fits
within the CircleCI Free plan's 2 GB-month storage allotment.

## Local validation performed

Run from this proof-of-concept worktree:

```bash
python3 -m unittest scripts/security/tests/test_validate_owasp_exception_candidates.py \
  scripts/circleci/tests/test_validate_security_activation.py
python3 scripts/security/validate-owasp-exception-candidates.py \
  --suppression docs/security/candidates/dependency-check-expanded-sixteen-dec15-candidate.xml \
  --approval-record docs/security/candidates/owasp-expanded-sixteen-dec15-approval.template.json \
  --scope expanded-sixteen-dec15 --mode proposal
ruby -e 'require "yaml"; YAML.load_file(".circleci/config.yml"); puts "YAML OK"'
git diff --check
```

These checks do not validate CircleCI executor image availability, GitHub App
status reporting, token permissions, NVD throughput, or the hosted cache.

## Free-tier planning

CircleCI documents 30,000 Free-plan credits/month, 2 GB-month storage, and
1 GB network transfer. It advertises up to 6,000 minutes on small Docker
resources; medium Docker is documented as 10 credits/minute. Treat a first
Dependency-Check NVD hydration as the principal risk: reserve 30-60 minutes
and measure its actual resource use before declaring a sustainable cadence.
As a planning estimate only, a five-job full scan taking 15-35 aggregate
minutes consumes roughly 150-350 medium-Docker credits; cached runs should be
measured rather than assumed. The 2 GB cache allowance may be the harder
constraint than compute time.

## Equivalence decision

CircleCI **can potentially be an equivalent release gate**, but only after the
private GitHub connection, PR status posting, built-in PR metadata, read-only
approval-token permissions, pinned-image digests, first NVD hydration, cache
integrity, and fail-closed negative controls are verified in a controlled run.
Until then, this is a NO-GO replacement: it is a prepared alternative, not a
proven hosted security authority.
