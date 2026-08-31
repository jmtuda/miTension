# Estado del proyecto

- **Fase actual:** P1 — calidad y entrega.
- **Completado:** P0.1–P0.13 y P1.1–P1.13.
- **Pendiente:** P1.14 y posteriores.
- **PR integradas:** #3 — `feat: protect measurements with Supabase Auth and RLS`; #4 — `feat: add offline Android measurement flow`; #6 — `feat: sincroniza mediciones Android con Supabase`; #7 — `feat: implementa P1.1–P1.6 Web`; #8 — `docs: sustituye OneDrive por compartir y exportar`; #9 — `feat: implementa exportación y compartir P1.7-P1.10`.
- **Corrección de cierre P0.5:** la protección RLS y la sesión web ya estaban integradas, pero Android solo aceptaba un token cargado externamente. Se completa con login inicial contra Supabase Auth, sesión cifrada persistente y renovación normal del token, sin registro público ni credenciales incrustadas.
- **Decisión vigente:** sin integración automática con OneDrive; Supabase sigue siendo la fuente canónica y la copia externa es manual mediante Compartir/Exportar.
- **P1.13:** preparado el despliegue reproducible sobre Supabase y Vercel, la configuración local segura de Android/web, el APK instalable de prueba, las comprobaciones de cuotas y la recuperación operativa; no se han creado proyectos ni guardado credenciales reales.
- **Siguiente:** ejecutar el checklist P1.14 de [`DEPLOYMENT.md`](DEPLOYMENT.md) contra una instancia real compartida por Android y web.
- **Bloqueos:** P1.14 requiere que el usuario cree/configure el proyecto Supabase, la cuenta única, el despliegue web y el dispositivo Android reales.
