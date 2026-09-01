# Estado del proyecto

- **Fase actual:** MVP completado y operativo.
- **Completado:** P0.1–P0.13 y P1.1–P1.18.
- **Pendiente:** ninguna tarea necesaria para el cierre del MVP.
- **PR integradas:** #3 — `feat: protect measurements with Supabase Auth and RLS`; #4 — `feat: add offline Android measurement flow`; #6 — `feat: sincroniza mediciones Android con Supabase`; #7 — `feat: implementa P1.1–P1.6 Web`; #8 — `docs: sustituye OneDrive por compartir y exportar`; #9 — `feat: implementa exportación y compartir P1.7-P1.10`; #10 — `feat: cierra autenticación Android, pruebas y CI`; #11 — `docs: prepara despliegue y validación E2E`.
- **Corrección de cierre P0.5:** la protección RLS y la sesión web ya estaban integradas, pero Android solo aceptaba un token cargado externamente. Se completa con login inicial contra Supabase Auth, sesión cifrada persistente y renovación normal del token, sin registro público ni credenciales incrustadas.
- **Decisión vigente:** sin integración automática con OneDrive; Supabase sigue siendo la fuente canónica y la copia externa es manual mediante Compartir/Exportar.
- **Despliegue real:** web desplegada en Vercel y operativa; APK Android instalado y probado. Ambos clientes usan el mismo proyecto Supabase.
- **Supabase real:** migraciones versionadas aplicadas; tabla `public.measurements`, RLS y políticas de propiedad activas para la cuenta única.
- **P1.14:** E2E real validado satisfactoriamente en web y Android. Se comprobaron login persistente, flujo de medición, funcionamiento offline, sincronización Android ↔ Supabase ↔ web, borrado, filtros y exportación/compartir CSV/PDF.
- **Migración histórica:** operación puntual completada desde `Tensión.xlsx`: 70 mediciones insertadas, 0 duplicados, 0 conflictos y 0 errores. Las fechas se interpretaron en `Europe/Madrid` y se convirtieron a UTC; `notes = null`.
- **Decisión de cierre:** la migración histórica fue una operación única. No se creó ni se necesita mantener un importador permanente; P1.15–P1.18 quedan cerradas con esa ejecución.
- **Siguiente:** ninguno dentro del MVP; no se abre una nueva fase de producto.
- **Bloqueos:** ninguno.
