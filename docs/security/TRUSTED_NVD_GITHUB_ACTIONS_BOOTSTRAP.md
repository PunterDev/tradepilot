# Trusted NVD GitHub Actions Bootstrap

This bootstrap adds a protected NVD publisher and a credential-free pull request
scanner. It does not replace, enable, or modify the existing CircleCI control
repository, CircleCI trigger, OWASP approval process, or application behavior.

## Trust model

`publish-verified-nvd.yml` runs only from protected `master` and must use the
`trusted-nvd-publisher` GitHub environment. The environment will contain
`NVD_API_KEY` only after a separate owner decision. Its update-only command is
created by `trusted_nvd_archive.py` from fixed source and never checks out pull
request code. It builds Dependency-Check `12.2.2` data, requires `odc.mv.db`,
rejects ingestion failures, writes a 24-hour manifest, uploads the archive, and
creates a GitHub provenance attestation.

`trusted-pr-dependency-check.yml` is evaluated from the protected default
branch through `pull_request_target`. Its first job uses a read-only token only
while it identifies a successful scheduled publisher run, validates the GitHub
artifact digest and provenance, and safely extracts the archive. It then passes
the verified data only within the current workflow. The scanner job has no NVD
key, no token environment variables, and checks out the exact PR head with
`persist-credentials: false` only after data verification.

The scanner always uses `autoUpdate=false`, `failOnError=true`,
`failBuildOnCVSS=8`, and `failBuildOnUnusedSuppressionRule=true`. It requires
HTML, JSON, and SARIF reports and restores the actual scanner exit code after
uploading them.

## Preconditions

1. Keep the external CircleCI architecture unchanged. This bootstrap is not a
   CircleCI replacement.
2. Create GitHub environment `trusted-nvd-publisher`, restrict it to `master`,
   and require an owner reviewer if the plan supports required reviewers.
3. Add `NVD_API_KEY` only to that environment, never as a repository secret.
4. Permit the protected publisher schedule. Its first successful run is needed
   before a PR can receive a trusted Dependency-Check result.
5. Configure a GitHub Actions event policy that explicitly permits the narrowly
   hardened `pull_request_target` workflow, if required by the repository plan.

The publisher artifact retention is two days, but scanner acceptance is limited
to 24 hours. GitHub artifact digest validation and provenance attestation are
both required; neither alone is accepted as sufficient provenance.

## OWASP policy and bootstrap limitation

The bootstrap deliberately supplies no suppression file. The December
16-tuple candidate stays inactive. Current protected `master` has no approved
original-eight policy artifact, so a later, separately reviewed policy merge is
required before an accepted exception can affect this scanner. Until then,
findings remain blocking.

The first bootstrap merge cannot honestly require a passing NVD scan because
the trusted data producer does not exist yet. It requires a recorded owner
authorization for this one infrastructure bootstrap, all other applicable
checks, intact deployment safeguards, and a post-merge publisher/consumer
verification before any product PR becomes merge-eligible.
