import type { SupabaseClient } from "@supabase/supabase-js";

export type Measurement = Readonly<{
  id: string;
  measured_at: string;
  systolic: number;
  diastolic: number;
  pulse: number;
  notes: string | null;
  deleted_at: string | null;
}>;

export type MeasurementDraft = Readonly<{
  measuredAt: string;
  systolic: number;
  diastolic: number;
  pulse: number;
  notes: string | null;
}>;

export type DateFilter = Readonly<{ from: string; to: string }>;

export interface MeasurementRepository {
  list(filter: DateFilter): Promise<Measurement[]>;
  create(draft: MeasurementDraft): Promise<void>;
  softDelete(id: string): Promise<void>;
}

type MeasurementRow = Measurement & { user_id: string };

export function createSupabaseMeasurementRepository(
  client: SupabaseClient,
): MeasurementRepository {
  return {
    async list(filter) {
      let query = client
        .from("measurements")
        .select("id,measured_at,systolic,diastolic,pulse,notes,deleted_at")
        .is("deleted_at", null)
        .order("measured_at", { ascending: false });

      if (filter.from) query = query.gte("measured_at", localDayStartIso(filter.from));
      if (filter.to) query = query.lt("measured_at", localDayAfterIso(filter.to));

      const { data, error } = await query;
      if (error) throw new Error(error.message);
      return (data ?? []) as Measurement[];
    },

    async create(draft) {
      const { data: userData, error: userError } = await client.auth.getUser();
      if (userError || !userData.user) throw new Error("La sesión no es válida.");

      const row: Omit<MeasurementRow, "deleted_at"> = {
        id: crypto.randomUUID(),
        user_id: userData.user.id,
        measured_at: draft.measuredAt,
        systolic: draft.systolic,
        diastolic: draft.diastolic,
        pulse: draft.pulse,
        notes: draft.notes,
      };
      const { error } = await client.from("measurements").insert(row);
      if (error) throw new Error(error.message);
    },

    async softDelete(id) {
      const { error } = await client
        .from("measurements")
        .update({ deleted_at: new Date().toISOString() })
        .eq("id", id)
        .is("deleted_at", null);
      if (error) throw new Error(error.message);
    },
  };
}

export function localDayStartIso(date: string): string {
  return new Date(`${date}T00:00:00`).toISOString();
}

export function localDayAfterIso(date: string): string {
  const result = new Date(`${date}T00:00:00`);
  result.setDate(result.getDate() + 1);
  return result.toISOString();
}
