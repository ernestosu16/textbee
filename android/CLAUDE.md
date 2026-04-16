# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos principales

```bash
# Build
./gradlew assembleDevDebug        # APK de desarrollo (debug)
./gradlew assembleProdDebug       # APK de producción (debug)
./gradlew assembleRelease         # APK de producción (release)

# Tests unitarios
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests com.vernu.sms.ExampleUnitTest

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vernu.sms.ExampleInstrumentedTest

# Lint
./gradlew lint
```

## Build Flavors

| Flavor | Package | API Base URL |
|--------|---------|--------------|
| `dev`  | `com.vernu.sms.dev` | `https://api.dev.textbee.dev/api/v1/` |
| `prod` | `com.vernu.sms` | `https://api.textbee.dev/api/v1/` |

`ENVIRONMENT` y `API_BASE_URL` se inyectan como `buildConfigField` según el flavor activo.

## Arquitectura

TextBee Android es una puerta de enlace SMS que reenvía mensajes entre dispositivos Android y un backend REST. El flujo principal tiene dos direcciones:

**SMS entrante → API:**
`SMSBroadcastReceiver` → `SMSReceivedWorker` (WorkManager) → `GatewayApiService` (Retrofit)

**API → SMS saliente:**
`FCMService` (Firebase Cloud Messaging) → `SmsSendWorker` (WorkManager) → `SMSHelper`

### Paquetes clave

- **`activities/`** — `MainActivity` (configuración y registro del dispositivo), `SMSFilterActivity`
- **`receivers/`** — `SMSBroadcastReceiver` (con deduplicación MD5), `SMSStatusReceiver`, `BootCompletedReceiver`
- **`workers/`** — `SmsSendWorker`, `SMSReceivedWorker`, `SMSStatusUpdateWorker`, `HeartbeatWorker`
- **`services/`** — `FCMService` (recibe comandos de envío), `StickyNotificationService`
- **`helpers/`** — `SharedPreferenceHelper` (wrapper de toda la config persistente), `SMSHelper` (multi-SIM), `SMSFilterHelper`, `HeartbeatManager`
- **`dtos/`** — DTOs para Retrofit (registro, SMS, heartbeat, SIM info)
- **`ApiManager.java`** — Singleton Retrofit configurado con interceptores

### Decisiones de diseño importantes

**Deduplicación de SMS:** `SMSBroadcastReceiver` genera un fingerprint MD5 de `sender + message + timestamp`. Un cache en memoria con TTL de 5 segundos descarta duplicados que Android emite a veces.

**Multi-SIM:** `SmsSendWorker` resuelve la SIM a usar con prioridad: instrucción del backend > preferencia guardada en app > SIM por defecto del dispositivo.

**Configuración dinámica via SharedPreferences:** Device ID, API key, feature flags (`gatewayEnabled`, `receiveSmsEnabled`, `stickyNotificationEnabled`), delay entre SMS enviados, e intervalo de heartbeat. Todo gestionado a través de `SharedPreferenceHelper`.

**WorkManager para resiliencia:** Todas las operaciones de red y envío de SMS pasan por WorkManager para garantizar ejecución aunque la app se cierre.

## Dependencias principales

- **Retrofit 2.9.0** + Gson — cliente HTTP
- **Firebase** (BOM 29.2.1) — FCM, Crashlytics, Analytics
- **WorkManager 2.7.1** — background tasks persistentes
- **ZXing 4.1.0** — escaneo de QR para configuración rápida
- Min SDK: 24 (Android 7.0) / Target SDK: 32 (Android 12)
