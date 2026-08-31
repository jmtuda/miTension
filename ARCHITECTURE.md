# Arquitectura

## Principios

- Offline-first en Android.
- PostgreSQL/Supabase es la fuente de verdad compartida.
- UUID generado por el cliente para reintentos idempotentes.
- Una sola regla de cálculo y casos de prueba equivalentes en todos los clientes.
- Secretos de administración y tokens de Microsoft nunca se distribuyen en Android ni en JavaScript público.
- Diseño mínimo compatible con niveles gratuitos; revisar cuotas antes del despliegue.

## Componentes

```text
Android (Kotlin + Compose)
  └─ Room + cola de sincronización
             │ HTTPS
Web          │
  └─ UI + capa servidor ── Supabase API/Functions ── PostgreSQL
                                      │
                                      └─ trabajador/outbox ── Microsoft Graph ── OneDrive
```

### Android

- Kotlin y Jetpack Compose.
- Room contiene la vista local de mediciones y el estado de sincronización.
- WorkManager ejecuta reintentos con conectividad y backoff.
- El asistente de alta conserva Medición 1 y 2 solo como estado/borrador. Al confirmar crea una medición local con UUID y estado pendiente.
- El historial lee Room para responder sin conexión.

### Web

- Aplicación web instalable en un alojamiento gratuito compatible.
- Usa el mismo flujo de tres pantallas y los mismos vectores de prueba de medias.
- Las operaciones privilegiadas y Microsoft Graph pasan por código de servidor/funciones; no se exponen claves secretas al navegador.
- CSV se genera con los registros filtrados visibles desde la fuente de verdad.

### Supabase

- PostgreSQL almacena mediciones, eliminaciones lógicas y eventos de respaldo.
- API o Edge Functions validan entradas, aplican acceso privado e idempotencia.
- Row Level Security debe estar activada. La clave `service_role` solo existe en servidor.
- Realtime no es requisito; sincronización por consulta incremental es suficiente.

## Acceso de usuario único

“Sin login” significa sin pantalla de autenticación cotidiana, no una base de datos pública. Antes de publicar debe elegirse una de estas opciones:

1. **Recomendada:** autorización inicial única por dispositivo/navegador y sesión persistente de Supabase Auth; después la app abre sin pedir credenciales mientras la sesión sea válida.
2. Aprovisionamiento manual de una credencial revocable por instalación, guardada en almacenamiento seguro y validada por una función de servidor.

No se acepta usar tablas abiertas con la clave anónima ni incluir `service_role` o secretos compartidos permanentes en el cliente. Para el MVP, la decisión recomendada es la opción 1; la UX concreta se cierra antes de implementar autenticación y RLS.

## Sincronización

Cada entidad usa un UUID estable y `updated_at` del servidor. Flujo Android:

1. Confirmar crea una fila local `PENDING_CREATE`.
2. WorkManager envía un `upsert` idempotente por `id`.
3. El servidor valida y devuelve su versión y marca temporal.
4. Android marca la fila `SYNCED`.
5. Android descarga cambios posteriores a su cursor, incluidos `deleted_at`.

Eliminar offline establece `deleted_at` local y `PENDING_DELETE`; el servidor aplica el tombstone de forma idempotente. La política inicial de conflicto es **última mutación del servidor**, adecuada porque no hay edición. Nunca se reutiliza un ID eliminado.

El cursor y los estados de sincronización son metadatos locales, no campos de negocio. Los fallos permanecen reintentables y visibles; no se descartan cambios silenciosamente.

## Respaldo en OneDrive

Tras aceptar una nueva medición en PostgreSQL, la misma transacción crea un evento en una tabla outbox. Un trabajador toma eventos pendientes y llama a Microsoft Graph. Así, un fallo de OneDrive no revierte ni pierde la medición.

- Autorización OAuth de Microsoft una vez; refresh token cifrado y solo en servidor.
- Evento único por `measurement_id` y tipo para evitar duplicados.
- Reintentos con backoff y registro de último error.
- Propuesta MVP: mantener un archivo JSON canónico reemplazable y un CSV legible en una carpeta `miTension`; cerrar formato y retención antes de implementarlo.
- Una eliminación debe quedar reflejada en el siguiente respaldo; no se borra evidencia local de la cola hasta confirmación.

## Seguridad y operación

- HTTPS, RLS, validación en servidor y mínimo privilegio.
- Variables secretas en el gestor del alojamiento/Supabase.
- No registrar valores médicos, tokens ni notas en logs de diagnóstico.
- Exportación y copias se consideran datos sensibles.
- Migraciones SQL versionadas y reproducibles.
- Pruebas automáticas para cálculo, validación, idempotencia, sincronización y RLS.

## Orden de implementación

1. Dominio y pruebas del cálculo.
2. Modelo/migraciones y políticas de acceso.
3. Android offline y sincronización.
4. Web, filtros y CSV.
5. Outbox y OneDrive.
6. Inspección e importación del Excel histórico.

