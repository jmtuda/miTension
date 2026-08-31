import { FileDown, FileText, Trash2 } from "lucide-react";
import type { DateFilter, Measurement } from "../data/measurements.ts";
import { formatLocalDate } from "../lib/date.ts";

type Props = Readonly<{
  measurements: readonly Measurement[];
  filter: DateFilter;
  loading: boolean;
  error: string;
  deletingId: string | null;
  onFilterChange: (filter: DateFilter) => void;
  onDelete: (measurement: Measurement) => void;
  onExport: (format: "csv" | "pdf") => void;
  exportStatus: string;
  onRetry: () => void;
}>;

export function History({ measurements, filter, loading, error, deletingId, onFilterChange, onDelete, onExport, exportStatus, onRetry }: Props) {
  return (
    <section className="history" aria-labelledby="history-title" aria-busy={loading}>
      <div className="section-heading">
        <div>
          <span className="eyebrow">Registro</span>
          <h2 id="history-title">Historial</h2>
        </div>
        <div className="export-actions" aria-label="Compartir o descargar">
          <button className="button secondary export" type="button" onClick={() => onExport("csv")} disabled={loading || measurements.length === 0}>
            <FileDown size={17} aria-hidden="true" /> CSV
          </button>
          <button className="button secondary export" type="button" onClick={() => onExport("pdf")} disabled={loading || measurements.length === 0}>
            <FileText size={17} aria-hidden="true" /> PDF
          </button>
        </div>
      </div>
      <form className="filters" onSubmit={(event) => event.preventDefault()} aria-label="Filtrar historial">
        <label>Desde<input type="date" value={filter.from} max={filter.to || undefined} onChange={(event) => onFilterChange({ ...filter, from: event.target.value })} /></label>
        <label>Hasta<input type="date" value={filter.to} min={filter.from || undefined} onChange={(event) => onFilterChange({ ...filter, to: event.target.value })} /></label>
        {(filter.from || filter.to) && <button className="button quiet" type="button" onClick={() => onFilterChange({ from: "", to: "" })}>Limpiar</button>}
      </form>
      {exportStatus && <p className="export-status" role="status">{exportStatus}</p>}
      <div className="status-region" aria-live="polite">
        {loading && <p className="message">Cargando mediciones…</p>}
        {!loading && error && <div className="message error" role="alert">{error} <button type="button" onClick={onRetry}>Reintentar</button></div>}
        {!loading && !error && measurements.length === 0 && (
          <div className="empty"><span aria-hidden="true">—</span><h3>No hay mediciones</h3><p>{filter.from || filter.to ? "No hay resultados en este intervalo." : "Tu primera medición aparecerá aquí."}</p></div>
        )}
      </div>
      {!loading && !error && measurements.length > 0 && (
        <ol className="measurement-list">
          {measurements.map((measurement) => (
            <li key={measurement.id} className="measurement-row">
              <div className="measurement-date"><time dateTime={measurement.measured_at}>{formatLocalDate(measurement.measured_at)}</time>{measurement.notes && <p>{measurement.notes}</p>}</div>
              <div className="measurement-values"><span><b>{measurement.systolic}</b><small>SIS</small></span><i>/</i><span><b>{measurement.diastolic}</b><small>DIA</small></span><span className="pulse"><b>{measurement.pulse}</b><small>PULSO</small></span></div>
              <button className="icon-button" type="button" aria-label={`Eliminar medición del ${formatLocalDate(measurement.measured_at)}`} onClick={() => onDelete(measurement)} disabled={deletingId === measurement.id}>
                <Trash2 size={18} aria-hidden="true" />
              </button>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
