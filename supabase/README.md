# Supabase

Las migraciones reproducibles viven en `migrations/` y las pruebas SQL en `tests/`.

Para validar el esquema contra PostgreSQL:

```sh
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrations/202608310001_create_measurements.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f tests/measurements_schema.test.sql
```

P0.5 añadirá las políticas de acceso de Supabase Auth y sus pruebas negativas. Hasta entonces, RLS está activado sin políticas para mantener la tabla cerrada a los roles de cliente.
