# TradePilot Production PostgreSQL Backup and Recovery

## Status

**NOT ACTIVE.** Scheduled automation is guarded by
`TRADEPILOT_PRODUCTION_BACKUP_ENABLED=true` and must remain disabled until the
repository owner completes the production activation checklist. A one-time,
owner-approved encrypted production backup has been structurally verified with
an isolated **APPLICATION-SCOPE RECOVERY**. That evidence does not enable the
schedule or establish a complete Supabase recovery procedure.

## Architecture

Daily GitHub Actions execution on `master` uses PostgreSQL 17 `pg_dump` to make
a custom-format (`-Fc`) logical backup. A long-lived read-only PostgreSQL
transaction exports one snapshot. The dump and every `public` table count each
import that exact snapshot before the holder commits. The runner verifies the
custom archive with `pg_restore --list`, encrypts it with an `age` public
recipient, and uploads the encrypted archive plus an encrypted table-count
manifest. The non-sensitive metadata binds both encrypted object SHA-256 values
to the exact archive; it never contains table names or counts. The workflow
verifies object size and checksum metadata after upload.

```text
Supabase PostgreSQL --TLS verify-full--> GitHub Actions runner
  --pg_dump custom archive--> ephemeral runner storage
  --age public-key encryption--> encrypted .dump.age + encrypted completeness manifest
  --private S3-compatible upload + independent checksum verification--> off-site storage
```

The proposed default prefix is `tradepilot/production/postgres/`. Objects must
be private. The backup storage credential may list, put, get, and delete only
that prefix. It must not grant public ACL, bucket policy, account, or unrelated
object access.

## Included and excluded data

The logical dump is a full database dump visible to the configured PostgreSQL
role. It is not limited with `--schema`. A stock PostgreSQL 17 image is not a
complete Supabase recovery target because the dump can include Supabase-managed
schemas and extensions such as `supabase_vault`. For this environment, the
supported isolated verification is therefore an **APPLICATION-SCOPE RESTORE**:
only `public` is restored after the archive's `public` DDL is checked to prove
it has no reference to a managed Supabase schema or extension. The process
fails before target mutation when that proof fails.

The application-owned `public` scope includes rows, indexes, constraints,
sequences, functions, RLS configuration/policies, and Flyway history; it
protects user accounts, trading accounts, trades, imports, journals, billing
records, and Account Survival records. It does **not** recover Supabase
platform schemas (`auth`, `extensions`, `graphql`, `graphql_public`,
`pgbouncer`, `realtime`, `storage`, `supabase_migrations`, and `vault`), their
extension definitions, cluster roles, platform configuration, or service
behavior. A complete Supabase-platform restore requires a verified
Supabase-compatible recovery target and separate approval.

It does not include PostgreSQL cluster roles, Supabase-managed platform
configuration, Auth/Storage objects outside the dumped database, Stripe data,
Render/Vercel configuration, GitHub data, or other third-party service data.
This is a logical backup, not Supabase point-in-time recovery.

## Schedule and retention

- Schedule: `17 9 * * *` UTC, every day at 09:17 UTC.
- America/Chicago: 03:17 CST or 04:17 CDT.
- Retention: 21 days, exceeding the 14-day minimum recovery target.
- Cleanup occurs only after a newly encrypted upload is verified. The script
  retains at least two encrypted artifacts and never removes the newest one.
- The first controlled production backup uses
  `TRADEPILOT_BACKUP_RETENTION_MODE=disabled`, so it performs no deletion.
- Upload refuses an existing artifact or metadata key rather than overwriting
  it, and the initial run separately downloads the encrypted object and checks
  it against its metadata checksum.
- A failed backup cannot enter cleanup because upload verification has not run.
- A freshness check flags an actionable warning if no encrypted backup is newer
  than 36 hours; the post-upload check makes a successful run fail if freshness
  remains unacceptable.

Provider lifecycle policies may be added as a defense-in-depth limit only after
they are verified not to undercut the 21-day window. The script remains the
authoritative cleanup mechanism because it is conditional on a successful,
verified current upload.

## Required GitHub configuration

