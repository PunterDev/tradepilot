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
artifact digest, run/workflow/repository identities, provenance attestation,
manifest, and safe extraction limits. It copies the verifier from the protected
`github.sha` checkout, records its SHA-256, and passes that exact file with the
verified package. The scanner verifies that hash and repeats the manifest/run
binding before it checks out the exact PR head. The scanner job has no NVD key
or GitHub token environment variables and uses `persist-credentials: false`;
untrusted PR content cannot replace the verifier before verification finishes.

The scanner captures the resolved internal PR head, compares it with the event
payload when present, rechecks that identity immediately before checkout, and
the separate trusted check-publisher job rechecks it after the scan. It creates
the `trusted-dependency-check` check only on the exact scanned SHA. A changed,
closed, retargeted, or external PR cannot receive a successful authoritative
check. This is distinct from the base-branch-associated workflow result created
by `pull_request_target`.

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
6. Restrict manual publisher dispatch to protected `master`. The scanner rejects
   events that do not resolve to protected `master` before reading publisher
   metadata.
7. Configure the future required check by both context and GitHub Actions app
   identity after one hosted proof confirms the check is attached to the exact
   PR head. Do not treat a `pull_request_target` workflow result alone as that
   proof.

The publisher artifact retention is two days, but scanner acceptance is limited
to 24 hours. GitHub artifact digest validation, publisher-run identity, GitHub
provenance attestation, manifest SHA-256, and bounded duplicate-safe ZIP/TAR
extraction are all required; none alone is accepted as sufficient provenance.
The verifier rejects links, special files, duplicate entries, path traversal,
unexpected inventory, truncated content, oversized data, and excessive archive
expansion before creating the database directory.

Dependency-Check `12.2.2` receives the key only by the documented
`nvdApiKeyEnvironmentVariable` setting. The key is not placed in a Maven
command-line argument. A refresh must produce a non-trivial `odc.mv.db` and
fail without a complete update before an artifact can be published.

## Pinned Actions

The workflows pin third-party actions to reviewed upstream commits: checkout
v4.6.0 (`7a6d8a8234af8eb26422e24e3006232cccaa061b`), setup-java v4.9.1
(`cf277c60eb25467037889841efdb72551f06f6c3`), upload-artifact v4.6.2
(`ea165f8d65b6e75b540449e92b4886f43607fa02`), download-artifact v4.3.0
(`d3f86a106a0bac45b974a628896c90dbdf5c8093`), and
attest-build-provenance v2.4.0 (`e8998f949152b193b063cb0ec769d69d929409be`).

## Environment cadence

Use a `trusted-nvd-publisher` environment restricted to protected `master` and
containing the NVD key only. A required reviewer can protect manual refreshes,
but it would also queue every scheduled run. Start with the six-hour schedule
only if a single owner can approve that operational cadence; otherwise use a
daily scheduled refresh plus explicitly approved manual refreshes. In either
case, the 24-hour scanner freshness limit remains authoritative.

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
