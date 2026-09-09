alter table if exists ai_pre_analyses
    alter column structure_quality_score type double precision
    using structure_quality_score::double precision;
