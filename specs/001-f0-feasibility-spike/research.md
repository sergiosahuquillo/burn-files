# Research: Spike F0 — Validación de viabilidad

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fecha**: 2026-11-01

Cada apartado sigue el formato *Decisión / Motivo / Alternativas*. No quedan `NEEDS CLARIFICATION`.

---

## R1. Ubicación y forma del spike en el repositorio

- **Decisión**: proyecto Gradle independiente en `spike/` (con su propio `settings.gradle.kts` y wrapper),
  un único módulo `:app`, `applicationId` `es.sergiosahuquillo.burnfiles.spike`.
- **Motivo**: el producto (F1) definirá su propia estructura multi-módulo (`app`, `core:*`,
  `feature:*`); mezclar el spike en ese build obligaría a decidir la arquitectura antes de tener los
  datos que el spike debe aportar. Un proyecto aparte se puede borrar o archivar sin tocar el producto,
  y un `applicationId` propio evita conflictos de firma con la futura app.
- **Alternativas**: módulo `:spike` dentro del build raíz (acopla el spike al diseño de F1);
  repositorio separado (pierde la trazabilidad spec → informe en el mismo repo).

## R2. Toolchain y versiones

- **Decisión**: Kotlin 2.x, Android Gradle Plugin y Compose BOM en su última versión estable en el
  momento de T001, fijadas en `gradle/libs.versions.toml`. `compileSdk`/`targetSdk` 36, `minSdk` 26.
  JDK 17 (instalado en la máquina). `androidx.tv:tv-material` 1.1.x estable.
- **Motivo**: restricciones técnicas de la constitución. Fijar en catálogo permite que F1 herede las
  mismas versiones si el spike las valida.
- **Alternativas**: versiones alfa/beta de Compose for TV (descartado: inestabilidad no deseada en un
  spike cuyo objetivo es medir la plataforma, no la librería).

## R3. Selección de UI móvil/TV en runtime

- **Decisión**: una sola `Activity` con Compose. Al crear, se detecta TV con
  `UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION` (respaldo:
  `PackageManager.FEATURE_LEANBACK`). Las pantallas comparten `ViewModel` y estado; solo cambian los
  composables (Material 3 en móvil, `tv-material` en TV). Manifest con `leanback` y `touchscreen`
  `required="false"`, categoría `LEANBACK_LAUNCHER` y `android:banner` 320×180.
- **Motivo**: es el mecanismo que usará el producto (constitución: «la capa de UI se selecciona en
  runtime»); el spike lo valida de paso en D2–D4.
- **Alternativas**: dos `Activity` distintas por lanzador (duplica navegación); solo UI Material 3 en TV
  (no valida la experiencia D-pad real).

## R4. Cadena de obtención de acceso al almacenamiento

- **Decisión** (en orden, reevaluando en cada `ON_RESUME`):
  1. API ≥ 30: `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` con `package:` URI.
  2. Si lanza `ActivityNotFoundException` o `SecurityException`:
     `Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION` (lista genérica).
  3. Si también falla: pantalla de instrucciones ADB con
     `adb shell appops set --uid es.sergiosahuquillo.burnfiles.spike MANAGE_EXTERNAL_STORAGE allow`.
  4. Si el usuario no lo concede: modo limitado `MediaStore`.
  - API 26–29: permisos runtime `READ/WRITE_EXTERNAL_STORAGE`; `requestLegacyExternalStorage="true"`
    para API 29.
  - Comprobación: `Environment.isExternalStorageManager()` (API ≥ 30) o `checkSelfPermission`.
  - Se registra cada intento con su resultado: `opened`, `activity_not_found`, `security_exception`,
    `granted`, `denied`, `no_effect` (pantalla abierta pero sin cambio de estado al volver).
- **Motivo**: es literalmente el orden de la constitución (principio IV) y del plan del proyecto.
  Registrar `no_effect` cubre el caso descrito en el plan de TVs que tienen la pantalla pero no la
  concesión.
- **Alternativas**: comprobar con `resolveActivity` antes de lanzar (en API 30+ exige declarar
  `<queries>` y aun así hay ROMs que resuelven la Activity y luego fallan; `try/catch` es obligatorio
  igualmente, así que se usan ambos: `resolveActivity` se registra como dato y `try/catch` decide).

## R5. Estrategias de recorrido para el benchmark

