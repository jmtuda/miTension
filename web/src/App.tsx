import { Activity, LogOut, Plus } from "lucide-react";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import type { AuthGateway, UserSession } from "./auth.ts";
import { Confirmation } from "./components/Confirmation.tsx";
import { History } from "./components/History.tsx";
import { ReadingForm } from "./components/ReadingForm.tsx";
import type { DateFilter, Measurement, MeasurementRepository } from "./data/measurements.ts";
import { addSecondReading, captureFirstReading, confirmMeasurement, type CalculatedMeasurement, type FirstReadingCaptured, type Reading } from "./domain/measurement.ts";
import { csvFile, measurementsToCsv } from "./lib/csv.ts";
import { measurementsToPdf } from "./lib/pdf.ts";
import { shareOrDownload } from "./lib/share.ts";

type Flow = { step: "idle" } | { step: "first" } | { step: "second"; first: FirstReadingCaptured } | { step: "confirm"; calculated: CalculatedMeasurement };

export function App({ auth, repository }: Readonly<{ auth: AuthGateway; repository: MeasurementRepository }>) {
  const [session, setSession] = useState<UserSession | null | undefined>(undefined);
  const [authError, setAuthError] = useState("");
  const [flow, setFlow] = useState<Flow>({ step: "idle" });
  const [measurements, setMeasurements] = useState<Measurement[]>([]);
  const [filter, setFilter] = useState<DateFilter>({ from: "", to: "" });
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState("");
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [exportStatus, setExportStatus] = useState("");

  useEffect(() => {
    let active = true;
    auth.getSession().then((value) => active && setSession(value)).catch((error: Error) => { if (active) { setAuthError(error.message); setSession(null); } });
    const unsubscribe = auth.onChange((value) => { if (active) setSession(value); });
    return () => { active = false; unsubscribe(); };
  }, [auth]);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true); setListError("");
    try { setMeasurements(await repository.list(filter)); }
    catch (error) { setListError(error instanceof Error ? error.message : "No se pudo cargar el historial."); }
    finally { setLoading(false); }
  }, [filter, repository, session]);

  useEffect(() => { void load(); }, [load]);

  if (session === undefined) return <main className="centered" aria-live="polite"><Activity className="brand-mark" /><p>Abriendo miTensión…</p></main>;
  if (!session) return <SignIn auth={auth} initialError={authError} />;

  function captureFirst(reading: Reading) { setFlow({ step: "second", first: captureFirstReading(reading) }); }
  function captureSecond(reading: Reading) {
    if (flow.step !== "second") return;
    setFlow({ step: "confirm", calculated: addSecondReading(flow.first, reading) });
  }
  async function save(measuredAt: string, notes: string | null) {
    if (flow.step !== "confirm") return;
    setSaving(true);
    try {
      const confirmed = confirmMeasurement(flow.calculated);
      await repository.create({ measuredAt, ...confirmed.values, notes });
      setFlow({ step: "idle" });
      await load();
    } finally { setSaving(false); }
  }
  async function requestDelete(measurement: Measurement) {
    if (!window.confirm("¿Eliminar esta medición? No se podrá recuperar.")) return;
    setDeletingId(measurement.id); setListError("");
    try { await repository.softDelete(measurement.id); setMeasurements((items) => items.filter((item) => item.id !== measurement.id)); }
    catch (error) { setListError(error instanceof Error ? error.message : "No se pudo eliminar la medición."); }
    finally { setDeletingId(null); }
  }
  async function exportCurrent(format: "csv" | "pdf") {
    const date = new Date().toISOString().slice(0, 10);
    const file = format === "csv"
      ? csvFile(measurementsToCsv(measurements), `mitension-${date}.csv`)
      : new File([measurementsToPdf(measurements)], `mitension-${date}.pdf`, { type: "application/pdf" });
    const result = await shareOrDownload(file);
    setExportStatus(result === "shared" ? `${format.toUpperCase()} compartido.` : `${format.toUpperCase()} descargado.`);
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="miTensión, inicio"><Activity aria-hidden="true" /><span>miTensión</span></a>
        <div className="account"><span>{session.email}</span><button className="icon-button" type="button" aria-label="Cerrar sesión" onClick={() => void auth.signOut()}><LogOut size={18} /></button></div>
      </header>
      <main id="top">
        <section className="welcome">
          <div><span className="eyebrow">Registro personal</span><h1>Tu tensión, registrada con calma.</h1><p>Dos lecturas consecutivas. Una media clara.</p></div>
          {flow.step === "idle" && <button className="button primary new-button" type="button" onClick={() => setFlow({ step: "first" })}><Plus size={19} /> Nueva medición</button>}
        </section>
        {flow.step === "first" && <ReadingForm number={1} onSubmit={captureFirst} onCancel={() => setFlow({ step: "idle" })} />}
        {flow.step === "second" && <ReadingForm number={2} onSubmit={captureSecond} onBack={() => setFlow({ step: "first" })} onCancel={() => setFlow({ step: "idle" })} />}
        {flow.step === "confirm" && <Confirmation measurement={flow.calculated} saving={saving} onBack={() => setFlow({ step: "second", first: captureFirstReading(flow.calculated.first) })} onCancel={() => setFlow({ step: "idle" })} onConfirm={save} />}
        <History measurements={measurements} filter={filter} loading={loading} error={listError} deletingId={deletingId} onFilterChange={setFilter} onDelete={(measurement) => void requestDelete(measurement)} onExport={(format) => void exportCurrent(format)} exportStatus={exportStatus} onRetry={() => void load()} />
      </main>
      <footer>Datos privados protegidos por tu sesión.</footer>
    </div>
  );
}

function SignIn({ auth, initialError }: Readonly<{ auth: AuthGateway; initialError: string }>) {
  const [email, setEmail] = useState(""); const [password, setPassword] = useState("");
  const [error, setError] = useState(initialError); const [submitting, setSubmitting] = useState(false);
  const gateway = useMemo(() => auth, [auth]);
  async function submit(event: FormEvent) {
    event.preventDefault(); setSubmitting(true); setError("");
    try { await gateway.signIn(email, password); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "No se pudo iniciar sesión."); }
    finally { setSubmitting(false); }
  }
  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="signin-title">
        <Activity className="brand-mark" aria-hidden="true" /><span className="eyebrow">Acceso privado</span><h1 id="signin-title">Bienvenido a miTensión</h1><p>Accede con la cuenta previamente autorizada. La sesión quedará guardada en este navegador.</p>
        <form onSubmit={submit}>
          <label className="field">Correo electrónico<input type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label className="field">Contraseña<input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          {error && <p className="message error" role="alert">{error}</p>}
          <button className="button primary full" type="submit" disabled={submitting}>{submitting ? "Accediendo…" : "Acceder"}</button>
        </form>
      </section>
    </main>
  );
}
