# Arquitectura

## Principios

- Offline-first en Android.
- PostgreSQL/Supabase es la fuente de verdad compartida.
- UUID generado por el cliente para reintentos idempotentes.
- Una sola regla de cálculo y casos de prueba equivalentes en todos los clientes.
- No hay integraciones cloud externas automáticas ni credenciales de terceros para exportar o compartir.
- Diseño mínimo compatible con niveles gratuitos; revisar cuotas antes del despliegue.

## Componentes

```text
Android (Kotlin + Compose)
  └─ Room + cola de sincronización
             │ HTTPS
Web          │
  └─ UI ──────────────── Supabase API/Functions ── PostgreSQL

Android/Web ── exportación bajo demanda CSV/PDF ── compartir/descargar
```

### Estructura del repositorio

```text
android/app/       esqueleto de la aplicación Compose
android/domain/    dominio Kotlin puro y pruebas
web/               dominio TypeScript y futura aplicación web
contracts/         vectores de comportamiento comunes
supabase/          migraciones y pruebas de PostgreSQL/Supabase
docs/              documentación que actúa como fuente de verdad
```

El cálculo se implementa de forma nativa en Kotlin y TypeScript. Ambos usan `contracts/measurement-mean-cases.csv` como contrato ejecutable para evitar divergencias sin introducir una dependencia compartida entre plataformas.

### Android

- Kotlin y Jetpack Compose.
- Room contiene la vista local de mediciones y el estado de sincronización.
- WorkManager ejecuta reintentos con conectividad y backoff.
- El asistente de alta conserva Medición 1 y 2 solo como estado/borrador. Al confirmar crea una medición local con UUID y estado pendiente.
- El historial lee Room para responder sin conexión.
- CSV y PDF se generan bajo demanda y se entregan al mecanismo nativo de compartir, que permite elegir cualquier aplicación instalada compatible.
- Supabase Auth permite autorizar inicialmente el dispositivo con la cuenta única ya aprovisionada. Access token, refresh token, usuario y caducidad se conservan en preferencias cifradas; el token se renueva antes de sincronizar. URL y Publishable key (`sb_publishable_...`) llegan mediante propiedades de build, nunca mediante credenciales incrustadas.

### Web

- Aplicación web instalable en un alojamiento gratuito compatible.
- Usa el mismo flujo de tres pantallas y los mismos vectores de prueba de medias.
- CSV se genera con los registros filtrados visibles desde la fuente de verdad.
- PDF se genera como informe compartible del conjunto filtrado.
- Los archivos se descargan y, si el navegador ofrece una capacidad estándar de compartir archivos, pueden compartirse con fallback a descarga.

### Supabase

- PostgreSQL almacena mediciones y eliminaciones lógicas como fuente canónica cloud.
- API o Edge Functions validan entradas, aplican acceso privado e idempotencia.
- Row Level Security debe estar activada. Ninguna clave elevada (`sb_secret_...` o la heredada `service_role`) entra en los clientes.
- Realtime no es requisito; sincronización por consulta incremental es suficiente.

## Acceso de usuario único

“Sin login” significa sin pantalla de autenticación cotidiana, no una base de datos pública. El MVP usará Supabase Auth con una autorización inicial por dispositivo o navegador y una sesión persistente. Después la aplicación abre sin pedir credenciales mientras la sesión siga siendo válida.

Se aprovisiona una única cuenta y se desactiva el registro público de nuevas cuentas. Cada medición pertenece a `auth.uid()` y las políticas RLS solo permiten al usuario autenticado leer, crear y aplicar soft delete sobre sus propias filas. El rol anónimo no tiene acceso y no se permite el borrado físico.

La Publishable key de Supabase puede distribuirse en los clientes junto con la sesión persistente del usuario; claves `sb_secret_...`, `service_role`, contraseñas y secretos compartidos permanentes solo existen en entornos de servidor. Android conserva la sesión cifrada en el dispositivo y web delega su persistencia y renovación al cliente oficial de Supabase.

## Sincronización

Cada entidad usa un UUID estable y `updated_at` del servidor. Flujo Android:

1. Confirmar crea una fila local `PENDING_CREATE`.
2. WorkManager envía un `upsert` idempotente por `id`.
3. El servidor valida y devuelve su versión y marca temporal.
4. Android marca la fila `SYNCED`.
5. Android descarga cambios posteriores a su cursor, incluidos `deleted_at`.

Eliminar offline establece `deleted_at` local y `PENDING_DELETE`; el servidor aplica el tombstone de forma idempotente. La política inicial de conflicto es **última mutación del servidor**, adecuada porque no hay edición. Nunca se reutiliza un ID eliminado.

El cursor y los estados de sincronización son metadatos locales, no campos de negocio. Los fallos permanecen reintentables y visibles; no se descartan cambios silenciosamente.

## Compartir y exportar

La copia externa es una acción manual bajo demanda. Android y web generan CSV o PDF a partir de los registros activos incluidos en los filtros actuales; estos archivos no sustituyen a Supabase como fuente canónica ni requieren outbox, trabajador o credenciales de terceros.

- Android delega el destino al mecanismo nativo de compartir.
- Web usa capacidades estándar del navegador para compartir cuando sean viables y ofrece siempre la descarga como fallback.
- OneDrive, Google Drive y correo son posibles destinos elegidos por el usuario, no integraciones automáticas de miTension.

## Seguridad y operación

- HTTPS, RLS, validación en servidor y mínimo privilegio.
- Variables secretas en el gestor del alojamiento/Supabase.
- No registrar valores médicos, tokens ni notas en logs de diagnóstico.
- Los archivos exportados y compartidos se consideran datos sensibles.
- Migraciones SQL versionadas y reproducibles.
- Pruebas automáticas para cálculo, validación, idempotencia, sincronización, RLS, exportación y compartir.
- Despliegue web estático en Vercel con `web/` como raíz y variables públicas de Supabase configuradas por entorno.
- La CLI de Supabase aplica las migraciones versionadas mediante `link`, `db push --dry-run` y `db push`; nunca se resetea el proyecto real.
- El APK instalable de verificación es una compilación `debug` transitoria configurada por variables de entorno o propiedades Gradle locales.

El procedimiento operativo y el checklist manual E2E están en [DEPLOYMENT.md](DEPLOYMENT.md).

## Orden de implementación

1. Dominio y pruebas del cálculo.
2. Modelo/migraciones y políticas de acceso.
3. Android offline y sincronización.
4. Web, filtros y CSV.
5. Exportación CSV/PDF y compartir bajo demanda.
6. Inspección e importación del Excel histórico.
