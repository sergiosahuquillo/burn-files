# Implementation Plan: Spike F0 — Validación de viabilidad

**Branch**: `001-f0-feasibility-spike` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-f0-feasibility-spike/spec.md`

## Summary

Herramienta de diagnóstico Android (una APK para móvil y TV) que valida los cuatro supuestos de mayor
riesgo antes de F1: (1) obtención de acceso total al almacenamiento con la cadena de fallback de la
constitución, (2) rendimiento de tres estrategias de recorrido sobre un conjunto sintético de 100.000
archivos, (3) descarga, verificación e instalación de una APK desde una pre-release de GitHub con
`PackageInstaller`, y (4) escritura en un pendrive USB desde TV. Cada prueba deja un registro JSON Lines
sin datos personales que se consolida en `docs/spike/f0-results.md` con una decisión por riesgo. Vive en
un proyecto Gradle propio (`spike/`) para no condicionar la arquitectura de F1.

## Technical Context

**Language/Version**: Kotlin 2.x (estable más reciente en T001), JDK 17

**Primary Dependencies**: Jetpack Compose (BOM estable), Material 3, `androidx.tv:tv-material` 1.1.x,
`androidx.activity:activity-compose`, `androidx.lifecycle` (ViewModel + runtime-compose), kotlinx-coroutines.
Sin Hilt, Room, DataStore ni librería HTTP (ver *Complexity Tracking*).

**Storage**: JSON Lines en `filesDir/results/`; marcador `dataset.json` del conjunto sintético;
`SharedPreferences` para el identificador de matriz.

**Testing**: JUnit 5 (JVM) para lógica pura; Compose UI test instrumentado para los diálogos destructivos
en emulador móvil y TV; validación manual según [quickstart.md](./quickstart.md).

**Target Platform**: Android `minSdk` 26, `targetSdk`/`compileSdk` 36; móvil y Android TV / Fire OS 7+.

**Project Type**: mobile-app (herramienta de diagnóstico, no distribuida a usuarios finales)

**Performance Goals**: medir, no optimizar. Referencias de la constitución: 200.000 archivos < 30 s
(móvil) y < 60 s (TV). Progreso ≥ 1 Hz; cancelación < 2 s.

**Constraints**: cero I/O en el hilo principal; sin GMS; red solo hacia GitHub; APK < 15 MB; sin acceso a
`Android/data`/`Android/obb`; registro sin rutas ni nombres de archivo.

**Scale/Scope**: 6 pantallas (Home, Access, Benchmark, Update, Usb, Results), 4 pruebas, matriz de 4–5
dispositivos, ~1 semana de trabajo.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Estado | Cómo se cumple |
|-----------|--------|----------------|
| I. Paridad móvil y TV | ✅ | Una APK, UI elegida en runtime (R3); criterios de foco por pantalla en [ui-navigation.md](./contracts/ui-navigation.md); matriz D1–D4 obligatoria (D5 opcional) |
| II. Seguridad del borrado | ⚠️ Justificado | No toca archivos del usuario. Dos borrados definitivos sin papelera: limpieza del conjunto sintético y archivo sonda USB, ambos creados por el propio spike → *Complexity Tracking* |
| III. Perímetro del sistema | ✅ | Solo almacenamiento compartido y volúmenes extraíbles; exclusión explícita de `Android/data`/`obb` en los recorridos (FR-011); sin root ni Shizuku; no se usa el directorio de la app en el USB (R8) |
| IV. Degradación de permisos | ✅ | Cadena de fallback exacta de la constitución (R4); `try/catch` en todo intent a ajustes; reevaluación en `ON_RESUME` |
| V. i18n y temas | ✅ | `values/` + `values-es/`, `<plurals>`, formato por locale, tokens de tema claro/oscuro, `tools/check_strings.py` en CI (R10) |
| VI. Independencia de Google y privacidad | ✅ | Sin GMS; red solo GitHub API/descargas (contrato [spike-release.md](./contracts/spike-release.md)); sin Sentry ni telemetría; registro de tipos cerrados verificado por test (R9) |
| VII. Rendimiento | ✅ | Todo I/O en `Dispatchers.IO`, cancelable, progreso ≥ 1 Hz; el propio benchmark mide los objetivos; presupuesto de APK comprobado en el quickstart |
| Restricciones técnicas | ⚠️ Justificado | Kotlin, Compose, Material 3, tv-material, MVVM+UDF, `minSdk` 26, `targetSdk` 36 ✅. Sin Hilt/Room/DataStore/WorkManager y un solo módulo → *Complexity Tracking* |
| Distribución | ✅ | La release de prueba es pre-release con tag `spike-v*`, que no colisiona con `vX.Y.Z` ni con el updater de producto |
| Flujo y calidad | ✅ | Spec → plan → tareas; ktlint + detekt; tests unitarios de la lógica de verificación (antesala del updater) y de UI para los flujos destructivos; Conventional Commits |

**Resultado (pre-research)**: PASA con dos excepciones justificadas.
**Resultado (post-design)**: PASA. El diseño de Phase 1 no introduce nuevas excepciones; el contrato
del registro cierra el principio VI y el contrato de UI concreta el principio I.

## Project Structure

### Documentation (this feature)

```text
specs/001-f0-feasibility-spike/
├── plan.md              # Este archivo
├── research.md          # Phase 0: decisiones R1–R11
├── data-model.md        # Phase 1: entidades y transiciones
├── quickstart.md        # Phase 1: guía de validación V1–V12
├── contracts/
│   ├── results-log.schema.json   # Formato del registro exportado
│   ├── spike-release.md          # Release de prueba en GitHub
│   └── ui-navigation.md          # Pantallas, foco TV y diálogos destructivos
├── checklists/
│   └── requirements.md  # Checklist de calidad de la spec
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
spike/                                   # Proyecto Gradle independiente
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── gradlew, gradlew.bat, gradle/wrapper/
├── config/detekt/detekt.yml
└── app/
    ├── build.gradle.kts                 # applicationId es.sergiosahuquillo.burnfiles.spike
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml      # leanback/touchscreen opcionales, LEANBACK_LAUNCHER, banner
        │   ├── kotlin/es/sergiosahuquillo/burnfiles/spike/
        │   │   ├── SpikeActivity.kt     # Selección móvil/TV
        │   │   ├── SpikeApp.kt          # Contenedor manual de dependencias
        │   │   ├── device/              # DeviceInfo, detección TV / Fire OS
        │   │   ├── log/                 # TestResult, ResultsLog (JSONL), exportación
        │   │   ├── access/              # Cadena de fallback, AccessViewModel
        │   │   ├── benchmark/           # Generador, estrategias, estadísticas, ViewModel
        │   │   ├── update/              # Cliente GitHub, verificación, PackageInstaller
        │   │   ├── usb/                 # Volúmenes, sondas DIRECT / DOCUMENT_TREE
        │   │   └── ui/
        │   │       ├── theme/           # Tokens claro/oscuro compartidos
        │   │       ├── mobile/          # Composables Material 3
        │   │       └── tv/              # Composables tv-material
        │   └── res/
        │       ├── values/strings.xml   # en (base)
        │       ├── values-es/strings.xml
        │       ├── xml/locales_config.xml, xml/file_paths.xml
        │       └── drawable/banner.xml  # 320×180
        ├── test/kotlin/…                # JUnit 5
        └── androidTest/kotlin/…         # Compose UI tests (diálogos destructivos)

