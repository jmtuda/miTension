# Modelo de datos

## Convenciones

- Identificadores UUID generados en cliente.
- Fechas en PostgreSQL como `timestamptz` en UTC; la UI las presenta en la zona local del dispositivo.
- Valores de presión y pulso como enteros.
- Migraciones SQL bajo control de versiones.
- La base canónica guarda solo la media confirmada, no Medición 1 y 2.

## PostgreSQL / Supabase

### `measurements`

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `uuid` | PK; creado por cliente |
| `user_id` | `uuid` | obligatorio; propietario en `auth.users`; servidor, `auth.uid()` |
| `measured_at` | `timestamptz` | obligatorio; momento de la medición |
| `systolic` | `smallint` | obligatorio; media redondeada |
| `diastolic` | `smallint` | obligatorio; media redondeada |
| `pulse` | `smallint` | obligatorio; media redondeada |
| `notes` | `text` | opcional; máximo 1.000 caracteres; normalizar vacío a `null` |
| `created_at` | `timestamptz` | obligatorio; servidor, `now()` |
| `updated_at` | `timestamptz` | obligatorio; servidor; cambia en cada mutación |
| `deleted_at` | `timestamptz` | `null` si está activo; tombstone al eliminar |

Restricciones mínimas: enteros positivos y `diastolic < systolic`. No se establecen rangos clínicos ni avisos médicos.

Índices:

- `measured_at DESC` para historial.
- `updated_at, id` para sincronización incremental estable.
- Índice parcial sobre registros activos (`deleted_at IS NULL`) si las consultas lo justifican.

No hay operación de actualización de campos clínicos en el MVP. Solo se permiten creación y establecimiento de `deleted_at`.

RLS limita lectura, creación y soft delete a filas cuyo `user_id` coincide con el usuario de Supabase Auth. `user_id` es inmutable. El rol anónimo no tiene permisos sobre la tabla y no existe política de borrado físico.

### `backup_outbox`

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `uuid` | PK |
| `measurement_id` | `uuid` | referencia a `measurements` |
| `event_type` | `text` | inicialmente `UPSERT` o `DELETE` |
| `created_at` | `timestamptz` | servidor |
| `processed_at` | `timestamptz` | `null` mientras esté pendiente |
| `attempt_count` | `integer` | por defecto `0` |
| `last_error` | `text` | mensaje saneado, sin datos médicos ni tokens |

Restricción única recomendada: `(measurement_id, event_type, versión-de-mutación)`. La representación concreta de la versión debe cerrarse al crear la migración para permitir alta y posterior eliminación sin colisiones.

### Configuración de integración

Los tokens de Microsoft no deben almacenarse en tablas accesibles al cliente. Usar almacenamiento cifrado de secretos del entorno o una tabla exclusivamente de servidor con cifrado y permisos restringidos. No incluirlos en exportaciones ni backups.

## Room (Android)

`measurements` replica los campos canónicos y añade metadatos locales:

| Campo local | Valores / propósito |
|---|---|
| `sync_state` | `PENDING_CREATE`, `PENDING_DELETE`, `SYNCED`, `ERROR` |
| `last_sync_error` | error saneado para diagnóstico/UX |
| `server_updated_at` | versión confirmada por servidor |

Una tabla de metadatos conserva el cursor de sincronización. Si se implementa persistencia de borradores, debe estar separada y contener las dos lecturas solo hasta confirmar o cancelar.

## Cálculo

Para cada campo `x`:

```text
mean(x1, x2) = ROUND_HALF_UP((x1 + x2) / 2)
```

Como las entradas son enteras, se puede implementar sin coma flotante. Vectores mínimos compartidos:

| Entrada | Resultado |
|---|---:|
| `120, 120` | `120` |
| `120, 122` | `121` |
| `121, 122` | `122` |
| `122, 121` | `122` |

## CSV

Columnas, en este orden:

```text
fecha_hora;sistolica;diastolica;pulso;notas
```

- UTF-8 con BOM, cabecera y separador `;`.
- Una fila por registro activo incluido en los filtros.
- Fechas ISO 8601 con offset explícito de la zona local usada al exportar.
- Escapado CSV estándar para notas.
- No exportar metadatos internos, borradores, eliminados ni tokens.

## Importación histórica

No diseñar el importador hasta recibir el Excel real. Entonces:

1. inventariar hojas, columnas, tipos, formatos y fórmulas;
2. definir mapeo, zona horaria, duplicados y filas inválidas;
3. ejecutar una previsualización sin escritura;
4. presentar recuentos y errores;
5. importar idempotentemente con copia previa y trazabilidad.
