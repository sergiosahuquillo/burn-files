# Data Model: Spike F0 — Validación de viabilidad

**Feature**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

Todo el estado persistente del spike es pequeño: un registro JSON Lines y un marcador del conjunto
sintético. No hay base de datos.

## DeviceInfo

Se calcula una vez por arranque y se incrusta en cada `TestResult`.

| Campo | Tipo | Regla |
|-------|------|-------|
| `matrixId` | enum `D1`…`D5`, `OTHER` | Elegido por el desarrollador en la primera ejecución; persistido en preferencias |
| `manufacturer` | string | `Build.MANUFACTURER` |
| `model` | string | `Build.MODEL` |
| `sdkInt` | int | `Build.VERSION.SDK_INT` |
| `release` | string | `Build.VERSION.RELEASE` |
| `fireOs` | string? | Versión de Fire OS si `ro.build.version.name` la indica; `null` si no es Fire TV |
| `uiMode` | enum `MOBILE`, `TV` | Según R3 |

## AccessLevel (enum)

`FULL` · `LEGACY_FULL` (API < 30 con lectura/escritura) · `MEDIA_ONLY` · `NONE`

## AccessAttempt

Una por paso de la cadena de fallback (FR-002).

| Campo | Tipo | Regla |
|-------|------|-------|
| `step` | enum `APP_SETTINGS`, `GENERIC_SETTINGS`, `ADB`, `RUNTIME_DIALOG`, `MEDIA_ONLY` | Orden fijo de R4 |
| `resolvable` | bool? | Resultado de `resolveActivity` (solo pasos de ajustes) |
| `outcome` | enum `OPENED`, `ACTIVITY_NOT_FOUND`, `SECURITY_EXCEPTION`, `GRANTED`, `DENIED`, `NO_EFFECT`, `SKIPPED` | |
| `levelAfter` | `AccessLevel` | Reevaluado en `ON_RESUME` |

**Transiciones de la cadena**:

```text
START ─► APP_SETTINGS ─(granted)──────────────────────────► DONE(FULL)
             │(not found / security / no effect)
             ▼
        GENERIC_SETTINGS ─(granted)───────────────────────► DONE(FULL)
             │(not found / security / no effect)
             ▼
            ADB ─(granted al volver a primer plano)────────► DONE(FULL)
             │(el desarrollador elige continuar sin acceso)
             ▼
        MEDIA_ONLY ───────────────────────────────────────► DONE(MEDIA_ONLY | NONE)

API < 30: START ─► RUNTIME_DIALOG ─(granted)─► DONE(LEGACY_FULL) / (denied)─► MEDIA_ONLY
```

## MediaCoverage

Solo en modo limitado (FR-005): `images`, `video`, `audio`, `downloads` → número de elementos visibles
(int ≥ 0).

## SyntheticDataset

Persistido en `BurnFilesSpike/dataset/dataset.json`.

| Campo | Tipo | Regla |
|-------|------|-------|
| `seed` | long | Por defecto `0xB0FF11E5` |
| `profile` | string | `default-100k` |
| `targetFiles` | int | ≥ 100.000 |
| `createdFiles` | int | Se actualiza por lotes |
| `folders` | int | |
| `totalBytes` | long | |
| `state` | enum `INCOMPLETE`, `COMPLETE`, `CLEANING` | `INCOMPLETE` al empezar; `COMPLETE` solo al terminar sin errores |

Regla: misma `seed` + mismo `profile` ⇒ mismo árbol (nombres, profundidades y tamaños). Verificado por
test unitario sobre el plan de generación, sin tocar disco.

## BenchmarkRun

| Campo | Tipo | Regla |
|-------|------|-------|
| `strategy` | enum `NIO_P1`, `NIO_P4`, `IO_P1`, `IO_P4`, `MEDIASTORE` | R5 |
| `dataset` | enum `SYNTHETIC`, `REAL_READ_ONLY` | |
| `repetition` | int 1–3 | |
| `cache` | enum `COLD`, `WARM` | |
| `durationMs` | long | |
| `files` · `folders` | int | |
| `bytes` | long | |
| `filesPerSecond` | double | Derivado |
| `outcome` | enum `OK`, `CANCELLED`, `ACCESS_LOST`, `ERROR` | |

**Agregado** `BenchmarkSummary` por estrategia: mediana y peor de `durationMs`, `filesPerSecond`.

## UpdateTest

| Campo | Tipo | Regla |
|-------|------|-------|
| `fromVersionCode` · `toVersionCode` | int | |
| `step` | enum `QUERY`, `DOWNLOAD`, `VERIFY_SHA256`, `VERIFY_CERT`, `INSTALL_PERMISSION`, `INSTALL` | |
| `outcome` | enum `OK`, `MISMATCH`, `NETWORK_ERROR`, `RATE_LIMITED`, `ACTIVITY_NOT_FOUND`, `USER_CANCELLED`, `FAILED` | |
| `downloadBytes` · `durationMs` | long | |
| `installerStatus` | int? | Código `PackageInstaller.STATUS_*` |

## UsbVolumeProbe

| Campo | Tipo | Regla |
|-------|------|-------|
| `probe` | enum `DIRECT`, `DOCUMENT_TREE` | R8 |
| `capacityBytes` · `freeBytes` | long | |
| `detect` · `write` · `readVerify` · `delete` | enum `OK`, `FAILED`, `READ_ONLY`, `VOLUME_REMOVED`, `ACTIVITY_NOT_FOUND`, `NOT_RUN` | Una por operación (FR-017) |
| `errorKind` | enum? | Clase de excepción normalizada (`EACCES`, `EROFS`, `ENOENT`, `IO`, `SECURITY`) |

El nombre del volumen **no** se registra (puede ser personal); se registra un índice ordinal.

## TestResult (entrada del registro)

Unión etiquetada: `DeviceInfo` + `timestamp` (ISO-8601 UTC) + `appVersionCode` + `test` (`ACCESS`,
`BENCHMARK`, `UPDATE`, `USB`) + el payload correspondiente. Esquema formal en
[contracts/results-log.schema.json](./contracts/results-log.schema.json).

**Validaciones**: ningún campo de texto libre; los únicos `string` son los de `DeviceInfo` y
`timestamp`. El test de saneado (FR-019) serializa entradas de todas las pruebas y comprueba que no
contienen `/`, `\`, `storage`, `emulated` ni nombres del conjunto sintético.

## SpikeReport (documento en el repo)

`docs/spike/f0-results.md`: tabla `DeviceInfo.matrixId × test` con el resultado clave de cada celda, la
tabla de `BenchmarkSummary` por dispositivo y una sección por riesgo con decisión
(`GO` / `GO_WITH_CHANGES` / `BLOCK`) y justificación (FR-021).
