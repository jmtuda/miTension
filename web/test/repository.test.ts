import { describe, expect, test, vi } from "vitest";
import { createSupabaseMeasurementRepository, localDayAfterIso, localDayStartIso } from "../src/data/measurements.ts";

function queryResult(data: unknown[] = []) {
  const query = {
    select: vi.fn(), is: vi.fn(), order: vi.fn(), gte: vi.fn(), lt: vi.fn(),
    update: vi.fn(), eq: vi.fn(), insert: vi.fn(), then: undefined as unknown,
  };
  for (const name of ["select", "is", "order", "gte", "lt", "update", "eq"] as const) query[name].mockReturnValue(query);
  query.insert.mockResolvedValue({ error: null });
  query.then = (resolve: (value: unknown) => void) => resolve({ data, error: null });
  return query;
}

describe("Supabase measurement repository", () => {
  test("lists only active rows, descending and within the inclusive local date interval", async () => {
    const query = queryResult();
    const client = { from: vi.fn(() => query), auth: { getUser: vi.fn() } };
    const repository = createSupabaseMeasurementRepository(client as never);
    await repository.list({ from: "2026-08-01", to: "2026-08-31" });
    expect(query.is).toHaveBeenCalledWith("deleted_at", null);
    expect(query.order).toHaveBeenCalledWith("measured_at", { ascending: false });
    expect(query.gte).toHaveBeenCalledWith("measured_at", localDayStartIso("2026-08-01"));
    expect(query.lt).toHaveBeenCalledWith("measured_at", localDayAfterIso("2026-08-31"));
  });

  test("creates confirmed averages for the authenticated owner", async () => {
    const query = queryResult();
    const client = { from: vi.fn(() => query), auth: { getUser: vi.fn().mockResolvedValue({ data: { user: { id: "owner" } }, error: null }) } };
    const repository = createSupabaseMeasurementRepository(client as never);
    await repository.create({ measuredAt: "2026-08-31T10:00:00Z", systolic: 121, diastolic: 81, pulse: 62, notes: null });
    expect(query.insert).toHaveBeenCalledWith(expect.objectContaining({ user_id: "owner", systolic: 121, diastolic: 81, pulse: 62 }));
  });

  test("soft-deletes and never issues a physical delete", async () => {
    const query = queryResult();
    const client = { from: vi.fn(() => query), auth: { getUser: vi.fn() } };
    await createSupabaseMeasurementRepository(client as never).softDelete("measurement-id");
    expect(query.update).toHaveBeenCalledWith({ deleted_at: expect.stringMatching(/Z$/) });
    expect(query.eq).toHaveBeenCalledWith("id", "measurement-id");
    expect(query.is).toHaveBeenCalledWith("deleted_at", null);
  });
});
