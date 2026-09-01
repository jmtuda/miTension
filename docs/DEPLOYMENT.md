# Despliegue y prueba E2E

Esta guía prepara un único entorno real compartido por Android y web. Los valores entre `<...>` se obtienen durante el proceso y no se guardan en Git. Supabase es la fuente canónica; no se crea almacenamiento para exportaciones.

## 1. Crear el proyecto Supabase

1. En Supabase, crear un proyecto en el plan gratuito y guardar la contraseña de la base de datos en un gestor de contraseñas.
2. Anotar localmente el **Project ref**, la **Project URL** (`https://<PROJECT_REF>.supabase.co`) y la **Publishable key** (`sb_publishable_...`) del diálogo **Connect** o de **Settings → API Keys**. No copiar claves `sb_secret_...`, `service_role` ni ninguna clave elevada a un cliente.
3. Revisar en el panel los límites vigentes de base de datos, transferencia, autenticación y pausa por inactividad. Para este MVP personal no se habilitan extras de pago.

## 2. Aplicar las migraciones

Desde la raíz del repositorio, con Node.js disponible, usar la versión de CLI validada por el proyecto:

```sh
npx supabase@2.116.0 login
npx supabase@2.116.0 link --project-ref <PROJECT_REF>
npx supabase@2.116.0 migration list
npx supabase@2.116.0 db push --dry-run
npx supabase@2.116.0 db push
npx supabase@2.116.0 migration list
```

El `dry-run` debe mostrar únicamente las migraciones versionadas en `supabase/migrations/`; el último listado debe mostrarlas aplicadas tanto en local como en remoto. `supabase/tests/auth_test_setup.sql` es solo para el PostgreSQL aislado del CI y **no** se aplica al proyecto real.

No ejecutar `db reset --linked`: borra los datos remotos. Las migraciones posteriores se añaden como archivos nuevos y se aplican con el mismo ciclo `migration list`, `db push --dry-run`, `db push`.

## 3. Crear la única cuenta

1. En **Authentication → Users**, crear manualmente un usuario con el correo personal elegido, contraseña fuerte y correo confirmado.
2. En **Authentication → Sign In / Providers → Email**, desactivar **Allow new users to sign up**.
3. Confirmar que el proveedor Email permite iniciar sesión y que **Allow anonymous sign-ins** está desactivado.
4. Probar únicamente el inicio de sesión de la cuenta creada. Las aplicaciones no ofrecen registro público.

La contraseña solo se introduce en la pantalla de autorización inicial de cada dispositivo/navegador. No se escribe en variables, Gradle, Vercel, documentación ni commits.

## 4. Configurar y desplegar web

Se usa Vercel Hobby por su soporte directo de React/Vite y el despliegue automático desde GitHub.

1. Importar `jmtuda/miTension` en Vercel.
2. Configurar **Root Directory** como `web` y **Framework Preset** como Vite.
3. Mantener `npm run build` como Build Command y `dist` como Output Directory.
4. Añadir en **Production** (y en Preview solo si se va a probar esa URL):

   ```text
   VITE_SUPABASE_URL=https://<PROJECT_REF>.supabase.co
   VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_<CLAVE_PUBLICA>
   ```

5. Desplegar y abrir la URL HTTPS resultante. Un cambio de variables requiere un nuevo despliegue.
6. En Supabase, configurar **Authentication → URL Configuration → Site URL** con la URL de producción de Vercel. No añadir comodines ni redirecciones que no se usen.

Las variables `VITE_*` forman parte del JavaScript público compilado: solo contienen la URL y la Publishable key. La protección de los datos depende de Auth y RLS. Nunca configurar aquí una clave `sb_secret_...` ni `service_role`.

Para desarrollo local, copiar `web/.env.example` a `web/.env.local`, completar esos dos valores y no versionar el archivo. Verificación previa al despliegue:

```sh
cd web
npm ci
npm test
npm run lint
npm run build
```

## 5. Generar un APK Android instalable

Configurar el mismo proyecto Supabase mediante variables de entorno o en `~/.gradle/gradle.properties`, nunca en `android/gradle.properties` versionado:

```text
SUPABASE_URL=https://<PROJECT_REF>.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_<CLAVE_PUBLICA>
```

Con JDK 17 y Android SDK 33 instalados:

```sh
cd android
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

En Windows usar `gradlew.bat` en lugar de `./gradlew`. El APK de prueba instalable queda en `android/app/build/outputs/apk/debug/app-debug.apk`; instalarlo con Android Studio o `adb install -r <ruta-al-apk>`. Es un artefacto transitorio firmado con la clave de depuración y no es una publicación de tienda.

## 6. Recuperación operativa mínima

- **Sesión expirada:** abrir la aplicación conectada; la renovación es automática. Si la sesión fue revocada, volver a autorizar el dispositivo con la única cuenta.
- **Proyecto pausado o cuota agotada:** revisar el estado y las cuotas en Supabase, reactivar el proyecto si el plan lo permite y reintentar sin recrear datos locales.
- **Despliegue web defectuoso:** volver al último deployment válido desde Vercel y comprobar las dos variables de entorno antes de redesplegar.
- **Esquema incompleto:** ejecutar `migration list` y después `db push --dry-run`; aplicar solo las migraciones pendientes versionadas. No editar tablas manualmente ni resetear el remoto.
- **Android sin sincronizar:** conservar Room y la sesión local, comprobar conectividad/configuración y dejar que WorkManager reintente. No borrar los datos de la aplicación antes de recuperar las mediciones pendientes.

## 7. Checklist manual P1.14 — completado

P1.14 se cerró después de ejecutar este checklist contra el Supabase real compartido por la web desplegada en Vercel y el APK Android instalado. Las evidencias se mantienen sin contraseñas, tokens ni notas médicas reales.

- [x] **Preparación:** CI verde; migraciones local/remoto coinciden; una sola cuenta Auth; registro y acceso anónimo desactivados; web y APK apuntan al mismo Project ref.
- [x] **Login inicial:** autorizar Android y web con la cuenta única; cerrar y volver a abrir ambos; confirmar que la sesión persiste sin login cotidiano.
- [x] **Alta Android offline:** activar modo avión, completar las dos lecturas con un caso `.5`, revisar media/fecha/nota, confirmar y comprobar estado pendiente en historial.
- [x] **Cancelación:** iniciar otra alta y cancelarla antes de confirmar; comprobar que no aparece en ningún historial.
- [x] **Reconexión y sync:** recuperar conexión, esperar/reintentar sincronización y comprobar que el pendiente pasa a sincronizado sin duplicarse.
- [x] **Android → web:** recargar web y verificar la misma medición, valores redondeados, nota y fecha/hora local.
- [x] **Web → Android:** crear y confirmar una medición distinta en web; sincronizar Android y verificar que aparece una sola vez.
- [x] **Soft delete Android offline:** sin conexión, confirmar el borrado de una medición; reconectar, sincronizar y verificar que desaparece en web sin borrado físico ni reaparición.
- [x] **Soft delete web:** borrar la otra medición desde web y comprobar tras sync que desaparece de Android.
- [x] **Filtros:** crear datos de prueba suficientes y comprobar que el mismo intervalo selecciona el conjunto esperado para historial y exportación.
- [x] **CSV Android:** exportar el filtro activo, compartir mediante el share sheet y comprobar BOM UTF-8, `;`, cabecera exacta, fecha con offset y escapado de notas.
- [x] **PDF Android:** exportar/compartir el filtro activo; comprobar legibilidad, zona local, campos requeridos y ausencia de eliminados/metadatos.
- [x] **CSV web:** descargar y, si el navegador lo permite, compartir; comprobar contenido idéntico al conjunto filtrado y fallback de descarga.
- [x] **PDF web:** descargar y, si está disponible, compartir; comprobar legibilidad y fallback de descarga.
- [x] **RLS observable:** cerrar sesión y confirmar que no se muestran datos; volver a autorizar y confirmar que reaparecen. Usar solo la Publishable key; no probar clientes con `sb_secret_...` ni `service_role`.
- [x] **Cierre:** confirmar que no quedan duplicados, pendientes inesperados ni datos de prueba que deban conservarse; guardar evidencias no sensibles y anotar incidencias.
