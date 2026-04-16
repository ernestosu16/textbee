# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev        # Start development server (http://localhost:3000)
npm run build      # Production build
npm run start      # Start production server
npm run lint       # Run ESLint
```

There are no test commands — the project has no test suite configured.

## Architecture Overview

This is the **web frontend** of the textbee.dev SMS gateway platform, built with Next.js 14 App Router. The backend API runs separately (default: `http://localhost:3001/api/v1`).

### Route Structure

Uses Next.js App Router with route groups:

- `app/(app)/(auth)/` — Public auth pages (login, register, reset-password, verify-email, logout)
- `app/(app)/dashboard/` — Protected dashboard (devices, API keys, webhooks, account, billing)
- `app/api/auth/[...nextauth]/` — NextAuth.js route handler

Middleware at `middleware.ts` guards `/app/dashboard` routes, redirecting unauthenticated users to login.

### Authentication

NextAuth.js with JWT strategy. Three providers: email/password login, email/password registration, and Google OAuth (ID token flow). Custom session extends the default to include `id`, `role`, `phone`, `avatar`, and `accessToken` fields.

### HTTP Clients

Two Axios instances in `lib/`:
- `httpBrowserClient.ts` — Uses `getSession()` for client-side requests
- `httpServerClient.ts` — Uses `getServerSession()` for server components; uses internal Docker DNS (`textbee-api:3001`) when `CONTAINER_RUNTIME=docker`

Both inject `Authorization: Bearer <token>` automatically. The browser client caches session lookups for 2 minutes to reduce overhead.

### Data Fetching

React Query (TanStack Query v5) is the primary data fetching layer. Convention:
- `useQuery` for reads (devices, API keys, webhooks, messages)
- `useMutation` + `useQueryClient` for writes with cache invalidation

All API endpoint strings are defined in `config/api.ts`.

### Forms

React Hook Form + Zod. Schemas live in `lib/schemas.ts`. Use `useFieldArray` for dynamic form fields (e.g., bulk SMS recipients).

### UI Layer

- **Radix UI** primitives in `components/ui/` — accessible, unstyled base components
- **Tailwind CSS** for styling; custom brand palette based on amber colors; dark mode via `class` strategy with `next-themes`
- **Lucide React** for icons
- Shared app-level components in `components/shared/`
- Page-specific components are co-located inside `(components)/` subdirectories within each feature folder

### Error Handling

`formatError()` from `lib/utils/errorHandler.ts` normalizes API errors. User feedback is delivered via toast notifications (see `hooks/use-toast.ts`).

## Key Environment Variables

| Variable | Purpose |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | Backend API URL (default: `http://localhost:3001/api/v1`) |
| `NEXT_PUBLIC_SITE_URL` | Frontend base URL |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID` | Google OAuth |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | Cloudflare Turnstile CAPTCHA |
| `AUTH_SECRET` | NextAuth JWT secret |
| `DATABASE_URL` | MongoDB connection string |
| `MAIL_*` | SMTP configuration (HOST, PORT, USER, PASS, FROM) |
| `CONTAINER_RUNTIME` | Set to `"docker"` to use internal API DNS |

## TypeScript Configuration

Strict mode is **disabled** (`strict: false`, `strictNullChecks: false`). Path alias `@/*` maps to the repo root. The build allows JS files (`allowJs: true`).

## Docker / Standalone Mode

`next.config.js` sets `output: 'standalone'` for Docker deployments. The server-side HTTP client automatically switches to the internal Docker hostname when `CONTAINER_RUNTIME=docker`.
