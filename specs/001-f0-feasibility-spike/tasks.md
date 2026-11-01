---

description: "Task list for the F0 feasibility spike"
---

# Tasks: Spike F0 — Validación de viabilidad

**Input**: Design documents from `/specs/001-f0-feasibility-spike/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: incluidas porque la constitución (*Flujo de desarrollo y calidad*) exige tests unitarios de
la lógica de verificación y tests de UI de Compose para los flujos destructivos en móvil y TV.

**Organization**: tareas agrupadas por historia de usuario para implementarlas y probarlas por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)

## Path Conventions

Proyecto Gradle independiente en `spike/` (ver *Structure Decision* del plan). Abreviaturas:

- `$SRC` = `spike/app/src/main/kotlin/es/sergiosahuquillo/burnfiles/spike`
- `$TEST` = `spike/app/src/test/kotlin/es/sergiosahuquillo/burnfiles/spike`
- `$ATEST` = `spike/app/src/androidTest/kotlin/es/sergiosahuquillo/burnfiles/spike`
- `$RES` = `spike/app/src/main/res`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: proyecto compilable, calidad y CI desde el primer commit.

- [ ] T001 Crear el proyecto Gradle en `spike/` (`settings.gradle.kts`, `build.gradle.kts`, wrapper `gradlew`/`gradlew.bat`/`gradle/wrapper/`) y fijar en `spike/gradle/libs.versions.toml` las últimas versiones estables de Kotlin 2.x, AGP, Compose BOM, `androidx.tv:tv-material` 1.1.x, activity-compose, lifecycle y kotlinx-coroutines (R2)
- [ ] T002 Configurar `spike/app/build.gradle.kts`: `applicationId` `es.sergiosahuquillo.burnfiles.spike`, `minSdk` 26, `compileSdk`/`targetSdk` 36, Compose, JUnit 5 en unit tests (`useJUnitPlatform()`), `versionCode`/`versionName` desde las propiedades `spikeVersionCode`/`spikeVersionName` (por defecto 1 / `0.0.1`) y firma de la variante debug con `debug.keystore`
- [ ] T003 [P] Añadir `.gitattributes` en la raíz con `eol=lf` para `*.sh`, `gradlew`, `*.kts`, `*.kt`, `*.xml`, `*.md` y `eol=crlf` para `*.bat`
- [ ] T004 [P] Configurar ktlint (`org.jlleitschuh.gradle.ktlint`) y detekt con `spike/config/detekt/detekt.yml` en `spike/build.gradle.kts`
- [ ] T005 [P] Crear `tools/check_strings.py`: recibe un directorio `res/`, compara las claves de `string`, `plurals` y `string-array` entre `values/` y `values-es/` y sale con código ≠ 0 listando las que faltan en cada idioma
- [ ] T006 [P] Crear `.github/workflows/spike.yml` (sin GMS ni secretos): en cambios bajo `spike/` o `tools/`, JDK 17, `./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug` en `spike/`, `python tools/check_strings.py spike/app/src/main/res` y fallo si el APK debug supera 15 MB

**Checkpoint**: `./gradlew assembleDebug` genera un APK vacío y la CI pasa.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: shell de la app (móvil/TV), tema, i18n, `DeviceInfo` y registro de resultados, de los que dependen todas las historias.

**⚠️ CRITICAL**: ninguna historia puede empezar antes de terminar esta fase.

- [ ] T007 Crear `spike/app/src/main/AndroidManifest.xml`: `uses-feature` `android.software.leanback` y `android.hardware.touchscreen` con `required="false"`, `SpikeActivity` con categorías `LAUNCHER` y `LEANBACK_LAUNCHER`, `android:banner="@drawable/banner"`, `android:localeConfig`, `requestLegacyExternalStorage="true"`, permisos `READ/WRITE_EXTERNAL_STORAGE` (`maxSdkVersion` 29 para WRITE), `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES/VIDEO/AUDIO`, `REQUEST_INSTALL_PACKAGES`, `INTERNET`, y `<queries>` para los intents de ajustes de R4/R7
- [ ] T008 [P] Crear `$RES/drawable/banner.xml` (320×180 dp vector), `$RES/xml/locales_config.xml` (`en`, `es`) y `$RES/xml/file_paths.xml` (`files-path` `results/`) con su `FileProvider` en el manifest
- [ ] T009 [P] Crear `$RES/values/strings.xml` y `$RES/values-es/strings.xml` con las claves de Home, navegación, botones comunes y los diálogos destructivos; plurales para conteos de archivos
- [ ] T010 [P] Implementar tokens de color claro/oscuro y tipografía compartidos en `$SRC/ui/theme/Tokens.kt`, y los temas `$SRC/ui/theme/MobileTheme.kt` (Material 3) y `$SRC/ui/theme/TvTheme.kt` (`androidx.tv.material3.MaterialTheme`) alimentados por esos tokens; TV en oscuro por defecto
- [ ] T011 [P] Implementar `$SRC/device/DeviceInfo.kt` y `$SRC/device/DeviceDetector.kt`: `matrixId` enum `D1`…`D5`, `OTHER` (persistido en `SharedPreferences`), `manufacturer`, `model`, `sdkInt`, `release`, `fireOs` (`null` si no es Fire TV) y `uiMode` `MOBILE`/`TV` según `UiModeManager` con respaldo `FEATURE_LEANBACK` (R3)
- [ ] T012 [P] Implementar el modelo cerrado del registro en `$SRC/log/TestResult.kt` según [results-log.schema.json](./contracts/results-log.schema.json) (`schemaVersion` 1, `timestamp` ISO-8601 UTC, payloads `ACCESS`/`BENCHMARK`/`UPDATE`/`USB`, solo enums y números salvo `DeviceInfo`) y su serialización JSON con `org.json`
- [ ] T013 Implementar `$SRC/log/ResultsLog.kt`: append de una línea por `TestResult` en `filesDir/results/results.jsonl` en `Dispatchers.IO`, lectura como `Flow<List<TestResult>>`
- [ ] T014 [P] Test unitario `$TEST/log/TestResultSerializationTest.kt`: ida y vuelta de cada tipo de payload y conformidad de campos/enums con el esquema
- [ ] T015 [P] Test unitario `$TEST/log/ResultsPrivacyTest.kt` (FR-019): serializar entradas de todas las pruebas y comprobar que no contienen `/`, `\`, `storage`, `emulated` ni nombres del conjunto sintético
- [ ] T016 Implementar `$SRC/SpikeApp.kt` (contenedor manual de dependencias) y `$SRC/SpikeActivity.kt` con navegación entre Home, Access, Benchmark, Update, Usb y Results, eligiendo en runtime `$SRC/ui/mobile/MobileShell.kt` (lista de tarjetas + barra superior) o `$SRC/ui/tv/TvShell.kt` (`NavigationDrawer`, overscan 5 %, foco inicial según [ui-navigation.md](./contracts/ui-navigation.md))
- [ ] T017 Implementar la selección de `matrixId` en el primer arranque en `$SRC/ui/mobile/MatrixPickerScreen.kt` y `$SRC/ui/tv/MatrixPickerScreen.kt`
- [ ] T018 [P] Implementar `$SRC/ui/common/DestructiveConfirmDialog.kt` (móvil y TV): muestra nº de elementos (`<plurals>`) y tamaño (`Formatter.formatShortFileSize`), foco/botón por defecto en «Cancelar», confirmación solo con «Eliminar»

**Checkpoint**: la app arranca en móvil y TV, se elige el dispositivo de la matriz y se navega a cinco pantallas vacías con el mando y con táctil.

---

## Phase 3: User Story 1 - Diagnóstico de acceso al almacenamiento (Priority: P1) 🎯 MVP

**Goal**: obtener el máximo nivel de acceso con la cadena de fallback y registrar nivel y vía.

**Independent Test**: en D1–D4, ejecutar Access › Iniciar diagnóstico y obtener una entrada `ACCESS` con `finalLevel` y los `attempts` (quickstart V1, V2).

### Tests for User Story 1

- [ ] T019 [P] [US1] Test unitario `$TEST/access/AccessChainTest.kt` de la máquina de estados de [data-model.md](./data-model.md#accessattempt): orden `APP_SETTINGS` → `GENERIC_SETTINGS` → `ADB` → `MEDIA_ONLY` en API ≥ 30, `RUNTIME_DIALOG` en API < 30, transición por `ACTIVITY_NOT_FOUND`, `SECURITY_EXCEPTION` y `NO_EFFECT`, y `GRANTED` detectado en reevaluación

### Implementation for User Story 1

- [ ] T020 [P] [US1] Implementar `$SRC/access/AccessLevel.kt` y `$SRC/access/AccessChecker.kt`: `FULL` · `LEGACY_FULL` · `MEDIA_ONLY` · `NONE` con `Environment.isExternalStorageManager()` (API ≥ 30) o `checkSelfPermission` (API < 30)
- [ ] T021 [US1] Implementar `$SRC/access/AccessChain.kt` (lógica pura, sin Android) con los pasos y `outcome` `OPENED`, `ACTIVITY_NOT_FOUND`, `SECURITY_EXCEPTION`, `GRANTED`, `DENIED`, `NO_EFFECT`, `SKIPPED` (R4)
- [ ] T022 [US1] Implementar `$SRC/access/SettingsLauncher.kt`: lanza `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` (con `package:` URI) y `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`, registra `resolveActivity` como `resolvable` y captura `ActivityNotFoundException`/`SecurityException` (FR-003)
- [ ] T023 [P] [US1] Implementar `$SRC/access/MediaCoverageProbe.kt`: cuenta imágenes, vídeo, audio y descargas visibles en `MediaStore` en `Dispatchers.IO` (FR-005)
- [ ] T024 [US1] Implementar `$SRC/access/AccessViewModel.kt`: reevalúa el nivel en cada `ON_RESUME` (FR-001), avanza la cadena, pide permisos runtime en API < 30 y escribe la entrada `ACCESS` en `ResultsLog`
- [ ] T025 [P] [US1] Implementar `$SRC/ui/mobile/AccessScreen.kt`: pasos, comando ADB `adb shell appops set --uid es.sergiosahuquillo.burnfiles.spike MANAGE_EXTERNAL_STORAGE allow` y botón «Copiar» (FR-004)
- [ ] T026 [P] [US1] Implementar `$SRC/ui/tv/AccessScreen.kt`: foco inicial «Iniciar diagnóstico», orden de arriba abajo, comando ADB legible a 3 m dentro del overscan, «Atrás» a Home
- [ ] T027 [US1] Añadir las cadenas de Access en `$RES/values/strings.xml` y `$RES/values-es/strings.xml`

**Checkpoint**: US1 funcional y probada sola; ya aporta el dato del riesgo más alto.

---

## Phase 4: User Story 5 - Informe de resultados y exportación (Priority: P1)

**Goal**: ver, exportar y recuperar por ADB los registros de cualquier prueba.

**Independent Test**: tras ejecutar US1, exportar el registro y validarlo contra el esquema (quickstart V9).

### Tests for User Story 5

- [ ] T028 [P] [US5] Test unitario `$TEST/log/ResultsSummaryTest.kt`: agregación de entradas en la vista resumida por prueba y resultado

### Implementation for User Story 5

- [ ] T029 [US5] Implementar `$SRC/log/ResultsExporter.kt`: compartir con `ACTION_SEND` vía `FileProvider` y copia a `Download/BurnFilesSpike/` (`MediaStore.Downloads` en API ≥ 29, archivo directo en API < 29) (FR-020)
- [ ] T030 [US5] Implementar `$SRC/log/ResultsViewModel.kt` con la vista resumida (`ResultsSummary`) y la acción de exportar
- [ ] T031 [P] [US5] Implementar `$SRC/ui/mobile/ResultsScreen.kt` y `$SRC/ui/tv/ResultsScreen.kt` (TV: foco inicial «Exportar», lista desplazable con D-pad, botón central expande la entrada)
- [ ] T032 [US5] Añadir las cadenas de Results en `$RES/values/strings.xml` y `$RES/values-es/strings.xml`

**Checkpoint**: US1 + US5 = MVP del spike (dato de acceso exportable de todos los dispositivos).

---

## Phase 5: User Story 2 - Benchmark de estrategias de recorrido (Priority: P2)

**Goal**: generar el conjunto sintético y medir las cinco configuraciones con mediana y peor.

**Independent Test**: en un dispositivo con acceso total, generar ≥ 100.000 archivos y obtener la tabla por estrategia (quickstart V3–V5, V12).

### Tests for User Story 2

- [ ] T033 [P] [US2] Test unitario `$TEST/benchmark/DatasetPlanTest.kt`: misma `seed` (por defecto `0xB0FF11E5`) + `profile` `default-100k` ⇒ mismo plan; `targetFiles` ≥ 100.000, profundidad 1–6, 97 % de tamaños en 0–2 KiB y 3 % en 2–64 KiB (R6), sin tocar disco
- [ ] T034 [P] [US2] Test unitario `$TEST/benchmark/TraversalTest.kt` sobre un árbol temporal en JVM: las cuatro estrategias de archivo cuentan los mismos `files`/`folders`/`bytes`, excluyen `Android/data` y `Android/obb` (FR-011) y respetan la cancelación
- [ ] T035 [P] [US2] Test unitario `$TEST/benchmark/BenchmarkStatsTest.kt`: mediana y peor de `durationMs` y `filesPerSecond` derivado
- [ ] T036 [P] [US2] Test de UI `$ATEST/benchmark/CleanupDialogTest.kt` en móvil y TV: el diálogo muestra nº de elementos y tamaño, «Cancelar» no borra nada y tiene el foco inicial en TV

### Implementation for User Story 2

- [ ] T037 [P] [US2] Implementar `$SRC/benchmark/DatasetPlan.kt` (plan determinista puro) y `$SRC/benchmark/DatasetMarker.kt` (`dataset.json` con `seed`, `profile`, `targetFiles`, `createdFiles`, `folders`, `totalBytes`, `state` `INCOMPLETE`/`COMPLETE`/`CLEANING`)
- [ ] T038 [US2] Implementar `$SRC/benchmark/DatasetGenerator.kt`: escribe el plan bajo `BurnFilesSpike/dataset/` en `Dispatchers.IO`, comprueba espacio libre (necesario + 10 %), progreso ≥ 1 Hz, cancelable, y detecta conjuntos `INCOMPLETE` para completar o limpiar
- [ ] T039 [P] [US2] Implementar `$SRC/benchmark/Traversal.kt` con `NIO_P1`, `NIO_P4`, `IO_P1`, `IO_P4` (`Dispatchers.IO.limitedParallelism(n)`, reparto por subcarpeta de primer nivel) y `$SRC/benchmark/MediaStoreTraversal.kt` (`MEDIASTORE`) (R5)
- [ ] T040 [US2] Implementar `$SRC/benchmark/BenchmarkRunner.kt`: 3 repeticiones por estrategia, `cache` `COLD`/`WARM`, solo `MEDIASTORE` en modo limitado, modo `REAL_READ_ONLY` sin escrituras, `outcome` `ACCESS_LOST` si se revoca el acceso, y una entrada `BENCHMARK` por ejecución
- [ ] T041 [US2] Implementar `$SRC/benchmark/DatasetCleaner.kt`: borra solo dentro de `BurnFilesSpike/dataset/` y solo si existe `dataset.json`; marca `CLEANING` antes de empezar (FR-010, *Complexity Tracking*)
- [ ] T042 [US2] Implementar `$SRC/benchmark/BenchmarkViewModel.kt` (progreso, cancelación < 2 s, confirmación de cancelar con «Atrás» durante una ejecución)
- [ ] T043 [P] [US2] Implementar `$SRC/ui/mobile/BenchmarkScreen.kt` y `$SRC/ui/tv/BenchmarkScreen.kt` (TV: foco inicial según haya o no conjunto, fila de acciones y tabla desplazable, foco a «Cancelar» durante la ejecución)
- [ ] T044 [US2] Añadir las cadenas de Benchmark (incluidos plurales de archivos/carpetas) en `$RES/values/strings.xml` y `$RES/values-es/strings.xml`

**Checkpoint**: US2 funcional y probada sola.

---

## Phase 6: User Story 3 - Instalación de una actualización descargada (Priority: P3)

**Goal**: pasar de la versión 0.0.1 a 0.0.2 desde una pre-release `spike-v*` verificada.

**Independent Test**: con la 0.0.1 instalada y `spike-v0.0.2` publicada, completar el flujo (quickstart V6, V7).

### Tests for User Story 3

- [ ] T045 [P] [US3] Test unitario `$TEST/update/ReleaseSelectionTest.kt`: parseo de la respuesta de releases, filtro por prefijo `spike-v`, `versionCode` = `major * 10000 + minor * 100 + patch` y elección de la mayor
- [ ] T046 [P] [US3] Test unitario `$TEST/update/Sha256VerifierTest.kt`: coincidencia, discrepancia y formato `sha256sum` del asset `.sha256`
- [ ] T047 [P] [US3] Test unitario `$TEST/update/CertificateMatcherTest.kt`: conjuntos de certificados iguales, distintos y vacíos

### Implementation for User Story 3

- [ ] T048 [P] [US3] Implementar `$SRC/update/GitHubReleasesClient.kt` con `HttpURLConnection` según [spike-release.md](./contracts/spike-release.md): cabeceras, `ETag`, `RATE_LIMITED` con `X-RateLimit-Reset`, redirecciones solo a `github.com` y `objects.githubusercontent.com` (FR-015)
- [ ] T049 [P] [US3] Implementar `$SRC/update/ReleaseSelector.kt`, `$SRC/update/Sha256Verifier.kt` y `$SRC/update/CertificateMatcher.kt` (lógica pura)
- [ ] T050 [US3] Implementar `$SRC/update/ApkDownloader.kt`: descarga a `cacheDir/updates/` con progreso, cancelable, borra el archivo ante cualquier fallo de verificación o cancelación
- [ ] T051 [US3] Implementar `$SRC/update/ApkInspector.kt`: `getPackageArchiveInfo` con `GET_SIGNING_CERTIFICATES` (API ≥ 28) o `GET_SIGNATURES` (API < 28); comprueba `packageName` y `versionCode` > instalado
- [ ] T052 [US3] Implementar `$SRC/update/SpikeInstaller.kt` y `$SRC/update/InstallResultReceiver.kt`: `canRequestPackageInstalls()`, `ACTION_MANAGE_UNKNOWN_APP_SOURCES` con `try/catch`, sesión `PackageInstaller` `MODE_FULL_INSTALL`, lanzamiento del `EXTRA_INTENT` ante `STATUS_PENDING_USER_ACTION`, sin `USER_ACTION_NOT_REQUIRED` (FR-014)
- [ ] T053 [US3] Implementar `$SRC/update/UpdateViewModel.kt`: pasos `QUERY` → `DOWNLOAD` → `VERIFY_SHA256` → `VERIFY_CERT` → `INSTALL_PERMISSION` → `INSTALL`, una entrada `UPDATE` por paso con su `outcome`
- [ ] T054 [P] [US3] Implementar `$SRC/ui/mobile/UpdateScreen.kt` y `$SRC/ui/tv/UpdateScreen.kt` (TV: foco inicial «Comprobar release de prueba», orden del flujo, «Atrás» con confirmación durante la descarga)
- [ ] T055 [US3] Añadir las cadenas de Update en `$RES/values/strings.xml` y `$RES/values-es/strings.xml`

**Checkpoint**: US3 funcional y probada sola.

---

## Phase 7: User Story 4 - Escritura en USB desde TV (Priority: P4)

**Goal**: resultado por operación (detectar, escribir, leer y verificar, eliminar) en ambas sondas.

**Independent Test**: con un pendrive en D2, ejecutar la prueba (quickstart V8).

### Tests for User Story 4

- [ ] T056 [P] [US4] Test unitario `$TEST/usb/UsbProbeTest.kt` sobre un directorio temporal en JVM: secuencia completa, `READ_ONLY`, `VOLUME_REMOVED` a mitad de escritura y normalización de `errorKind` (`EACCES`, `EROFS`, `ENOENT`, `IO`, `SECURITY`)

### Implementation for User Story 4

- [ ] T057 [P] [US4] Implementar `$SRC/usb/VolumeLister.kt`: `getStorageVolumes()` filtrando `isRemovable`, raíz por `getDirectory()` (API ≥ 30) o derivada de `getExternalFilesDirs(null)` (API < 30), capacidad y libre con `StatFs`; el nombre del volumen solo se muestra, se registra `volumeIndex`
- [ ] T058 [US4] Implementar `$SRC/usb/DirectProbe.kt`: `BurnFilesSpike-probe-<uuid>.bin` de 64 KiB en la raíz, lectura y comparación SHA-256, borrado del archivo recién creado (FR-017)
- [ ] T059 [US4] Implementar `$SRC/usb/DocumentTreeProbe.kt`: `ACTION_OPEN_DOCUMENT_TREE` con `try/catch` (`ACTIVITY_NOT_FOUND`), misma secuencia con `DocumentFile`
- [ ] T060 [US4] Implementar `$SRC/usb/UsbViewModel.kt`: una entrada `USB` por sonda; sin volúmenes, no ofrece ejecutar la prueba
- [ ] T061 [P] [US4] Implementar `$SRC/ui/mobile/UsbScreen.kt` y `$SRC/ui/tv/UsbScreen.kt` (TV: foco inicial en el primer volumen o «Volver a buscar»)
- [ ] T062 [US4] Añadir las cadenas de Usb en `$RES/values/strings.xml` y `$RES/values-es/strings.xml`

**Checkpoint**: las cinco historias funcionan por separado.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T063 [P] Revisar todas las pantallas en tema claro y oscuro y en en/es en D1 y D4; corregir contrastes por debajo de WCAG AA (quickstart V11)
- [ ] T064 [P] Recorrido completo solo con mando en D2–D4 contra [ui-navigation.md](./contracts/ui-navigation.md) (quickstart V10)
- [ ] T065 Compilar 0.0.1 y 0.0.2, generar los `.sha256` y publicar la pre-release `spike-v0.0.2` según [spike-release.md](./contracts/spike-release.md) (requiere confirmación del desarrollador antes de publicar)
- [ ] T066 Ejecutar los escenarios V1–V12 del [quickstart.md](./quickstart.md) en D1–D4 (y D5 si se dispone) y recuperar los `results-<matrixId>.jsonl`
- [ ] T067 Redactar `docs/spike/f0-results.md`: tabla dispositivo × prueba, resumen del benchmark por dispositivo, decisión `GO` / `GO_WITH_CHANGES` / `BLOCK` por riesgo con justificación y huecos conocidos de [research.md](./research.md#huecos-conocidos-se-trasladan-al-informe) (FR-021)
- [ ] T068 Actualizar `docs/plan-del-proyecto.md` y, si procede, proponer enmiendas a la constitución según las decisiones del informe; actualizar la sección *Comandos* de `CLAUDE.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las historias.
- **US1, US5, US2, US3, US4 (Phases 3–7)**: dependen solo de Foundational; entre sí son independientes.
  US5 se puede probar con cualquier otra historia como fuente de datos (o con entradas de ejemplo).
