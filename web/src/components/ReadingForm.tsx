import { useState, type FormEvent } from "react";
import type { Reading } from "../domain/measurement.ts";

type Props = Readonly<{
  number: 1 | 2;
  initial?: Reading;
  onSubmit: (reading: Reading) => void;
  onBack?: () => void;
  onCancel: () => void;
}>;

export function ReadingForm({ number, initial, onSubmit, onBack, onCancel }: Props) {
  const [values, setValues] = useState({
    systolic: initial ? String(initial.systolic) : "",
    diastolic: initial ? String(initial.diastolic) : "",
    pulse: initial ? String(initial.pulse) : "",
  });
  const [error, setError] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    const reading = {
      systolic: Number(values.systolic),
      diastolic: Number(values.diastolic),
      pulse: Number(values.pulse),
    };
    try {
      if (!values.systolic || !values.diastolic || !values.pulse) {
        throw new RangeError("Completa los tres valores.");
      }
      onSubmit(reading);
    } catch (reason) {
      setError(reason instanceof Error ? friendlyValidation(reason.message) : "Revisa los valores.");
    }
  }

  return (
    <section className="flow-card" aria-labelledby="reading-title">
      <div className="step-kicker">Paso {number} de 3</div>
      <h2 id="reading-title">Medición {number}</h2>
      <p className="muted">Introduce los valores tal como aparecen en el tensiómetro.</p>
      <form onSubmit={submit} noValidate>
        <div className="reading-grid">
          <NumberField label="Sistólica" unit="mmHg" value={values.systolic} onChange={(systolic) => setValues({ ...values, systolic })} />
          <NumberField label="Diastólica" unit="mmHg" value={values.diastolic} onChange={(diastolic) => setValues({ ...values, diastolic })} />
          <NumberField label="Pulso" unit="lpm" value={values.pulse} onChange={(pulse) => setValues({ ...values, pulse })} />
        </div>
        {error && <p className="message error" role="alert">{error}</p>}
        <div className="actions">
          {onBack && <button className="button secondary" type="button" onClick={onBack}>Atrás</button>}
          <button className="button primary" type="submit">Continuar</button>
          <button className="button quiet" type="button" onClick={onCancel}>Cancelar</button>
        </div>
      </form>
    </section>
  );
}

function NumberField({ label, unit, value, onChange }: Readonly<{ label: string; unit: string; value: string; onChange: (value: string) => void }>) {
  const id = label.toLowerCase();
  return (
    <label className="number-field" htmlFor={id}>
      <span>{label}</span>
      <span className="input-wrap">
        <input id={id} inputMode="numeric" type="number" min="1" step="1" required value={value} onChange={(event) => onChange(event.target.value)} />
        <small>{unit}</small>
      </span>
    </label>
  );
}

function friendlyValidation(message: string): string {
  if (message.includes("diastolic")) return "La diastólica debe ser menor que la sistólica.";
  if (message.includes("positive integer")) return "Los valores deben ser números enteros positivos.";
  return message;
}
