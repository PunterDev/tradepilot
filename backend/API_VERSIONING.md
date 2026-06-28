# TradePilot API Versioning

Current product API version: `1.0`

Canonical launch prefix: `/api/v1`

Legacy compatibility prefix: `/api`

TradePilot uses URL-based API versioning. New frontend and client integrations should call `/api/v1/**`. Existing `/api/**` routes remain available during the launch transition and return deprecation headers where practical.

## Routing

The backend keeps controller mappings under the existing `/api/**` paths and uses a lightweight servlet filter to normalize `/api/v1/**` requests to those handlers. This keeps current controllers stable while making `/api/v1` the canonical public API prefix.

Every `/api/**` response includes:

```http
X-TradePilot-API-Version: 1.0
```

Legacy API responses may also include:

```http
Deprecation: true
Link: </api/v1>; rel="successor-version"
```

## Webhook Exception

Stripe webhook URLs are external callback URLs and remain stable at:

```text
/api/stripe/webhook
```

The versioning filter can also route `/api/v1/stripe/webhook`, but do not remove or rename the legacy Stripe webhook URL until the Stripe Dashboard configuration is intentionally changed.

## Audited Route Roots

| Legacy route root | Canonical route root |
| --- | --- |
| `/api/auth/**` | `/api/v1/auth/**` |
| `/api/accounts/**` | `/api/v1/accounts/**` |
| `/api/trades/**` | `/api/v1/trades/**` |
| `/api/stats/**` | `/api/v1/stats/**` |
| `/api/analytics/**` | `/api/v1/analytics/**` |
| `/api/journals/**` | `/api/v1/journals/**` |
| `/api/discipline/**` | `/api/v1/discipline/**` |
| `/api/sessions/review/**` | `/api/v1/sessions/review/**` |
| `/api/coach/**` | `/api/v1/coach/**` |
| `/api/ai/**` | `/api/v1/ai/**` |
| `/api/billing/**` | `/api/v1/billing/**` |
| `/api/contact` | `/api/v1/contact` |
| `/api/feature-usage/**` | `/api/v1/feature-usage/**` |
| `/api/admin/**` | `/api/v1/admin/**` |
| `/api/dev/**` | `/api/v1/dev/**` |
| `/api/stripe/webhook` | `/api/v1/stripe/webhook` optional; keep legacy configured |

## Future Versions

Future breaking API changes should use a new URL prefix such as `/api/v2`. Keep legacy prefixes available only for an intentional deprecation window.
