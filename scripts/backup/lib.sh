#!/usr/bin/env bash
# Shared helpers for the encrypted PostgreSQL backup scripts.

set -Eeuo pipefail

backup_die() {
  printf 'backup error: %s\n' "$*" >&2
  exit 1
}

backup_require_command() {
  command -v "$1" >/dev/null 2>&1 || backup_die "required command is unavailable: $1"
}

backup_require_var() {
  local name="$1"
  [[ -n "${!name:-}" ]] || backup_die "required environment variable is unset: $name"
}

backup_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

backup_decode_base64() {
  local value="$1"
  local destination="$2"
  if base64 --decode >/dev/null 2>&1 <<<"$value"; then
    base64 --decode <<<"$value" >"$destination"
  else
    base64 -D <<<"$value" >"$destination"
  fi
}

backup_s3_args() {
  BACKUP_S3_ARGS=()
  if [[ -n "${TRADEPILOT_BACKUP_S3_ENDPOINT_URL:-}" ]]; then
    BACKUP_S3_ARGS+=(--endpoint-url "$TRADEPILOT_BACKUP_S3_ENDPOINT_URL")
  fi
  BACKUP_S3_ARGS+=(--region "${TRADEPILOT_BACKUP_S3_REGION:-auto}")
}

backup_object_prefix() {
  local prefix="${TRADEPILOT_BACKUP_S3_PREFIX:-tradepilot/production/postgres}"
  printf '%s' "${prefix#/}" | sed 's:/*$::'
}
