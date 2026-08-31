create extension if not exists pgcrypto;

create table public.measurements (
    id uuid primary key,
    measured_at timestamptz not null,
    systolic smallint not null check (systolic > 0),
    diastolic smallint not null check (diastolic > 0),
    pulse smallint not null check (pulse > 0),
    notes text null check (notes is null or char_length(notes) <= 1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz null,
    constraint measurements_diastolic_below_systolic
        check (diastolic < systolic)
);

create index measurements_history_idx
    on public.measurements (measured_at desc, id)
    where deleted_at is null;

create index measurements_sync_idx
    on public.measurements (updated_at, id);

create function public.protect_measurement_and_set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.id is distinct from old.id
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

create trigger measurements_protect_update
before update on public.measurements
for each row
execute function public.protect_measurement_and_set_updated_at();

alter table public.measurements enable row level security;
