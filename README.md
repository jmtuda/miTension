# miTension

Aplicación Android y web para registrar y consultar mediciones personales de tensión arterial.

## Estado actual

El MVP está completado y operativo. La web desplegada en Vercel y el APK Android
han superado la validación E2E contra el mismo proyecto real de Supabase.

## Aplicación web

Requiere Node.js y las variables públicas `VITE_SUPABASE_URL` y
`VITE_SUPABASE_PUBLISHABLE_KEY`; consulta `web/.env.example`. Desde `web/`:

```text
npm install
npm run dev
```

La integración real usa la sesión persistente de Supabase Auth y las políticas
RLS existentes. Las pruebas automatizadas usan dobles de la API y no requieren
ni almacenan credenciales de Supabase.

## Documentación

- [Proyecto](docs/PROJECT.md)
- [Arquitectura](docs/ARCHITECTURE.md)
- [Modelo de datos](docs/DATA_MODEL.md)
- [Backlog](docs/BACKLOG.md)
- [Estado actual](docs/STATUS.md)
- [Despliegue y prueba E2E](docs/DEPLOYMENT.md)