- **Decisión**: cinco configuraciones, todas sobre `Dispatchers.IO` con `limitedParallelism(n)` y
  reparto por subcarpeta de primer nivel:

  | ID | Estrategia | Paralelismo |
  |----|------------|-------------|
  | `nio-p1` | `java.nio.file.Files.walkFileTree` | 1 |
  | `nio-p4` | `java.nio.file.Files.walkFileTree` | 4 |
  | `io-p1` | `java.io.File.listFiles` recursivo | 1 |
  | `io-p4` | `java.io.File.listFiles` recursivo | 4 |
  | `mediastore` | Consulta a `MediaStore.Files.getContentUri("external")` (`_SIZE`, `MIME_TYPE`, `DATE_MODIFIED`) | n/a |

  `Android/data` y `Android/obb` se excluyen con `SKIP_SUBTREE` / filtro previo. Cada configuración se
  ejecuta 3 veces; se informa mediana y peor. Se registra también si la caché de disco estaba «fría»
  (primera ejecución tras generar o reiniciar) o «caliente».
- **Motivo**: el plan del proyecto propone NIO con paralelismo 4; medir también 1 permite saber si el
  paralelismo ayuda en TV con eMMC lenta (puede empeorar). `MediaStore` es el único camino en modo
  limitado y también puede ser más rápido que recorrer disco con acceso total.
- **Alternativas**: `Files.walk` (stream) — mismo coste que `walkFileTree` con peor control de errores;
  JNI/`readdir` nativo — fuera de alcance (añade NDK y tamaño de APK); medir la inserción en el índice
  (Room) — se deja para F2, donde se diseña el índice real (hueco conocido, se anota en el informe).

## R6. Conjunto sintético

- **Decisión**: generador determinista (semilla fija `0xB0FF11E5` por defecto) bajo
  `<almacenamiento compartido>/BurnFilesSpike/dataset/`. Perfil por defecto:
  - 100.000 archivos, ~4.000 carpetas, profundidad 1–6 (ramificación decreciente: muchas carpetas
    poco profundas).
  - Tamaños: 97 % entre 0 y 2 KiB, 3 % entre 2 y 64 KiB → ≈ 200 MiB en total.
  - Extensiones mezcladas (`.jpg`, `.mp4`, `.txt`, `.apk`, `.zip`, `.tmp`, sin extensión) para que el
    índice multimedia los clasifique de forma variada; contenido de relleno no válido como multimedia.
  - Un marcador `dataset.json` con semilla, perfil y estado (`incomplete`/`complete`) escrito al inicio
    y al final, para detectar generaciones interrumpidas.
  - Se comprueba el espacio libre antes de empezar (necesario + 10 %).
- **Motivo**: el coste de recorrido depende del número de entradas, no de los bytes; un perfil de
  ~200 MiB cabe en un Fire TV Stick (8 GB) y reproduce la forma típica de un almacenamiento móvil.
- **Alternativas**: tamaños realistas (varios GB; no caben en TV); archivos de 0 bytes (el sistema
  podría optimizarlos y distorsionar la medida).

## R7. Descarga, verificación e instalación de la actualización

- **Decisión**:
  - Consulta: `GET https://api.github.com/repos/sergiosahuquillo/burn-files/releases?per_page=20` y
    filtrado por `tag_name` con prefijo `spike-v` (las pre-releases no aparecen en `/latest`).
  - Cliente HTTP: `HttpURLConnection` + `org.json` del framework (cero dependencias nuevas).
  - Descarga a `cacheDir/updates/` con progreso; verificación SHA-256 con `MessageDigest` contra el
    asset `.sha256` publicado.
  - Firma: `PackageManager.getPackageArchiveInfo` con `GET_SIGNING_CERTIFICATES` (API ≥ 28) o
    `GET_SIGNATURES` (API < 28); se compara el SHA-256 de cada certificado con el del paquete
    instalado.
  - Permiso: `canRequestPackageInstalls()`; si falta, `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` con
    `package:` URI, protegido con `try/catch`.
  - Instalación: sesión de `PackageInstaller` (`MODE_FULL_INSTALL`), `commit` con `PendingIntent` a un
    `BroadcastReceiver`; ante `STATUS_PENDING_USER_ACTION` se lanza el `EXTRA_INTENT` del sistema.
    Nunca se usa `USER_ACTION_NOT_REQUIRED`.
  - Versiones N y N+1: `versionCode` 1 y 2 (propiedad Gradle `spikeVersionCode`), ambas firmadas con la
    misma clave de pruebas (el `debug.keystore` del desarrollador).
- **Motivo**: replica el flujo del updater de producto descrito en el plan (pasos 4–5) con el mínimo
  de dependencias; si funciona así en Fire OS 7 y Android TV, F1 puede elegir libremente el cliente
  HTTP.
- **Alternativas**: `ACTION_VIEW`/`ACTION_INSTALL_PACKAGE` con `FileProvider` (obsoleto desde API 29 y
  no es lo que usará el producto); OkHttp/Ktor (dependencia innecesaria para el spike).

## R8. Prueba de escritura en USB

