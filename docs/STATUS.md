# Estado del proyecto

- **Fase actual:** P1 — calidad y entrega.
- **Completado:** P0.1–P0.13 y P1.1–P1.12.
- **Pendiente:** P1.13 y posteriores.
- **PR integradas:** #3 — `feat: protect measurements with Supabase Auth and RLS`; #4 — `feat: add offline Android measurement flow`; #6 — `feat: sincroniza mediciones Android con Supabase`; #7 — `feat: implementa P1.1–P1.6 Web`; #8 — `docs: sustituye OneDrive por compartir y exportar`; #9 — `feat: implementa exportación y compartir P1.7-P1.10`.
- **Corrección de cierre P0.5:** la protección RLS y la sesión web ya estaban integradas, pero Android solo aceptaba un token cargado externamente. Se completa con login inicial contra Supabase Auth, sesión cifrada persistente y renovación normal del token, sin registro público ni credenciales incrustadas.
- **Decisión vigente:** sin integración automática con OneDrive; Supabase sigue siendo la fuente canónica y la copia externa es manual mediante Compartir/Exportar.
- **Siguiente:** P1.13/P1.14 requieren configuración y credenciales de infraestructura real; no se han iniciado.
- **Bloqueos:** ninguno conocido.
