import type { Measurement } from "../data/measurements.ts";

export const CSV_HEADER = "fecha_hora;sistolica;diastolica;pulso;notas";
export const UTF8_BOM = "\uFEFF";

export function measurementsToCsv(
  measurements: readonly Measurement[],
  timeZoneOffset = currentOffset,
): string {
  const rows = measurements.map((measurement) =>
    [
      escapeCsv(formatIsoWithOffset(new Date(measurement.measured_at), timeZoneOffset)),
      measurement.systolic,
      measurement.diastolic,
      measurement.pulse,
      escapeCsv(measurement.notes ?? ""),
    ].join(";"),
  );
  return `${UTF8_BOM}${[CSV_HEADER, ...rows].join("\r\n")}\r\n`;
}

export function escapeCsv(value: string): string {
  if (!/[;"\r\n]/.test(value)) return value;
  return `"${value.replaceAll('"', '""')}"`;
}

export function formatIsoWithOffset(
  instant: Date,
  getOffset: (date: Date) => number = currentOffset,
): string {
  const offsetMinutes = getOffset(instant);
  const local = new Date(instant.getTime() - offsetMinutes * 60_000);
  const dateTime = local.toISOString().slice(0, 19);
  const sign = offsetMinutes <= 0 ? "+" : "-";
  const absolute = Math.abs(offsetMinutes);
  const hours = String(Math.floor(absolute / 60)).padStart(2, "0");
  const minutes = String(absolute % 60).padStart(2, "0");
  return `${dateTime}${sign}${hours}:${minutes}`;
}

function currentOffset(date: Date): number {
  return date.getTimezoneOffset();
}

export function csvFile(csv: string, filename: string): File {
  return new File([csv], filename, { type: "text/csv;charset=utf-8" });
}
