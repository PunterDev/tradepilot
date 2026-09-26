#!/usr/bin/env bash
set -euo pipefail

mode="runtime"
if [[ "${1:-}" == "--template" ]]; then
  mode="template"
fi

failures=0

present() {
  local name="$1"
  if [[ -n "${!name:-}" ]]; then
    printf 'PRESENT  %s\n' "$name"
  else
    printf 'MISSING  %s\n' "$name"
    failures=1
  fi
}

present_any() {
  local label="$1"
  shift
  local name
  for name in "$@"; do
    if [[ -n "${!name:-}" ]]; then
      printf 'PRESENT  %s (via %s)\n' "$label" "$name"
      return
    fi
  done
  printf 'MISSING  %s\n' "$label"
  failures=1
}

template_value() {
  local file="$1" name="$2" expected="${3:-}"
  local line
  line="$(rg -m1 "^${name}=" "$file" || true)"
  if [[ -z "$line" ]]; then
    printf 'MISSING  %s in %s\n' "$name" "$file"
    failures=1
  elif [[ -n "$expected" && "$line" != "${name}=${expected}" ]]; then
    printf 'INVALID  %s in %s\n' "$name" "$file"
    failures=1
  else
    printf 'PRESENT  %s in %s\n' "$name" "$file"
  fi
}

if [[ "$mode" == "template" ]]; then
  printf '%s\n' 'Checking V1 production templates (no secret values read)'
  for name in DB_URL DB_USERNAME DB_PASSWORD SPRING_PROFILES_ACTIVE JWT_SECRET APP_ALLOWED_ORIGINS APP_FRONTEND_URL RATE_LIMIT_STORE APP_ADMIN_ACCESS_PROTECTION STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET STRIPE_PRO_MONTHLY_PRICE_ID STRIPE_PRO_YEARLY_PRICE_ID RESEND_API_KEY EMAIL_FROM CONTACT_EMAIL; do
    template_value backend/.env.example "$name"
  done
  template_value backend/.env.example V2_FEATURES_ENABLED false
  template_value backend/.env.example V3_FEATURES_ENABLED false
  template_value backend/.env.example MARKET_DATA_PROVIDER disabled
  template_value frontend/.env.example VITE_API_BASE_URL
  template_value frontend/.env.example VITE_V2_FEATURES_ENABLED false
  template_value frontend/.env.example VITE_V3_FEATURES_ENABLED false
else
  printf '%s\n' 'V1 backend startup'
  present_any DATABASE_URL DB_URL DATABASE_URL
  present_any DATABASE_USERNAME DB_USERNAME DATABASE_USERNAME
  present_any DATABASE_PASSWORD DB_PASSWORD DATABASE_PASSWORD
  present SPRING_PROFILES_ACTIVE
  present JWT_SECRET
  present APP_ALLOWED_ORIGINS
  present APP_FRONTEND_URL
  present RATE_LIMIT_STORE
  present APP_ADMIN_ACCESS_PROTECTION

  printf '%s\n' 'Stripe Live'
  present STRIPE_SECRET_KEY
  present STRIPE_WEBHOOK_SECRET
  present STRIPE_PRO_MONTHLY_PRICE_ID
  present STRIPE_PRO_YEARLY_PRICE_ID

  printf '%s\n' 'Email delivery'
  present RESEND_API_KEY
  present EMAIL_FROM
  present CONTACT_EMAIL

  printf '%s\n' 'Frontend build'
  present VITE_API_BASE_URL
  present VITE_V2_FEATURES_ENABLED
  present VITE_V3_FEATURES_ENABLED

  v2="${V2_FEATURES_ENABLED:-false}"
  v3="${V3_FEATURES_ENABLED:-false}"
  printf 'V2 configuration: %s\n' "$v2"
  if [[ "$v2" == "true" ]]; then
    present QDRANT_URL
    present QDRANT_API_KEY
  else
    printf '%s\n' 'SKIPPED  Qdrant and Pre-Analysis beta configuration (V2 disabled)'
  fi
  printf 'V3 configuration: %s\n' "$v3"
  if [[ "$v3" == "true" ]]; then
    present DATABENTO_API_KEY
  else
    printf '%s\n' 'SKIPPED  Databento and Trade Replay configuration (V3 disabled)'
  fi
fi

if [[ "$failures" -ne 0 ]]; then
  printf '%s\n' 'FAIL: required V1 production configuration is incomplete.' >&2
  exit 1
fi
printf '%s\n' 'PASS: required V1 production configuration is present.'