- **Polish (Phase 8)**: T063–T064 tras las historias que se quieran revisar; T065 tras US3; T066 tras
  todas; T067 tras T066; T068 tras T067.

### Within Each User Story

- Tests primero (deben fallar), luego lógica pura, luego integración Android, ViewModel, pantallas y cadenas.
- Las pantallas móvil y TV de una misma historia se pueden hacer en paralelo.

### Parallel Opportunities

- Setup: T003–T006 en paralelo tras T001–T002.
- Foundational: T008–T012, T014, T015 y T018 en paralelo; T013 tras T012; T016 tras T010–T011; T017 tras T016.
- Con Foundational terminado, cada historia puede ir en paralelo con las demás (comparten solo los
  archivos de cadenas: coordinar T027, T032, T044, T055 y T062).

## Parallel Example: User Story 2

```text
# Tests de US2 en paralelo:
T033 DatasetPlanTest · T034 TraversalTest · T035 BenchmarkStatsTest · T036 CleanupDialogTest

# Implementación en paralelo (archivos distintos):
T037 DatasetPlan + DatasetMarker · T039 Traversal + MediaStoreTraversal · T043 BenchmarkScreen (móvil y TV)
```

## Implementation Strategy

### MVP First (US1 + US5)

1. Phase 1 Setup → Phase 2 Foundational.
2. Phase 3 (US1) y Phase 4 (US5).
3. **Parar y validar**: instalar en D1–D4, ejecutar V1, V2 y V9. Con esto ya se conoce el riesgo más alto.

### Incremental Delivery

1. MVP → instalar en la matriz.
2. US2 (benchmark) → V3–V5, V12.
3. US3 (update) → publicar pre-release → V6–V7. A partir de aquí las nuevas builds del spike pueden
   llegar a los dispositivos por la propia prueba de actualización.
4. US4 (USB) → V8.
5. Polish → informe y puerta F0 → F1.

## Notes

- `[P]` = archivos distintos, sin dependencias pendientes.
- Commit por tarea o grupo lógico en formato Conventional Commits (`feat(spike): …`, `test(spike): …`).
- T065 publica contenido en GitHub: pedir confirmación antes de ejecutarlo.
- Ninguna tarea toca rutas fuera de `spike/`, `tools/`, `.github/`, `docs/` y `.gitattributes`.
