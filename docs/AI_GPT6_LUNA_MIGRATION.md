# GPT-6 Luna AI Migration

## Scope and default

TradePilot analysis generation defaults to `gpt-6-luna` through the OpenAI
Responses API. The backend does not expose a model selector to clients.
`OPENAI_MODEL` is the server-side rollout and rollback control.

`OpenAiResponsesClient` is the single generation gateway. It is used by:

- AI trade review and asynchronous trade analysis
- chart review
- daily and weekly coach snapshots
- session review
- Trading DNA
- Performance Timeline narrative
- private Pre-Market Analysis

Embeddings remain on the OpenAI embeddings endpoint because they are not
generation requests.

## Request and reliability controls

Responses requests use `store: false`, a developer instruction, JSON output,
and `reasoning.effort=low` by default. The supported Responses model is
currently `gpt-6-luna`; its accepted efforts are `none`, `low`, `medium`,
`high`, `xhigh`, and `max`. Any other Responses model/effort combination fails
before a provider request. The client has at most three
provider attempts, classifies failures, rejects incomplete/refused/blank
output, and records every provider attempt. DTO parsing remains at each feature
boundary; malformed output is rejected rather than turned into a fabricated
review.

Each request is capped at `OPENAI_MAX_OUTPUT_TOKENS=4096` by default. The
server rejects values outside the model's `1..128000` limit before contacting
the provider. This cap applies to the explicit legacy rollback request as well;
it does not change user credits or feature credit prices.

`OPENAI_LEGACY_CHAT_COMPLETIONS_ENABLED=true` is an emergency server-side
rollback only. It restores the existing Chat Completions payload for a
compatible configured model. It is false by default and must not be enabled
from the browser.

## Grounding and fallbacks

Prompts require conclusions to be based on visible chart evidence or explicitly
supplied trade, journal, and deterministic inputs. They instruct the model not
to infer hidden fills, emotions, rule violations, or profitability. Provider
fallbacks are neutral and disclose that no AI conclusion was produced; they do
not invent behavioral findings.

## Billing and observability

Chart review now enforces and records the existing `AI_TRADE_REVIEW` credit
before/after a successful generation, matching the product's existing review
credit policy. Existing cached trade-analysis behavior is unchanged.

Each provider attempt records the user, feature, configured model, input,
cached-input, output, reasoning, and total tokens, estimated cost, latency,
attempt number, success, and sanitized error category. Image input is included
in the provider's input-token total rather than estimated separately. `V032`
adds the additional usage columns without changing historical totals.

Set the selected model's published rates through:

- `OPENAI_INPUT_COST_PER_MILLION_TOKENS`
- `OPENAI_CACHED_INPUT_COST_PER_MILLION_TOKENS`
- `OPENAI_OUTPUT_COST_PER_MILLION_TOKENS`

For the configured GPT-6 Luna baseline, the defaults are `$0.10` input,
`$0.01` cached input, and `$0.50` output per million tokens. Reasoning tokens
are already included in `output_tokens`, so they are recorded but never priced
a second time. The configured values must be reviewed whenever the model
changes.

For illustration only, 100 one-credit requests that each use 5,000 uncached
input tokens and 1,000 output tokens estimate to `$0.10`; 100 requests at
50,000 input and 10,000 output tokens estimate to `$1.00`. These are token-use
examples, not a claim about typical customer behavior or product margin.
They also exclude provider modes, regional processing, cache-write charges, and
very-large-prompt premiums that are not separately exposed in the current usage
payload; these must be considered before enabling those modes.

## Release checklist

1. Set `OPENAI_MODEL=gpt-6-luna` and verify the three current published pricing values.
2. Keep `OPENAI_LEGACY_CHAT_COMPLETIONS_ENABLED=false`.
3. Apply `V032` through normal Flyway deployment and confirm metrics persist.
4. Exercise one non-sensitive test account flow for each enabled feature and
   confirm no output exposes server configuration or credentials.
5. Watch AI error rate, retry count, latency, token/cost metrics, and credit
   use during the rollout.
6. `V032` follows the Account Survival `V031` migration and therefore must not
   be released first while Flyway out-of-order migrations remain disabled.
7. If a provider regression requires rollback, set a compatible model and the
   explicit legacy flag, deploy normally, and preserve the observed metrics for
   follow-up. Do not change client contracts or disable credit checks.
