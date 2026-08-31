# Backlog del MVP

Estados sugeridos: `Pendiente`, `En curso`, `Bloqueado`, `Hecho`. Prioridades: P0 imprescindible, P1 necesaria para cerrar MVP, P2 posterior.

## P0 — Base y decisiones

- [x] **P0.1 Definir reglas pendientes:** rangos/avisos, acceso privado sin login cotidiano, zona horaria y CSV.
- [x] **P0.2 Crear estructura del repositorio:** Android, web, Supabase/migraciones, pruebas y CI mínimos.
- [x] **P0.3 Implementar dominio de medición:** dos lecturas, `ROUND_HALF_UP`, validación y vectores de prueba compartidos.
- [x] **P0.4 Crear esquema Supabase:** `measurements`, índices, timestamps, soft delete y migraciones reproducibles.
- [x] **P0.5 Proteger acceso:** Supabase Auth con cuenta única y sesión persistente, propiedad por usuario, RLS y pruebas negativas; ninguna tabla clínica pública.

## P0 — Android offline-first

- [x] **P0.6 Flujo Medición 1:** entrada y validación; no persiste registro definitivo.
- [x] **P0.7 Flujo Medición 2:** entrada y validación; permite volver sin perder la primera.
- [x] **P0.8 Confirmación:** muestra lecturas, medias, fecha/hora y nota; cancelar no guarda.
- [x] **P0.9 Room:** entidad canónica, DAO, migraciones locales y estados de sincronización.
- [x] **P0.10 Historial offline:** ordenar por fecha, ocultar eliminados y mostrar pendientes/errores.
- [x] **P0.11 Alta sincronizable:** UUID estable, WorkManager, reintentos y upsert sin duplicados.
- [x] **P0.12 Eliminación lógica:** confirmación, tombstone offline y propagación sin edición.
- [x] **P0.13 Sincronización incremental:** cursor estable, descarga de altas/eliminaciones y recuperación tras interrupciones.

## P1 — Web

- [x] **P1.1 Flujo completo de alta:** paridad funcional y de cálculo con Android.
- [x] **P1.2 Historial:** listado, orden y detalle básico.
- [x] **P1.3 Filtros:** al menos intervalo de fechas; cerrar filtros adicionales con el usuario.
- [x] **P1.4 Eliminación lógica:** confirmación y actualización del historial.
- [x] **P1.5 Exportación CSV:** filtros activos, UTF-8, fechas inequívocas y notas escapadas.
- [x] **P1.6 Diseño adaptable y estados de error/carga accesibles.**

## P1 — Compartir y exportar

- [x] **P1.7 Exportación CSV en Android:** generar bajo demanda el conjunto filtrado con el formato aprobado.
- [x] **P1.8 Informe PDF en Android y web:** generar bajo demanda un informe compartible de los registros activos filtrados.
- [x] **P1.9 Compartir en Android:** usar el mecanismo nativo para enviar CSV/PDF a aplicaciones instaladas.
- [x] **P1.10 Compartir/descargar en web:** compartir CSV/PDF cuando el navegador lo permita y usar descarga como fallback.

## P1 — Calidad y entrega

- [x] **P1.11 Pruebas automáticas:** cálculo, validación, Room, sincronización, RLS, CSV, PDF, compartir y autenticación.
- [x] **P1.12 CI:** compilación, lint y pruebas Android/web/SQL en cada cambio.
- [ ] **P1.13 Despliegue gratuito:** documentar configuración, secretos, cuotas y procedimiento de recuperación.
- [ ] **P1.14 Prueba extremo a extremo:** alta offline, reconexión, web, eliminación y exportación/compartir manual.

## P1 — Importación al final del MVP

- [ ] **P1.15 Inspeccionar el Excel real** sin modificarlo.
- [ ] **P1.16 Aprobar mapeo y reglas** de fechas, duplicados y errores.
- [ ] **P1.17 Previsualizar y validar** recuentos antes de escribir.
- [ ] **P1.18 Importar idempotentemente** y emitir informe de resultado.

## P2 — v2

- [ ] Gráficas y tendencias.
- [ ] Evaluar conservación de lecturas originales.
- [ ] Evaluar multiusuario y login completo.
- [ ] Mejoras de filtros, búsqueda y exportaciones.

## Definición de terminado

Una tarea está terminada cuando tiene criterios de aceptación comprobados, pruebas proporcionales al riesgo, secretos fuera del código, documentación actualizada y no introduce rutas que omitan el flujo de dos mediciones y confirmación.
