export function toDateTimeLocalValue(date: Date): string {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function localInputToUtc(value: string): string {
  const date = new Date(value);
  if (!value || Number.isNaN(date.getTime())) throw new RangeError("Fecha y hora no válidas");
  return date.toISOString();
}

export function formatLocalDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
