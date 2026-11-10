# CircleCI Trusted Context Isolation - Phase 3B

## Status and Evidence

This is a local-only design. It creates no CircleCI context, changes no
pipeline, and grants no credential access.

The hosted canary YAML is self-contained and does not invoke
`verify_trusted_circleci_route.py`. That Python verifier and its tests are
included as local regression evidence for the exact Policy A and Policy B
metadata contract; they are not a runtime dependency or an alternate path to
context access.

The owner reports that CircleCI pipeline 20 ran `config-source-canary` manually
and passed with configuration and checkout commit
`aa093cda9841af7bff9e17dfaa1b1057a6e00f42`. The local repository directly
verifies that this is the current `origin/master` commit. The remaining
pipeline metadata is **OWNER_ATTESTED** because this environment has no
CircleCI API credential.

| Field | Status | Evidence |
| --- | --- | --- |
| Integration source `github_app` | OWNER_ATTESTED | Sanitized pipeline 20 output. |
| Trigger type and event `api` | OWNER_ATTESTED | Manual CircleCI execution. |
| Config source | OWNER_ATTESTED | `PunterDev/tradepilot`, `.circleci/trusted-owasp-canary.yml`, `refs/heads/master`. |
| Config SHA, checkout revision, and Git HEAD match | OWNER_ATTESTED | All reported as `aa093cda9841af7bff9e17dfaa1b1057a6e00f42`. |
| Checkout branch is `master`; SSH is disabled | OWNER_ATTESTED | Sanitized pipeline 20 output. |
| Current protected master commit | DIRECTLY VERIFIED | `origin/master` is `aa093cda9841af7bff9e17dfaa1b1057a6e00f42`. |

The nine sanitized metadata lines from pipeline 20 are not available in this
worktree. Retain them later to independently verify the owner-attested rows;
do not invent them, request a new API credential, or treat their absence as a
successful direct verification. No secret value belongs in that evidence.

## Verified CircleCI Inputs

CircleCI documents these GitHub App values: `pipeline.config.repository.name`,
`pipeline.config.file_path`, `pipeline.config.ref`, and
`pipeline.config.sha`; `pipeline.git.*` describes the independently selected
checkout. `pipeline.trigger.type` supports `github_app` and `api`, while
`pipeline.event.name` supports `push`, `pull_request`, and `api`.

CircleCI documents that context expression restrictions can use pipeline
values other than `pipeline.trigger_parameters.*` and `pipeline.parameters.*`.
`job.ssh.enabled` is explicitly supported. If any referenced value is absent,
the context is unauthorized before job steps execute. Project, security-group,
and expression restrictions combine with logical AND. Security-group
restrictions are only available for CircleCI organizations of the documented
`github` type; this design therefore does **not** assume an owner-only group
exists or use one as a control.

