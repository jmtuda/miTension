import { describe, expect, test } from "vitest";
import type { Measurement } from "../src/data/measurements.ts";
import { measurementsToPdf } from "../src/lib/pdf.ts";

const measurement: Measurement = {
  id: "internal-id-must-not-appear",
  measured_at: "2026-08-31T10:00:00.000Z",
  systolic: 121,
  diastolic: 81,
  pulse: 62,
  notes: "Después del paseo",
  deleted_at: null,
};

describe("PDF report", () => {
  test("creates a valid, transient report with visible measurement fields but no metadata", () => {
    const bytes = measurementsToPdf([measurement], new Date("2026-08-31T12:00:00Z"));
    const source = new TextDecoder("latin1").decode(bytes);
    expect(source.startsWith("%PDF-1.4")).toBe(true);
    expect(source).toContain("Informe de mediciones");
    expect(source).toContain("121");
    expect(source).toContain("Después del paseo");
    expect(source).not.toContain(measurement.id);
    expect(source).not.toContain("deleted_at");
    expect(source).toMatch(/startxref\n\d+\n%%EOF$/);
  });

  test("paginates long filtered reports", () => {
    const many = Array.from({ length: 80 }, (_, index) => ({ ...measurement, id: String(index) }));
    const source = new TextDecoder("latin1").decode(measurementsToPdf(many));
    const count = Number(source.match(/\/Type \/Pages \/Kids \[[^\]]+] \/Count (\d+)/)?.[1]);
    expect(count).toBeGreaterThan(1);
    expect(source).toContain(`Pagina ${count} de ${count}`);
  });
});