Create a protected GitHub Environment named `production-backup`, restrict its
approval/access to the repository owner and named recovery operators, and place
all production secrets there. The workflow is master-only and also requires the
repository variable `TRADEPILOT_PRODUCTION_BACKUP_ENABLED=true`. Keep that
variable unset or false until every activation item below is approved.

Secrets, names only:

- `TRADEPILOT_BACKUP_PGHOST`
- `TRADEPILOT_BACKUP_PGPORT`
- `TRADEPILOT_BACKUP_PGDATABASE`
- `TRADEPILOT_BACKUP_PGUSER`
- `TRADEPILOT_BACKUP_PGPASSWORD`
- `TRADEPILOT_BACKUP_PGSSLROOTCERT_BASE64`
- `TRADEPILOT_BACKUP_AGE_RECIPIENT`
- `TRADEPILOT_BACKUP_S3_BUCKET`
- `TRADEPILOT_BACKUP_S3_ACCESS_KEY_ID`
- `TRADEPILOT_BACKUP_S3_SECRET_ACCESS_KEY`

Repository variables, names only:

- `TRADEPILOT_PRODUCTION_BACKUP_ENABLED`
- `TRADEPILOT_BACKUP_S3_PREFIX`
- `TRADEPILOT_BACKUP_S3_REGION`
- `TRADEPILOT_BACKUP_S3_ENDPOINT_URL`

Use the Supabase direct database connection or its session pooler only when
Supabase confirms the chosen endpoint supports a full logical dump. Do not use
transaction pooling for `pg_dump`. The workflow requires `verify-full` TLS and
an approved CA root certificate; it does not accept `sslmode=require` as a
substitute for certificate/hostname verification.

Before activation, run the separately approval-gated
`scripts/backup/production-connectivity-preflight.sh` exactly once against the
configured endpoint. It requires
`TRADEPILOT_PRODUCTION_CONNECTIVITY_PREFLIGHT_APPROVED=yes`, PostgreSQL 17,
`verify-full` TLS, a read-only `SELECT 1`, and a temporary schema-only custom
archive validated by `pg_restore --list`; it deletes that archive on exit. The
default connection timeout is 15 seconds. Supabase currently recommends a
direct connection for `pg_dump`; its session pooler is the IPv4 fallback, while
transaction pooling is unsuitable. The approved preflight is the evidence that
the configured endpoint works for this workflow.

## Encryption and key custody

The workflow receives only an `age` **public recipient**. It cannot decrypt
backups. Store the private identity outside GitHub and the repository, in an
approved encrypted password manager or hardware-backed/offline recovery
location. Document two authorized recovery operators and a key-loss escalation
path before enabling production automation. Rotate the storage credential and
age recipient through a controlled change; retain the former private key until
all backups encrypted to it expire.

Manual custody action: save a second protected copy of
`~/.config/tradepilot-backup/age-identity.txt` as an encrypted password-manager
attachment or on encrypted offline media. Do not paste its contents into chat,
issue trackers, repositories, GitHub secrets, or shell history. An authorized
recovery operator must verify access to that second copy before activation.

Cloudflare R2 is a compatible low-cost option because it supports S3 APIs and
does not charge egress in its standard model; a private existing S3 bucket is
also acceptable. No provider, bucket, account, subscription, or billable
resource was created by this change. Estimate storage after the first synthetic
and approved production dump: approximately compressed logical-dump size times
21, plus object requests. At current unknown production size, monthly cost is
**UNVERIFIED**. Confirm provider pricing and lifecycle behavior before approval.

## Restore runbook

1. Obtain explicit incident/recovery approval and provision a new, isolated,
   disposable PostgreSQL 17 target. Never restore over Supabase production.
   The local verification launcher uses Docker `--network none`, publishes no
   database port, and reaches PostgreSQL only through `docker exec`; it never
   copies the age identity or R2 credentials into the container.
2. Download the encrypted `.dump.age` and its `.metadata.json` from the private
   backup prefix using a recovery operator credential.
3. Provide the approved private `age` identity file only to the isolated
   recovery environment.
