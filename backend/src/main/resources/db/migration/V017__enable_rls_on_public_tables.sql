do $$
declare
    tbl record;
begin
    for tbl in
        select schemaname, tablename
        from pg_tables
        where schemaname = 'public'
    loop
        execute format(
            'alter table %I.%I enable row level security',
            tbl.schemaname,
            tbl.tablename
        );
    end loop;
end
$$;