tools/check_strings.py                   # Paridad de claves en/es (reutilizable en F1)
.github/workflows/spike.yml              # CI: ktlint, detekt, unit tests, assembleDebug, strings
docs/spike/f0-results.md                 # Informe consolidado (US5)
```

**Structure Decision**: proyecto aparte en `spike/` con un único módulo `:app` organizado por paquetes
de dominio que imitan los futuros módulos (`access` ≈ `core:fs/permisos`, `update` ≈ `feature:updater`),
de modo que lo que se promueva en F1 tenga un destino claro. `tools/` y `.github/` quedan en la raíz
porque los reutilizará el producto.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Borrado definitivo sin papelera del conjunto sintético (principio II) | Son ~100.000 archivos creados por el propio spike en su carpeta `BurnFilesSpike/dataset/`; hay que eliminarlos para devolver el espacio en TVs de 8 GB | No hay papelera en el spike (es funcionalidad de F2); mandarlos a una papelera conservaría ~200 MiB durante 7 días. Mitigación: confirmación con nº de elementos y tamaño, borrado restringido a la raíz del spike y marcador `dataset.json` obligatorio para operar |
| Borrado del archivo sonda en el USB (principio II) | La prueba debe verificar que el borrado funciona en el volumen | Dejarlo ensuciaría el pendrive del usuario. Nombre único `BurnFilesSpike-probe-<uuid>.bin` y solo se borra el archivo que acaba de crearse |
| Sin Hilt, Room, DataStore ni WorkManager; un solo módulo (restricciones técnicas) | Herramienta desechable de ~5 pantallas sin trabajo en segundo plano ni datos relacionales | Montar la arquitectura completa en el spike adelantaría decisiones de F1 que dependen de sus resultados y multiplicaría el tiempo de una fase de 1 semana. El código promovido a producto se adaptará a Hilt/Room en su propia spec |
