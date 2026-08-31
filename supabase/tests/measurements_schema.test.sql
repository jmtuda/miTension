begin;

do $$
declare
    measurement_id uuid := gen_random_uuid();
    original_updated_at timestamptz;
begin
    insert into public.measurements (
        id, measured_at, systolic, diastolic, pulse, notes
    ) values (
        measurement_id, '2026-08-31T07:00:00Z', 121, 79, 61, null
    );

    select updated_at into original_updated_at
    from public.measurements
    where id = measurement_id;

    perform pg_sleep(0.01);
    update public.measurements
    set deleted_at = now()
    where id = measurement_id;

    if not exists (
        select 1 from public.measurements
        where id = measurement_id
          and deleted_at is not null
          and updated_at > original_updated_at
    ) then
        raise exception 'soft delete must update deleted_at and updated_at';
    end if;
end;
$$;

do $$
begin
    begin
        insert into public.measurements (
            id, measured_at, systolic, diastolic, pulse
        ) values (
            gen_random_uuid(), now(), 80, 80, 60
        );
        raise exception 'equal systolic and diastolic should fail';
    exception
        when check_violation then null;
    end;

    begin
        insert into public.measurements (
            id, measured_at, systolic, diastolic, pulse
        ) values (
            gen_random_uuid(), now(), 120, 80, 0
        );
        raise exception 'non-positive pulse should fail';
    exception
        when check_violation then null;
    end;

    begin
        insert into public.measurements (
            id, measured_at, systolic, diastolic, pulse, notes
        ) values (
            gen_random_uuid(), now(), 120, 80, 60, repeat('x', 1001)
        );
        raise exception 'notes longer than 1000 characters should fail';
    exception
        when check_violation then null;
    end;
end;
$$;

do $$
declare
    measurement_id uuid := gen_random_uuid();
begin
    insert into public.measurements (
        id, measured_at, systolic, diastolic, pulse
    ) values (
        measurement_id, now(), 120, 80, 60
    );

    begin
        update public.measurements
        set systolic = 121
        where id = measurement_id;
        raise exception 'clinical values should be immutable';
    exception
        when raise_exception then
            if sqlerrm <> 'confirmed measurement fields are immutable' then
                raise;
            end if;
    end;
end;
$$;

do $$
begin
    if not exists (
        select 1
        from pg_class
        where oid = 'public.measurements'::regclass
          and relrowsecurity
    ) then
        raise exception 'row level security must be enabled';
    end if;

    if (select count(*) from pg_policies where schemaname = 'public' and tablename = 'measurements') <> 0 then
        raise exception 'P0.4 must not define the P0.5 access policies';
    end if;
end;
$$;

rollback;
