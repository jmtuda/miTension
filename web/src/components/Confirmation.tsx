import { useState, type FormEvent } from "react";
import type { CalculatedMeasurement } from "../domain/measurement.ts";
import { localInputToUtc, toDateTimeLocalValue } from "../lib/date.ts";

type Props = Readonly<{
  measurement: CalculatedMeasurement;
  saving: boolean;
  onBack: () => void;
  onCancel: () => void;
  onConfirm: (measuredAt: string, notes: string | null) => Promise<void>;
}>;

export function Confirmation({ measurement, saving, onBack, onCancel, onConfirm }: Props) {
  const [measuredAt, setMeasuredAt] = useState(() => toDateTimeLocalValue(new Date()));
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      if (notes.length > 1000) throw new RangeError("La nota no puede superar 1.000 caracteres.");
      await onConfirm(localInputToUtc(measuredAt), notes.trim() || null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "No se pudo guardar la medición.");
    }
  }

  return (
    <section className="flow-card" aria-labelledby="confirmation-title">
      <div className="step-kicker">Paso 3 de 3</div>
      <h2 id="confirmation-title">Revisar y confirmar</h2>
      <p className="muted">Solo se guardarán las medias confirmadas.</p>
      <div className="comparison" aria-label="Resumen de valores">
        <ReadingSummary label="Medición 1" values={measurement.first} />
        <ReadingSummary label="Medición 2" values={measurement.second} />
        <ReadingSummary label="Media" values={measurement.result} emphasized />
      </div>
      <form onSubmit={submit}>
        <label className="field" htmlFor="measured-at">
          <span>Fecha y hora</span>
          <input id="measured-at" type="datetime-local" required value={measuredAt} onChange={(event) => setMeasuredAt(event.target.value)} />
        </label>
        <label className="field" htmlFor="notes">
          <span>Nota <small>(opcional)</small></span>
          <textarea id="notes" maxLength={1000} rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} />
          <small className="counter">{notes.length}/1.000</small>
        </label>
        {error && <p className="message error" role="alert">{error}</p>}
        <div className="actions">
          <button className="button secondary" type="button" onClick={onBack} disabled={saving}>Atrás</button>
          <button className="button primary" type="submit" disabled={saving}>{saving ? "Guardando…" : "Confirmar y guardar"}</button>
          <button className="button quiet" type="button" onClick={onCancel} disabled={saving}>Cancelar</button>
        </div>
      </form>
    </section>
  );
}

function ReadingSummary({ label, values, emphasized = false }: Readonly<{ label: string; values: { systolic: number; diastolic: number; pulse: number }; emphasized?: boolean }>) {
  return (
    <div className={emphasized ? "summary mean" : "summary"}>
      <strong>{label}</strong>
      <span><b>{values.systolic}</b> / <b>{values.diastolic}</b> mmHg</span>
      <small>{values.pulse} lpm</small>
    </div>
  );
}
