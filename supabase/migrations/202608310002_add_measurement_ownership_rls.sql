begin;

alter table public.measurements
    add column user_id uuid;

do $$
declare
    existing_user_count bigint;
    sole_user_id uuid;
begin
    if exists (select 1 from public.measurements) then
        select count(*), min(id::text)::uuid
        into existing_user_count, sole_user_id
        from auth.users;

        if existing_user_count <> 1 then
            raise exception
                'cannot assign existing measurements: expected exactly one auth user, found %',
                existing_user_count;
        end if;

        update public.measurements
        set user_id = sole_user_id;
    end if;
end;
$$;

alter table public.measurements
    alter column user_id set default auth.uid(),
    alter column user_id set not null,
    add constraint measurements_user_id_fkey
        foreign key (user_id) references auth.users (id) on delete restrict;

create index measurements_user_history_idx
    on public.measurements (user_id, measured_at desc, id)
    where deleted_at is null;

create index measurements_user_sync_idx
    on public.measurements (user_id, updated_at, id);

create or replace function public.protect_measurement_and_set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.id is distinct from old.id
        or new.user_id is distinct from old.user_id
        or new.measured_at is distinct from old.measured_at
        or new.systolic is distinct from old.systolic
        or new.diastolic is distinct from old.diastolic
        or new.pulse is distinct from old.pulse
        or new.notes is distinct from old.notes
        or new.created_at is distinct from old.created_at then
        raise exception 'confirmed measurement fields are immutable';
    end if;

    if old.deleted_at is not null and new.deleted_at is distinct from old.deleted_at then
        raise exception 'a deleted measurement cannot be restored or deleted again';
    end if;

    new.updated_at = clock_timestamp();
    return new;
end;
$$;

revoke all on table public.measurements from anon, authenticated;
grant select, insert, update on table public.measurements to authenticated;

create policy measurements_select_own
on public.measurements
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy measurements_insert_own
on public.measurements
for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy measurements_update_own
on public.measurements
for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

commit;
