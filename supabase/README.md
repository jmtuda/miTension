# Supabase

Las migraciones reproducibles viven en `migrations/` y las pruebas SQL en `tests/`.

La creación del proyecto remoto, el enlace con la CLI y la aplicación segura de
las migraciones se describen en [`docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md).

Para validar el esquema contra PostgreSQL:

```sh
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f tests/auth_test_setup.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/202608310001_create_measurements.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f tests/measurements_schema.test.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/202608310002_add_measurement_ownership_rls.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f tests/measurements_rls.test.sql
```

`auth_test_setup.sql` simula en PostgreSQL los roles y funciones que Supabase proporciona; no se aplica a un proyecto Supabase real.

## Configuración de autenticación

- Crear manualmente la única cuenta de la aplicación en Supabase Auth.
- Desactivar el registro público de nuevas cuentas.
- Autorizar esa cuenta una vez en cada dispositivo o navegador y conservar su sesión de forma segura.
- Distribuir en Android/web únicamente la Publishable key (`sb_publishable_...`); nunca la contraseña, claves `sb_secret_...`, `service_role` ni otros secretos.

La segunda migración añade el propietario `user_id`, conserva RLS y limita lectura, inserción y actualización a las filas del usuario autenticado. El rol anónimo no tiene permisos y el borrado físico no se concede. Si ya existen mediciones, la migración solo puede asignarlas automáticamente cuando existe exactamente una cuenta Auth; en cualquier otro caso aborta para evitar atribuir datos al usuario incorrecto.