4. Set `TRADEPILOT_RESTORE_CONFIRM_DISPOSABLE=yes`,
   `TRADEPILOT_RESTORE_SCOPE=application-public`, and the target connection
   variables. Run `scripts/backup/restore-verify.sh`. This labels and verifies
   the result as an APPLICATION-SCOPE RESTORE; it is not a complete Supabase
   restore.
5. The script verifies encrypted SHA-256, decrypts into temporary storage,
   validates the archive, refuses a target containing any user-managed schema,
   relation, function, type, or non-default extension, validates that the
   `public` archive DDL has no managed-schema/extension dependencies, verifies
   the encrypted completeness manifest is bound to the archive, restores
   with `pg_restore --schema=public --strict-names --exit-on-error`, validates
   the recovered public structural counts, exact sequence names and ownership
   (including identity sequences), non-advancing restored sequence state,
   required application tables and Flyway history, all encrypted snapshot-bound
   `public` table counts, and all foreign keys are validated. Missing, altered,
   or mismatched completeness manifests fail the application-scope verification
   closed before it can be reported complete.
6. Capture the restore result in the recovery record. The existing isolated
   APPLICATION-SCOPE verification completed in four seconds after its local
   inputs were ready. It excludes approval, credential retrieval, encrypted
   download, target provisioning, investigation, and application recovery, so
   it is **not** an incident RTO. Measure and approve full incident RTO only
   from a representative recovery exercise.

Never pass the private key, password, or connection URL as a command argument.
Use environment injection from an approved secret manager and remove temporary
plaintext files at process exit, including failures.

## Monitoring and incident handling

GitHub Actions is the first monitoring signal. **PENDING ACTIVATION:** the
repository owner must configure a GitHub Actions failure-alert route to the
named primary recovery operator and a named backup operator, with an escalation
path when no acknowledged response is recorded. Record the names and test the
route without exposing contact details in this repository. The workflow emits a warning before a stale
backup attempt and fails if the verified object remains older than 36 hours
after upload. Operators must review failed jobs immediately, verify the last
known good object and checksum, rotate potentially compromised storage
credentials, and reassess the encryption recipient if compromise is suspected.

The proposed RPO is **up to 24 hours**, subject to successful daily runs. It is
not zero-loss protection. PITR is unavailable in this design unless separately
enabled and verified at the provider level.

## Synthetic verification and production activation

The scripts have shell syntax, retention-selection, checksum/archive, and
cleanup tests. `scripts/backup/tests/synthetic-backup-restore.sh` runs the
actual dump/encrypt/decrypt/restore flow against disposable local PostgreSQL
17 containers. Its `TRADEPILOT_BACKUP_SYNTHETIC_TEST_MODE=true` permits
`sslmode=disable` only for loopback test containers; the production workflow
does not set it and remains locked to `verify-full`. A full synthetic
PostgreSQL 17 backup/encrypt/decrypt/restore test is required in CI or an
approved isolated environment before activation.
Production verification is deliberately blocked until a private destination,
age recipient, database connection details, restore owner, and access-controlled
recovery target are approved. The protected recovery identity must have a
separately retrievable protected copy; verify its location and access through
the recovery operator process without reading, printing, uploading, or storing
the identity in this repository.

### Activation checklist

1. Owner approves a private S3-compatible bucket and its expected cost.
2. Bucket public access is disabled; prefix-limited least-privilege credentials
   are created and recovery read access is limited to named operators.
3. Two authorized recovery operators and private-key custody are recorded.
4. Supabase confirms the selected direct/session endpoint supports `pg_dump`
   with `verify-full` TLS; transaction pooling is not selected.
5. Secrets/variables are configured in protected `production-backup`.
6. A synthetic end-to-end restore passes.
7. A current-format production backup, including its encrypted snapshot-bound
   completeness manifest, is created to the approved private destination and
   restored only into an approved, access-controlled non-production target.
8. The owner records retention, backup size/cost, RPO acceptance, measured RTO,
   the tested alert route, and the restore result. Only then set the enable
   variable to `true`.

## Decision

**NO-GO for production backup activation** until every activation item is
complete and a current-format real encrypted production backup has passed
secured restore verification. The prior successful recovery remains valid
structural evidence, but it predates the snapshot-bound completeness manifest
and cannot prove that newer control. The design itself makes no production
change.
