import { describe, expect, test } from "vitest";
import type { Measurement } from "../src/data/measurements.ts";
import { CSV_HEADER, escapeCsv, formatIsoWithOffset, measurementsToCsv, UTF8_BOM } from "../src/lib/csv.ts";

const measurement: Measurement = {
  id: "a", measured_at: "2026-08-31T10:00:00.000Z", systolic: 121,
  diastolic: 81, pulse: 62, notes: "Línea 1; \"bien\"\nLínea 2", deleted_at: null,
};

describe("CSV export", () => {
  test("uses BOM, exact header, semicolons, explicit offset and correct escaping", () => {
    const csv = measurementsToCsv([measurement], () => -120);
    expect(csv.startsWith(`${UTF8_BOM}${CSV_HEADER}\r\n`)).toBe(true);
    expect(csv).toContain('2026-08-31T12:00:00+02:00;121;81;62;"Línea 1; ""bien""\nLínea 2"');
    expect(csv.endsWith("\r\n")).toBe(true);
  });

  test("escapes only fields that require it", () => {
    expect(escapeCsv("simple")).toBe("simple");
    expect(escapeCsv("a;b")).toBe('"a;b"');
    expect(formatIsoWithOffset(new Date("2026-01-01T00:00:00Z"), () => 300)).toBe("2025-12-31T19:00:00-05:00");
  });
});
