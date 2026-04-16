# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos principales

```bash
# Desarrollo
pnpm start:dev        # Modo watch
pnpm start:debug      # Con debugging

# Build
pnpm build            # Limpia dist/ y compila con nest build

# Tests
pnpm test             # Jest unitario
pnpm test:watch       # Jest en modo watch
pnpm test:cov         # Reporte de cobertura
pnpm test:e2e         # Tests end-to-end (config: test/jest-e2e.json)

# Test de un solo archivo
pnpm test -- --testPathPattern=gateway.service

# Linting y formato
pnpm lint             # ESLint con fix automático
pnpm format           # Prettier sobre src/ y test/
```

## Stack tecnológico

- **Framework**: NestJS 11 + TypeScript 5.6
- **Base de datos**: MongoDB con Mongoose ODM
- **Autenticación**: JWT (Passport) + API Keys (hash bcrypt) + Turnstile CAPTCHA (Cloudflare)
- **Colas async**: Bull (SMS queue, Webhook delivery, Billing notifications) — requiere Redis
- **Email**: Nodemailer con templates Handlebars (en `src/mail/templates/`)
- **Push notifications**: Firebase Cloud Messaging (FCM)
- **Pagos**: Polar.sh
- **Rate limiting**: ThrottlerByIpGuard
- **Package manager**: pnpm

## Arquitectura de módulos

El API es una puerta de enlace SMS que usa dispositivos Android como gateways. Estructura de `src/`:

```
auth/        # JWT, API Keys, Password Reset, Email Verification, Access Logs
users/       # Gestión de usuarios, roles (REGULAR/ADMIN)
gateway/     # Core: Devices, SMS, SMSBatch, colas Bull, cron tasks
webhook/     # Subscripciones y entrega de webhooks con reintentos
billing/     # Polar.sh: planes, suscripciones, checkouts, notificaciones
mail/        # Servicio de email con templates compilados en build
support/     # Tickets de soporte
common/      # Turnstile CAPTCHA service
```

### Configuración de entrada (main.ts)

- Prefijo global: `/api`
- Versionado URI: `/api/v1/...`
- Swagger en `/` con autenticación persistente
- Firebase Admin SDK inicializado en arranque
- CORS habilitado, body parser limit 2mb

### Módulo Gateway (core)

Schemas principales: `Device`, `SMS`, `SMSBatch`, `DeviceTombstone`

La cola Bull de SMS se activa con `USE_SMS_QUEUE=true` y es configurable con `SMS_QUEUE_LIMITER` (max/duration). Las cron tasks `SmsStatusUpdateTask` y `HeartbeatCheckTask` gestionan el estado de dispositivos y mensajes.

### Autenticación

Tres mecanismos de auth combinables:
1. **JWT Bearer**: Guard `JwtAuthGuard` / `AuthGuard`
2. **API Keys**: Almacenadas en BD, verificadas con bcrypt
3. **OptionalAuthGuard**: Permite requests sin auth (datos públicos)

## Variables de entorno clave

Revisar `.env.example` para la lista completa. Las críticas son:

```env
PORT=3001
MONGO_URI=mongodb://user:pass@host:port/db?authSource=admin
JWT_SECRET=...
FRONTEND_URL=http://localhost:3000

# Firebase (FCM para dispositivos Android)
FIREBASE_PROJECT_ID=...
FIREBASE_PRIVATE_KEY=...   # Los \n deben convertirse a saltos de línea reales

# SMS Queue (opcional, requiere Redis)
USE_SMS_QUEUE=false
REDIS_URL=redis://localhost:6379

# Webhooks
WEBHOOK_DELIVERY_TIMEOUT_MS=30000

# Email
MAIL_HOST=...
MAIL_USER=...
MAIL_PASS=...

# Pagos
POLAR_ACCESS_TOKEN=...
```

## Docker

El stack completo se levanta desde la raíz del proyecto (no desde `api/`):

```bash
# Desde /Users/ernestosr/Projects/textbee/
docker-compose up -d
```

Servicios: `textbee-db` (MongoDB:27017), `mongo-express` (UI:8081), `textbee-api` (3001), `textbee-web` (3000), `textbee-redis` (6379).

El Dockerfile usa multi-stage build: deps → builder (`nest build`) → runner (prod sin devDeps, usuario no-root `nestjs:nodejs`).

## Convenciones del código

- **Prettier**: `singleQuote: true`, `trailingComma: 'all'`, `semi: false`
- Los templates de email (Handlebars) en `src/mail/templates/` se copian a `dist/` como assets en el build (ver `nest-cli.json`)
- Los schemas Mongoose usan `timestamps: true` por convención
- Los módulos Bull comparten la instancia Redis configurada en `BullModule.forRoot`
