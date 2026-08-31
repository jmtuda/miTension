import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App.tsx";
import { createSupabaseAuthGateway } from "./auth.ts";
import { createSupabaseMeasurementRepository } from "./data/measurements.ts";
import { isSupabaseConfigured, supabase } from "./lib/supabase.ts";
import "./styles.css";

const root = createRoot(document.getElementById("root")!);

root.render(
  <StrictMode>
    {isSupabaseConfigured && supabase ? (
      <App auth={createSupabaseAuthGateway(supabase)} repository={createSupabaseMeasurementRepository(supabase)} />
    ) : (
      <main className="centered configuration-error" role="alert">
        <h1>Falta configurar Supabase</h1>
        <p>Añade <code>VITE_SUPABASE_URL</code> y <code>VITE_SUPABASE_ANON_KEY</code> al entorno.</p>
      </main>
    )}
  </StrictMode>,
);
