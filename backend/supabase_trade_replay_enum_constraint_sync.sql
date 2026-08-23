-- AI Trade Replay enum constraint sync.
-- Safe to run repeatedly in Supabase/Postgres. Preserves existing replay data
-- and fails loudly if unknown legacy enum values are present.

DO $tradepilot$
DECLARE
    unexpected_insight_types text[];
BEGIN
    SELECT array_agg(DISTINCT insight_type ORDER BY insight_type)
    INTO unexpected_insight_types
    FROM trade_replay_insights
    WHERE insight_type IS NOT NULL
      AND insight_type NOT IN (
          'ENTRY',
          'PARTIAL_EXIT',
          'FINAL_EXIT',
          'MFE',
          'MAE',
          'STRONG_EXCURSION_CAPTURE',
          'GOOD_EXCURSION_CAPTURE',
          'MODERATE_EXCURSION_CAPTURE',
          'LARGE_PROFIT_GIVEBACK',
          'WINNER_TURNED_LOSER',
          'RISK_EFFICIENCY',
          'POST_EXIT_CONTINUATION',
          'EXIT_NEAR_END_OF_MOVE',
          'TARGET_REACHED_AFTER_EXIT',
          'STOP_LEVEL_TRADED_DURING_POSITION',
          'STOP_LEVEL_TRADED_AFTER_EXIT',
          'STOP_LEVEL_TRADED_BEFORE_ENTRY',
          'STOPPED_THEN_RECOVERED',
          'LOW_EXCURSION_CAPTURE',
          'EXTENDED_ENTRY',
          'OVERTRADING_CLUSTER',
          'OUTSIDE_TRADING_WINDOW',
          'DATA_QUALITY_WARNING'
      );

    IF unexpected_insight_types IS NOT NULL THEN
        RAISE EXCEPTION 'Unexpected trade_replay_insights.insight_type values: %', unexpected_insight_types;
    END IF;
END
$tradepilot$;

ALTER TABLE trade_replay_insights
    DROP CONSTRAINT IF EXISTS ck_trade_replay_insights_type;

ALTER TABLE trade_replay_insights
    ADD CONSTRAINT ck_trade_replay_insights_type
    CHECK (insight_type IN (
        'ENTRY',
        'PARTIAL_EXIT',
        'FINAL_EXIT',
        'MFE',
        'MAE',
        'STRONG_EXCURSION_CAPTURE',
        'GOOD_EXCURSION_CAPTURE',
        'MODERATE_EXCURSION_CAPTURE',
        'LARGE_PROFIT_GIVEBACK',
        'WINNER_TURNED_LOSER',
        'RISK_EFFICIENCY',
        'POST_EXIT_CONTINUATION',
        'EXIT_NEAR_END_OF_MOVE',
        'TARGET_REACHED_AFTER_EXIT',
        'STOP_LEVEL_TRADED_DURING_POSITION',
        'STOP_LEVEL_TRADED_AFTER_EXIT',
        'STOP_LEVEL_TRADED_BEFORE_ENTRY',
        'STOPPED_THEN_RECOVERED',
        'LOW_EXCURSION_CAPTURE',
        'EXTENDED_ENTRY',
        'OVERTRADING_CLUSTER',
        'OUTSIDE_TRADING_WINDOW',
        'DATA_QUALITY_WARNING'
    ));

DO $tradepilot$
DECLARE
    unexpected_replay_statuses text[];
BEGIN
    SELECT array_agg(DISTINCT status ORDER BY status)
    INTO unexpected_replay_statuses
    FROM trade_replays
    WHERE status IS NOT NULL
      AND status NOT IN (
          'PENDING',
          'WAITING_FOR_MARKET_DATA',
          'FETCHING_DATA',
          'ANALYZING',
          'READY',
          'PROVIDER_NOT_CONFIGURED',
          'UNSUPPORTED_INSTRUMENT',
          'CONTRACT_RESOLUTION_FAILED',
          'NOT_ENOUGH_DATA',
          'DEMO_REPLAY_UNAVAILABLE',
          'FAILED'
      );

    IF unexpected_replay_statuses IS NOT NULL THEN
        RAISE EXCEPTION 'Unexpected trade_replays.status values: %', unexpected_replay_statuses;
    END IF;
END
$tradepilot$;

ALTER TABLE trade_replays
    DROP CONSTRAINT IF EXISTS ck_trade_replays_status;

ALTER TABLE trade_replays
    ADD CONSTRAINT ck_trade_replays_status
    CHECK (status IN (
        'PENDING',
        'WAITING_FOR_MARKET_DATA',
        'FETCHING_DATA',
        'ANALYZING',
        'READY',
        'PROVIDER_NOT_CONFIGURED',
        'UNSUPPORTED_INSTRUMENT',
        'CONTRACT_RESOLUTION_FAILED',
        'NOT_ENOUGH_DATA',
        'DEMO_REPLAY_UNAVAILABLE',
        'FAILED'
    ));
