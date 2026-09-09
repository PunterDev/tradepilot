-- Tradovate Performance exports store offsetless broker-local timestamps.
-- Treat those timestamps as America/Chicago, classify in America/New_York,
-- and leave the displayed/persisted local timestamp untouched.
with parsed_performance_trades as (
    select
        id,
        case
            when split_part(external_trade_key, '|', 10)::timestamp <= split_part(external_trade_key, '|', 11)::timestamp
                then split_part(external_trade_key, '|', 10)::timestamp
            else split_part(external_trade_key, '|', 11)::timestamp
        end as entry_source_time
    from trades
    where external_trade_key like 'TRADOVATE_PERFORMANCE|%'
      and array_length(string_to_array(external_trade_key, '|'), 1) >= 11
      and split_part(external_trade_key, '|', 10) ~ '^[0-9]{1,2}/[0-9]{1,2}/[0-9]{4} [0-9]{1,2}:[0-9]{2}:[0-9]{2}$'
      and split_part(external_trade_key, '|', 11) ~ '^[0-9]{1,2}/[0-9]{1,2}/[0-9]{4} [0-9]{1,2}:[0-9]{2}:[0-9]{2}$'
),
classified_performance_trades as (
    select
        id,
        case
            when market_time >= time '18:00:00' or market_time < time '03:00:00' then 'ASIA'
            when market_time >= time '03:00:00' and market_time < time '09:30:00' then 'LONDON'
            when market_time >= time '09:30:00' and market_time < time '12:30:00' then 'NY_AM'
            when market_time >= time '12:30:00' and market_time < time '16:00:00' then 'NY_PM'
            else 'UNKNOWN'
        end as corrected_session
    from (
        select
            id,
            ((entry_source_time at time zone 'America/Chicago') at time zone 'America/New_York')::time as market_time
        from parsed_performance_trades
    ) market_times
)
update trades
set session = classified_performance_trades.corrected_session
from classified_performance_trades
where trades.id = classified_performance_trades.id
  and trades.session is distinct from classified_performance_trades.corrected_session;
