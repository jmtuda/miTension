# miTension

## Objetivo

Aplicación personal para registrar y consultar la tensión arterial desde Android y web. El MVP es para un único usuario, debe funcionar sin coste y no tendrá inicio de sesión interactivo en la aplicación.

## Regla principal de registro

Una lectura aislada nunca se guarda como medición definitiva. El flujo obligatorio es:

1. **Medición 1:** sistólica, diastólica y pulso.
2. **Medición 2:** los mismos tres valores, tomada consecutivamente.
3. **Resultado:** media aritmética por campo.
4. **Confirmación:** mostrar ambas lecturas y el resultado calculado.
5. **Guardado:** persistir únicamente el resultado cuando el usuario confirma.

Cancelar antes de confirmar no crea ningún registro. Las dos lecturas pueden mantenerse como borrador local durante el flujo, pero el MVP no las conserva en la base de datos definitiva.

### Redondeo propuesto

Guardar cada media como entero, redondeando `.5` hacia arriba (`ROUND_HALF_UP`). Ejemplo: `121` y `122` producen `122`. Esta propuesta se adopta para el MVP salvo decisión contraria antes de implementar el cálculo; debe existir una sola función compartida o casos de prueba comunes para Android, web e importación.

## Alcance del MVP

- Android: alta con el flujo obligatorio, historial y eliminación.
- Android offline: Room como fuente local y sincronización automática al recuperar conexión.
- Web: alta con el mismo flujo, historial, filtros y exportación CSV.
- Nube: Supabase con PostgreSQL.
- Eliminación sin edición; la eliminación será lógica (`soft delete`).
- Copia en OneDrive tras cada nueva medición sincronizada, usando Microsoft Graph y una autorización inicial de la cuenta Microsoft.
- Importación del Excel histórico al final del MVP, después de inspeccionar y validar su estructura real.

## Fuera del MVP

- Varios usuarios y gestión de cuentas.
- Edición de mediciones guardadas.
- Gráficas.
- Informes PDF.
- Integración con dispositivos médicos.
- Conservación histórica de las dos lecturas originales.

Gráficas y PDF quedan previstas para v2.

## Reglas funcionales

- Campos obligatorios: fecha/hora, sistólica, diastólica y pulso. Nota opcional.
- Fecha/hora y nota se introducen o revisan en la confirmación, no dos veces.
- Validar campos y rangos de plausibilidad antes de avanzar. Los límites exactos deben definirse y probarse antes de cerrar la pantalla.
- Un registro confirmado no se edita; puede eliminarse después de una confirmación explícita.
- El historial normal oculta registros con `deleted_at`.
- La exportación CSV respeta los filtros activos y excluye eliminados.
- La app informa de forma visible si hay cambios pendientes de sincronizar o un error persistente.

## Criterios de éxito

- No existe una ruta que guarde solo la primera o la segunda lectura.
- Android permite completar el flujo sin Internet y sincroniza después sin duplicados.
- Android y web calculan exactamente el mismo resultado.
- Una eliminación offline termina reflejada en todos los clientes.
- Cada alta aceptada por la nube dispara un respaldo idempotente en OneDrive.
- El producto puede desplegarse dentro de los niveles gratuitos elegidos.

## Decisiones abiertas

Antes de implementar las partes afectadas hay que cerrar:

1. Rangos aceptables y si un valor fuera de rango se bloquea o solo advierte.
2. Mecanismo de acceso privado sin login interactivo (véase `ARCHITECTURE.md`).
3. Formato, ubicación y política de retención del respaldo de OneDrive.
4. Zona horaria de presentación y formato final del CSV.
5. Estructura real del Excel histórico y reglas de resolución de filas inválidas.