Sources: [pipeline values](https://circleci.com/docs/guides/orchestrate/pipeline-variables/),
[contexts](https://circleci.com/docs/guides/server/security/contexts/), and
[expression-restriction support](https://support.circleci.com/en/articles/16968400-pipeline-values-you-can-use-in-context-expression-restrictions).

## Policy A: Synthetic Context Canary

`tradepilot-context-canary` contains exactly one synthetic value:
`TRADEPILOT_CANARY_MARKER`. It is not a secret, is never printed, and must
never be reused for NVD, GitHub, deployment, or production credentials.

Set a CircleCI **project restriction** for `PunterDev/tradepilot`. Add the
following expression restriction after replacing the literal with the exact
commit that contains the future dedicated canary configuration:

```
pipeline.git.repo_owner == "PunterDev"
and pipeline.git.repo_name == "tradepilot"
and pipeline.git.branch == "master"
and pipeline.integration_source == "github_app"
and pipeline.config.repository.name == "PunterDev/tradepilot"
and pipeline.config.file_path == ".circleci/trusted-context-isolation-canary.yml"
and pipeline.config.ref == "refs/heads/master"
and pipeline.config.sha == "<exact-canary-control-sha>"
and pipeline.trigger.type == "api"
and pipeline.event.name == "api"
and not job.ssh.enabled
```

This intentionally permits the known manual UI/API route only for the
non-sensitive canary. The canary job checks only that the marker is present;
it must not echo, hash, branch on, or otherwise reveal it. A context does not
become privileged because this test succeeds.

An optional security-group restriction may be added only after the owner
confirms that the CircleCI organization type and an actual group support it.
It is defense in depth, not a prerequisite assumed by this plan.

## Policy B: Future Privileged Contexts

Create separate contexts for the NVD API key and the GitHub approval-read
credential. They must never be attached to PR workflows and are not allowed
to use the Policy A manual route.

Each privileged context needs the project restriction above plus this exact
shape of expression, with the final protected control SHA substituted:

```
pipeline.git.repo_owner == "PunterDev"
and pipeline.git.repo_name == "tradepilot"
and pipeline.git.branch == "master"
and pipeline.integration_source == "github_app"
and pipeline.config.repository.name == "PunterDev/tradepilot"
and pipeline.config.file_path == ".circleci/trusted-owasp-control.yml"
and pipeline.config.ref == "refs/heads/master"
and pipeline.config.sha == "<exact-privileged-control-sha>"
and pipeline.trigger.type == "github_app"
and pipeline.event.name == "push"
and not job.ssh.enabled
```

The protected job code independently verifies the exact configuration SHA,
checkout SHA, checked-out Git HEAD, repository, `master` checkout, source
path/ref, authenticated owner approval, candidate hash, tuple scope, expiry,
and target PR head. The NVD refresh receives only `NVD_API_KEY`; the approval
validation receives only the read-only GitHub token; analysis receives neither.
No PR-controlled config, checkout, script, Maven configuration, or dependency
is executed by a credentialed job. Pull-request, `api`, `schedule`, webhook,
SSH rerun, missing metadata, and any mismatch fail closed. A future change to
allow pull-request events requires a separate trust-boundary review.

The existing historical eight-rule approval remains archival only. The
December sixteen-tuple candidate remains inactive with SHA-256
`852d8f4178e6cf264346aaf1eb236e50894120c239049258f39dd6b760ceb190` and
requires a fresh PR #7-bound approval before it can be used.

## Positive-Route Comparison

| Option | What it proves | What it cannot prove | Required change |
| --- | --- | --- | --- |
| A. Manual API with synthetic canary | The UI accepts the expression; CircleCI starts a master-sourced job only when the marker context restriction matches; marker is not exposed. | No credential isolation, no non-API trigger, no owner approval, and no privileged-route safety. | Add a dedicated inactive canary config to protected master, configure its GitHub App pipeline/checkouts from master, then create the synthetic context. This requires a separately authorized master merge because the current canary must remain unchanged. |
| B. GitHub App push event with privileged contexts | The final trusted route rejects API and binds config/checkouts to protected master. It can support an approved PR analysis without executing PR code in credentialed jobs. | It does not make a PR approval valid by itself; the validator and fresh authenticated approval remain required. | Merge and configure the reviewed trusted-control path on master, apply deployment safeguards, prove actual event metadata in a credential-free dry run, then separately authorize contexts and one controlled run. |

**Recommended order:** A first, because it grants no real secret and exercises
only one synthetic marker. B is the only viable future privileged route. Do
not use a manual pipeline parameter, a context name, or a branch name as
authorization for Policy B.

## Context-Denial Matrix

A negative control passes only when CircleCI reports **Unauthorized before any
job step executes**. Invalid YAML, missing checkout files, or script errors
are **INCONCLUSIVE**.

| Case | Policy A expected result | Policy B expected result |
| --- | --- | --- |
| Wrong project/repository owner/name | Unauthorized | Unauthorized |
| Wrong config repository/path/ref | Unauthorized | Unauthorized |
| Wrong control SHA | Unauthorized | Unauthorized |
| Wrong checkout branch/SHA | Unauthorized | Unauthorized |
| SSH rerun | Unauthorized | Unauthorized |
| Missing pipeline value | Unauthorized | Unauthorized |
| PR-controlled config or checkout | Unauthorized | Unauthorized |
| Manual API trigger | Starts only with the exact canary route | Unauthorized |
| GitHub App push | Unauthorized (wrong trigger) | May start only after exact trusted route and fresh approval; otherwise validator rejects before privileged action |
| GitHub App pull request | Unauthorized (wrong trigger) | Unauthorized pending a separate trust-boundary review |
| API-supplied configuration or arbitrary pipeline parameters | Unauthorized | Unauthorized |

For the Policy A positive test, collect sanitized metadata, the job-start
record, and proof that no command output contains the marker. For each
negative test, retain the CircleCI `Unauthorized` state and verify that no job
steps began. Do not treat a parser or runtime failure as context evidence.

## Required Changes and Owner Boundaries

The prepared `.circleci/trusted-context-isolation-canary.yml` is the separately
reviewed master-only change needed before the synthetic canary can run. It
contains no credentials, no OWASP operation, and a workflow disabled by default.
CircleCI Project Setup must then set both configuration source and checkout
source to `PunterDev/tradepilot` at `master`; a context expression cannot
repair an unsafe pipeline definition.

Before any privileged run, the owner must separately authorize all of:

1. A protected-master merge after Render and Vercel deployment safeguards are
   verified.
2. A credential-free GitHub App event dry run proving the actual metadata for
   the final control path.
3. Creation of each least-privilege context with its project and expression
   restrictions, using the final immutable control SHA.
4. A fresh authenticated approval bound to the final PR #7 head, the inactive
   December candidate hash, all sixteen tuples, the December 15, 2026 expiry,
   and the three configuration limitations.
5. One controlled privileged workflow execution, followed by review of its
   report and context-denial evidence.

No master merge is required for this local design itself. A separate master
merge is required for either executable positive experiment, because the
current master canary must not be modified in place. PR #12 demonstrated that
Vercel can deploy after a master merge despite its previously selected Ignored
Build Step. A provider-level, observed production safeguard for both Render and
Vercel is therefore a mandatory pre-merge gate; it cannot be inferred from
file-path or ignored-build settings.

## Existing Security Policy

This design does not activate any OWASP suppression. It preserves
`failBuildOnCVSS=8`, `failOnError=true`, unused-suppression rejection, exact
PURL/CVE matching, expiry validation, the PR #10 secretless scanners, and the
historical Gitleaks approval trail.
