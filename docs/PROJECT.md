# miTension

## Objetivo

Aplicación personal para registrar y consultar la tensión arterial desde Android y web. El MVP es para un único usuario, debe funcionar sin coste y no tendrá un inicio de sesión cotidiano: se autoriza una vez por dispositivo y se conserva la sesión.

## Regla principal de registro

Una lectura aislada nunca se guarda como medición definitiva. El flujo obligatorio es:

1. **Medición 1:** sistólica, diastólica y pulso.
2. **Medición 2:** los mismos tres valores, tomada consecutivamente.
3. **Resultado:** media aritmética por campo.
4. **Confirmación:** mostrar ambas lecturas y el resultado calculado.
5. **Guardado:** persistir únicamente el resultado cuando el usuario confirma.

Cancelar antes de confirmar no crea ningún registro. Las dos lecturas pueden mantenerse como borrador local durante el flujo, pero el MVP no las conserva en la base de datos definitiva.

### Redondeo

Guardar cada media como entero, redondeando `.5` hacia arriba (`ROUND_HALF_UP`). Ejemplo: `121` y `122` producen `122`. Debe existir una sola función compartida o casos de prueba comunes para Android, web e importación.

## Alcance del MVP

- Android: alta con el flujo obligatorio, historial y eliminación.
- Android offline: Room como fuente local y sincronización automática al recuperar conexión.
- Web: alta con el mismo flujo, historial, filtros y exportación CSV/PDF.
- Nube: Supabase con PostgreSQL.
- Eliminación sin edición; la eliminación será lógica (`soft delete`).
- Compartir/exportar manualmente bajo demanda en CSV o PDF, sin copia cloud automática externa.
- Migración puntual del Excel histórico al cierre del MVP, sin importador permanente.

## Fuera del MVP

- Varios usuarios y gestión de cuentas.
- Edición de mediciones guardadas.
- Gráficas.
- Integración con dispositivos médicos.
- Conservación histórica de las dos lecturas originales.

Las gráficas quedan previstas para v2.

## Reglas funcionales

- Campos obligatorios: fecha/hora, sistólica, diastólica y pulso. Nota de texto libre opcional, con un máximo de 1.000 caracteres.
- Fecha/hora y nota se introducen o revisan en la confirmación, no dos veces.
- Por defecto, la fecha/hora es la actual. Se guarda el instante en UTC y se presenta en la zona local del dispositivo.
- Validar únicamente enteros positivos y `diastolic < systolic`. No se fijan rangos clínicos ni avisos médicos en el MVP.
- Un registro confirmado no se edita; puede eliminarse después de una confirmación explícita.
- La eliminación oculta el registro inmediatamente y usa `soft delete`; no hay papelera visible en el MVP.
- La exportación CSV respeta los filtros activos y excluye eliminados.
- El CSV usa UTF-8 con BOM, separador `;` y las columnas `fecha_hora;sistolica;diastolica;pulso;notas`.
- El PDF es un informe compartible generado bajo demanda con el conjunto filtrado y sin registros eliminados.
- Android usa el mecanismo nativo de compartir para enviar CSV o PDF a aplicaciones instaladas, como OneDrive, Google Drive o correo.
- Web descarga CSV o PDF y, cuando las capacidades estándar del navegador lo permitan, ofrece compartir con fallback a descarga.
- La app informa de forma visible si hay cambios pendientes de sincronizar o un error persistente.

## Criterios de éxito

- No existe una ruta que guarde solo la primera o la segunda lectura.
- Android permite completar el flujo sin Internet y sincroniza después sin duplicados.
- Android y web calculan exactamente el mismo resultado.
- Una eliminación offline termina reflejada en todos los clientes.
- CSV y PDF pueden generarse y compartirse manualmente desde Android y web sin alterar los datos canónicos.
- El producto puede desplegarse dentro de los niveles gratuitos elegidos.

## Migración histórica cerrada

Se inspeccionó `Tensión.xlsx` y se migraron únicamente las 70 mediciones individuales válidas. Fecha y hora se interpretaron en `Europe/Madrid` y se convirtieron a UTC; máxima, mínima y frecuencia se mapearon a los campos canónicos y `notes = null`. El preview y la verificación final dieron 0 duplicados, 0 conflictos y 0 errores. Fue una operación única: no se creó ni se necesita un importador permanente.