- **Decisión**:
  - Detección: `StorageManager.getStorageVolumes()` filtrando `isRemovable`; ruta con
    `StorageVolume.getDirectory()` (API ≥ 30) o derivada de `Context.getExternalFilesDirs(null)`
    recortando hasta la raíz del volumen (API < 30). Capacidad/libre con `StatFs`.
  - Sonda A (acceso directo): crear `BurnFilesSpike-probe-<uuid>.bin` (64 KiB aleatorios) en la raíz del
    volumen, leer y comparar SHA-256, eliminar.
  - Sonda B (selector del sistema, solo si existe): `ACTION_OPEN_DOCUMENT_TREE` sobre el volumen y la
    misma secuencia con `DocumentFile`; si la Activity no existe se registra `activity_not_found`.
  - No se usa el directorio específico de la app en el volumen (`Android/data/...`), por el principio III.
- **Motivo**: en Android 9 (Fire OS 7) `WRITE_EXTERNAL_STORAGE` no concede escritura en volúmenes
  secundarios fuera del directorio de la app; la sonda B mide si el selector del sistema es una salida
  real en TV. Es exactamente la pregunta del entregable «Escritura en pendrive USB desde TV».
- **Alternativas**: solo sonda A (no informaría del plan B); `MediaStore` sobre volúmenes extraíbles
  (solo cubre multimedia y no permite verificar escritura arbitraria).

## R9. Registro de resultados y exportación

- **Decisión**: JSON Lines en `filesDir/results/results.jsonl` (una entrada por prueba, esquema en
  [contracts/results-log.schema.json](./contracts/results-log.schema.json)). Exportación:
  - Compartir con `ACTION_SEND` vía `FileProvider` (si hay destino).
  - Copiar a `Download/BurnFilesSpike/` (vía `MediaStore.Downloads` en API ≥ 29, archivo directo en
    API < 29).
  - Recuperación por ADB siempre disponible al ser build depurable:
    `adb shell run-as es.sergiosahuquillo.burnfiles.spike cat files/results/results.jsonl`.
  - El escritor de registros solo acepta tipos cerrados (enums, números, identificadores de matriz):
    no hay campos de texto libre que puedan arrastrar rutas o nombres de archivo.
- **Motivo**: `filesDir` funciona sin ningún permiso (también en modo limitado); `run-as` evita depender
  de que el TV tenga app para compartir. Tipos cerrados hacen el principio VI verificable por test.
- **Alternativas**: Room (excesivo para un registro de decenas de líneas); escribir en
  `BurnFilesSpike/` del almacenamiento compartido (no disponible en modo limitado).

## R10. Internacionalización, temas y calidad

- **Decisión**:
  - `res/values/strings.xml` (en) y `res/values-es/strings.xml` (es), `<plurals>` donde haya conteos;
    `locales_config.xml`. Tamaños con `Formatter.formatShortFileSize`, duraciones y fechas con
    `DateTimeFormatter` del locale activo.
  - Tema: `MaterialTheme` (móvil) y `androidx.tv.material3.MaterialTheme` (TV) alimentados por los mismos
    tokens de color claro/oscuro; nada de colores literales en composables.
  - Calidad: ktlint (plugin `org.jlleitschuh.gradle.ktlint`) y detekt; script
    `tools/check_strings.py` que falla si una clave existe en un idioma y no en el otro (reutilizable
    por F1). CI en GitHub Actions para `spike/` (build, unit tests, ktlint, detekt, strings).
  - Tests: JUnit 5 en JVM para la lógica pura (cadena de fallback, verificación SHA-256 y certificado,
    determinismo del generador, saneado del registro, estadísticas del benchmark); test de UI de
    Compose instrumentado para el diálogo destructivo de limpieza en móvil y TV.
- **Motivo**: principios V y VII y la sección *Flujo de desarrollo y calidad* de la constitución; el
  spike aplica los mismos estándares para no acumular deuda si parte de su código se promueve.
- **Alternativas**: omitir i18n/temas en el spike (violaría los principios V sin necesidad real).

## R11. Entorno de pruebas

- **Decisión**: imágenes del SDK a instalar (no presentes en la máquina a 2026-11-01):
  `system-images;android-30;android-tv;x86` y `system-images;android-34;android-tv;x86`
  (opcional `system-images;android-31;android-tv;x86`). Fire TV por ADB en red
  (`adb connect <ip>:5555`). Móvil físico Android 13+ por USB o Wi-Fi.
- **Motivo**: matriz mínima del principio I.
- **Alternativas**: imágenes Google TV (`google-tv`) — válidas como complemento, pero las `android-tv`
  reproducen mejor el caso de TV 11/12 sin pantalla de acceso total.

## Huecos conocidos (se trasladan al informe)

- Coste de inserción en el índice (Room): se mide en F2.
- TV real con Android 11+ sin pantalla de acceso total: solo emulador (riesgo ya aceptado en el plan).
- Verificación de desarrollador de Google en Fire OS: no se puede validar en el spike.
