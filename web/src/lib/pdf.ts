import type { Measurement } from "../data/measurements.ts";
import { formatLocalDate } from "./date.ts";

const PAGE_WIDTH = 595;
const PAGE_HEIGHT = 842;
const MARGIN = 42;
const ROW_WIDTHS = [145, 62, 62, 56, 186] as const;

type PdfPage = Readonly<{ commands: string }>;

export function measurementsToPdf(
  measurements: readonly Measurement[],
  generatedAt = new Date(),
): ArrayBuffer {
  const pages = paginate(measurements);
  const objects: string[] = [];
  const pageIds = pages.map((_, index) => 5 + index * 2);
  objects[1] = "<< /Type /Catalog /Pages 2 0 R >>";
  objects[2] = `<< /Type /Pages /Kids [${pageIds.map((id) => `${id} 0 R`).join(" ")}] /Count ${pages.length} >>`;
  objects[3] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>";
  objects[4] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>";

  pages.forEach((page, index) => {
    const pageId = pageIds[index];
    const contentId = pageId + 1;
    const header = pageHeader(index + 1, pages.length, generatedAt);
    const stream = `${header}${page.commands}${pageFooter(index + 1, pages.length)}`;
    objects[pageId] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${contentId} 0 R >>`;
    objects[contentId] = `<< /Length ${byteLength(stream)} >>\nstream\n${stream}endstream`;
  });

  return buildPdf(objects);
}

function paginate(measurements: readonly Measurement[]): PdfPage[] {
  const pages: PdfPage[] = [];
  let commands = tableHeader(735);
  let y = 710;

  if (measurements.length === 0) {
    commands += text("No hay mediciones en el conjunto filtrado.", MARGIN, y, 10, false);
    return [{ commands }];
  }

  for (const measurement of measurements) {
    const notes = wrap(measurement.notes ?? "", 34);
    const rowHeight = Math.max(30, 18 + notes.length * 12);
    if (y - rowHeight < 70) {
      pages.push({ commands });
      commands = tableHeader(735);
      y = 710;
    }
    commands += row(measurement, notes, y, rowHeight);
    y -= rowHeight;
  }
  pages.push({ commands });
  return pages;
}

function pageHeader(page: number, total: number, generatedAt: Date): string {
  return [
    text("miTension", MARGIN, 795, 20, true),
    text("Informe de mediciones", MARGIN, 770, 14, true),
    text(`Generado: ${formatLocalDate(generatedAt.toISOString())}`, MARGIN, 750, 8, false),
    page > 1 ? text(`Continuacion - pagina ${page} de ${total}`, 410, 770, 8, false) : "",
  ].join("");
}

function pageFooter(page: number, total: number): string {
  return line(MARGIN, 48, PAGE_WIDTH - MARGIN, 48, 0.5) + text(`Pagina ${page} de ${total}`, 480, 32, 8, false);
}

function tableHeader(y: number): string {
  const labels = ["Fecha y hora", "Sistolica", "Diastolica", "Pulso", "Notas"];
  let x = MARGIN;
  let result = rectangle(MARGIN, y - 24, PAGE_WIDTH - MARGIN * 2, 24, "0.91 0.95 0.93");
  labels.forEach((label, index) => {
    result += text(label, x + 5, y - 16, 8, true);
    x += ROW_WIDTHS[index];
  });
  return result;
}

function row(measurement: Measurement, notes: readonly string[], y: number, height: number): string {
  const values = [formatLocalDate(measurement.measured_at), String(measurement.systolic), String(measurement.diastolic), String(measurement.pulse)];
  let x = MARGIN;
  let result = line(MARGIN, y - height, PAGE_WIDTH - MARGIN, y - height, 0.35);
  values.forEach((value, index) => {
    result += text(value, x + 5, y - 18, 8, false);
    x += ROW_WIDTHS[index];
  });
  notes.forEach((note, index) => { result += text(note, x + 5, y - 18 - index * 12, 8, false); });
  return result;
}

function wrap(value: string, maxLength: number): string[] {
  const normalized = value.replace(/\s+/g, " ").trim();
  if (!normalized) return [""];
  const lines: string[] = [];
  let current = "";
  for (const word of normalized.split(" ")) {
    if (word.length > maxLength) {
      if (current) { lines.push(current); current = ""; }
      for (let index = 0; index < word.length; index += maxLength) lines.push(word.slice(index, index + maxLength));
    } else if (!current || `${current} ${word}`.length <= maxLength) current = current ? `${current} ${word}` : word;
    else { lines.push(current); current = word; }
  }
  if (current) lines.push(current);
  return lines;
}

function text(value: string, x: number, y: number, size: number, bold: boolean): string {
  return `BT /${bold ? "F2" : "F1"} ${size} Tf 1 0 0 1 ${x} ${y} Tm (${escapePdf(value)}) Tj ET\n`;
}

function line(x1: number, y1: number, x2: number, y2: number, width: number): string {
  return `${width} w 0.78 G ${x1} ${y1} m ${x2} ${y2} l S\n`;
}

function rectangle(x: number, y: number, width: number, height: number, color: string): string {
  return `${color} rg ${x} ${y} ${width} ${height} re f 0 g\n`;
}

function escapePdf(value: string): string {
  return toWinAnsi(value).replaceAll("\\", "\\\\").replaceAll("(", "\\(").replaceAll(")", "\\)");
}

function toWinAnsi(value: string): string {
  return Array.from(value.normalize("NFC"), (character) => character.charCodeAt(0) <= 255 ? character : "?").join("");
}

function byteLength(value: string): number { return value.length; }

function buildPdf(objects: string[]): ArrayBuffer {
  let output = "%PDF-1.4\n%âãÏÓ\n";
  const offsets = [0];
  for (let id = 1; id < objects.length; id += 1) {
    offsets[id] = byteLength(output);
    output += `${id} 0 obj\n${objects[id]}\nendobj\n`;
  }
  const xref = byteLength(output);
  output += `xref\n0 ${objects.length}\n0000000000 65535 f \n`;
  for (let id = 1; id < objects.length; id += 1) output += `${String(offsets[id]).padStart(10, "0")} 00000 n \n`;
  output += `trailer\n<< /Size ${objects.length} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
  return Uint8Array.from(output, (character) => character.charCodeAt(0) & 0xff).buffer as ArrayBuffer;
}
