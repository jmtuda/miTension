begin;

insert into auth.users (id) values
    ('11111111-1111-1111-1111-111111111111'),
    ('22222222-2222-2222-2222-222222222222');

set local role authenticated;
select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', true);

insert into public.measurements (
    id, measured_at, systolic, diastolic, pulse, notes
) values (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '2026-08-31T08:00:00Z',
    120,
    80,
    60,
    'owned by the authenticated user'
);

do $$
begin
    if not exists (
        select 1
        from public.measurements
        where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
          and user_id = '11111111-1111-1111-1111-111111111111'
    ) then
        raise exception 'authenticated user must be able to insert and read own measurement';
    end if;

    begin
        insert into public.measurements (
            id, user_id, measured_at, systolic, diastolic, pulse
        ) values (
            'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
            '22222222-2222-2222-2222-222222222222',
            now(),
            121,
            81,
            61
        );
        raise exception 'authenticated user must not create a measurement for another user';
    exception
        when insufficient_privilege then null;
    end;
end;
$$;

update public.measurements
set deleted_at = now()
where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

do $$
begin
    if not exists (
        select 1
        from public.measurements
        where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
          and deleted_at is not null
    ) then
        raise exception 'owner must be able to soft-delete own measurement';
    end if;

    begin
        update public.measurements
        set systolic = 121
        where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
        raise exception 'clinical fields must remain immutable';
    exception
        when raise_exception then
            if sqlerrm <> 'confirmed measurement fields are immutable' then
                raise;
            end if;
    end;

    begin
        update public.measurements
        set deleted_at = null
        where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
        raise exception 'deleted measurement must not be restorable';
    exception
        when raise_exception then
            if sqlerrm <> 'a deleted measurement cannot be restored or deleted again' then
                raise;
            end if;
    end;

    begin
        delete from public.measurements
        where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
        raise exception 'physical deletion must not be allowed';
    exception
        when insufficient_privilege then null;
    end;
end;
$$;

select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', true);

do $$
declare
    changed_rows bigint;
begin
    if exists (select 1 from public.measurements) then
        raise exception 'different authenticated user must not read another user measurement';
    end if;

    update public.measurements
    set deleted_at = now();
    get diagnostics changed_rows = row_count;

    if changed_rows <> 0 then
        raise exception 'different authenticated user must not update another user measurement';
    end if;
end;
$$;

reset role;
set local role anon;
select set_config('request.jwt.claim.sub', '', true);

do $$
begin
    begin
        perform 1 from public.measurements;
        raise exception 'anonymous role must not read measurements';
    exception
        when insufficient_privilege then null;
    end;

    begin
        insert into public.measurements (
            id, measured_at, systolic, diastolic, pulse
        ) values (
            'cccccccc-cccc-cccc-cccc-cccccccccccc',
            now(),
            120,
            80,
            60
        );
        raise exception 'anonymous role must not insert measurements';
    exception
        when insufficient_privilege then null;
    end;
end;
$$;

reset role;

do $$
begin
    if not exists (
        select 1
        from pg_class
        where oid = 'public.measurements'::regclass
          and relrowsecurity
    ) then
        raise exception 'row level security must remain enabled';
    end if;

    if (
        select count(*)
        from pg_policies
        where schemaname = 'public'
          and tablename = 'measurements'
    ) <> 3 then
        raise exception 'measurements must have exactly three owner policies';
    end if;
end;
$$;

rollback;
