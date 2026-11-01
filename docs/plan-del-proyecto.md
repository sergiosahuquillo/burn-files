# Burn Files — Plan del proyecto

Oct 31, 2026 · @Sergio

## Visión y alcance

Burn Files es un gestor de archivos para Android y Android TV cuya pantalla principal no es un explorador, sino un análisis de almacenamiento que dice qué se puede borrar y cuánto se recupera. El explorador clásico queda como segunda pestaña.

**Objetivos del producto**

- Al abrir la app, en menos de 10 s en un dispositivo medio, mostrar el espacio usado por categoría y una lista de candidatos a borrar ordenada por tamaño.
- No proponer nunca nada del sistema ni datos privados de otras apps: solo archivos del almacenamiento compartido que el usuario controla.
- Borrado seguro: papelera interna con retención configurable antes del borrado definitivo.
- Paridad funcional en móvil y TV con una sola APK, navegable por táctil y por mando (D-pad).
- Requisitos fijos: tema claro/oscuro, español e inglés, actualizaciones automáticas desde GitHub Releases.

**Fuera de alcance (v1)**: root, limpieza de caché de otras apps (Android no lo permite sin privilegios de sistema), nube/FTP/SMB, iOS.

## Decisión de stack

Recomendación: **Kotlin nativo + Jetpack Compose + Compose for TV**, en lugar del stack Expo habitual. La app es solo Android y casi todo su valor está en APIs nativas (sistema de archivos, MediaStore, StorageStatsManager, PackageInstaller), así que React Native aporta poco y añade una capa puente.

| Criterio | Kotlin + Compose | Expo + react-native-tvos |
| --- | --- | --- |
| Escaneo de cientos de miles de archivos | Coroutines en el mismo proceso, sin puente | Requiere un módulo Expo en Kotlin igualmente |
| Android TV | Compose for TV estable (tv-material 1.1.0, tv-foundation 1.0.0, mayo 2026) | Fork react-native-tvos atado a la versión de RN; compatibilidad de Uniwind/Reusables con foco D-pad sin validar |
| Auto-update desde GitHub | Nativo (PackageInstaller) | Nativo igualmente: expo-updates solo sirve OTA de JS, no actualiza la APK |
| Tamaño de APK | \~8–12 MB | \~25–40 MB con Hermes |
| Velocidad de desarrollo de UI | Media | Alta (tu stack conocido) |
| iOS en el futuro | No | Posible, pero un file manager en iOS es otro producto |

**Stack propuesto**

- Lenguaje y UI: Kotlin 2.x, Jetpack Compose, Material 3 (móvil) y `androidx.tv:tv-material` (TV).
- Arquitectura: MVVM + UDF, Hilt para DI, Navigation Compose.
- Datos: Room (índice del escaneo, papelera, historial), DataStore (ajustes).
- Trabajo en segundo plano: WorkManager (escaneo programado, limpieza de papelera, comprobación de updates).
- Red: Ktor client o OkHttp para la API de GitHub.
- Calidad: ktlint + detekt, tests con JUnit5/Turbine, Compose UI tests; CI en GitHub Actions.
- Package id: `es.sergiosahuquillo.burnfiles`. `minSdk` 26 (Android 8), `targetSdk` 36.

**Decidido:** Kotlin + Compose, `minSdk` 26, repositorio público con licencia GPL-3.0. Informes de errores con Sentry, desactivado por defecto (opt-in) y sin rutas ni nombres de archivo en los eventos.

## Funcionalidades priorizadas

El MVP entrega el análisis y el borrado seguro; el explorador completo llega en v1.0. Fases: MVP (uso propio), v1.0 (primera release pública), v1.x (después).

| Funcionalidad | Fase | Notas |
| --- | --- | --- |
| Dashboard de almacenamiento: usado/libre por volumen y por categoría | MVP | Imágenes, vídeo, audio, documentos, APKs, archivos comprimidos, descargas, otros |
| Top archivos grandes con selección múltiple y borrado | MVP | Umbral configurable (por defecto ≥ 50 MB) |
| Top carpetas por tamaño (vista árbol/treemap) | MVP | Drill-down hasta el archivo |
| Papelera interna con restauración | MVP | Retención por defecto 7 días |
| APKs ya instalados u obsoletos | MVP | Compara versionCode con el paquete instalado |
| Explorador: navegar, abrir con, compartir | MVP |  |
| Copiar, mover, renombrar, crear carpeta, borrar | v1.0 | Cola de operaciones con progreso y cancelación |
| Duplicados (tamaño + hash parcial + hash completo) | v1.0 | El de mayor impacto tras los archivos grandes |
| Carpetas vacías y archivos temporales (`.tmp`, `.log`, miniaturas `.thumbnails`) | v1.0 |  |
| Archivos antiguos sin abrir (> N meses) | v1.0 | Basado en fecha de modificación |
| Búsqueda por nombre, tipo, tamaño y fecha | v1.0 | Sobre el índice Room, no recorriendo disco |
| Ordenar, filtrar, vista lista/cuadrícula, archivos ocultos | v1.0 |  |
| Favoritos y accesos rápidos | v1.0 |  |
| USB OTG y tarjeta SD | v1.0 | Clave en TV: muchos usuarios conectan pendrives |
| Apps instaladas por tamaño, con acceso directo a sus ajustes para limpiar caché | v1.0 | Requiere permiso de uso (`PACKAGE_USAGE_STATS`) |
| Comprimir/descomprimir ZIP | v1.x |  |
| Miniaturas y visor integrado de imagen/vídeo/texto | v1.x |  |
| Imágenes similares y capturas de pantalla antiguas | v1.x | Hash perceptual |
| Escaneo programado y notificación de espacio bajo | v1.x | WorkManager |
| Acceso a `Android/data` vía Shizuku | v1.x | Opcional, usuarios avanzados |

## Motor de análisis de espacio

El análisis es incremental: el primer arranque recorre todo el almacenamiento y guarda un índice en Room; los siguientes solo revisan carpetas cuyo `lastModified` cambió. Así el dashboard aparece al instante con datos previos y se refresca en segundo plano.

**Pipeline**

1. **Totales instantáneos**: `StorageStatsManager` / `StatFs` por volumen (usado, libre, total). Se pinta en < 1 s.
2. **Desglose rápido**: consultas a `MediaStore` agrupadas por tipo y tamaño. Da categorías aproximadas en 1–3 s.
3. **Recorrido completo**: `Files.walkFileTree` (NIO) en paralelo por subcarpeta de primer nivel con coroutines (`Dispatchers.IO`, límite de paralelismo 4). Emite progreso por `Flow`.
4. **Indexado**: inserción por lotes en Room (ruta, tamaño, mtime, tipo MIME, carpeta padre, tamaño agregado por carpeta).
5. **Detectores**: cada regla de candidatos es una clase independiente que consulta el índice (archivos grandes, APKs, duplicados, vacías, temporales, antiguos).
6. **Resultado**: lista de candidatos con tamaño recuperable total y por regla.

**Qué es eliminable** (lista blanca, no negra)

- Solo se analiza el almacenamiento compartido accesible con el permiso concedido: `/storage/emulated/0` y volúmenes extraíbles.
- Excluidos siempre: `Android/data`, `Android/obb` (el sistema los bloquea desde Android 11), la carpeta de la propia app y archivos `.nomedia` como marcador, no como objetivo.
- Carpetas sensibles marcadas como «revisar con cuidado», nunca preseleccionadas: `DCIM/Camera`, `Pictures`, `Documents`, `WhatsApp/Media` y carpetas de copias de seguridad conocidas.
- Preseleccionables por defecto solo: APKs ya instalados, temporales, miniaturas regenerables y duplicados (manteniendo siempre una copia).
- Nada se borra sin confirmación explícita; todo pasa por la papelera salvo que el usuario la desactive.

**Rendimiento objetivo**: 200.000 archivos en < 30 s en un móvil de gama media; en TV (CPU y disco más lentos) se acepta hasta 60 s con progreso visible. Hay que medirlo en el spike de la fase 0.

## Permisos y almacenamiento

Un file manager real en Android 11+ necesita `MANAGE_EXTERNAL_STORAGE` («Acceso a todos los archivos»). Es un permiso especial: no se concede con un diálogo, sino llevando al usuario a Ajustes › Acceso especial de apps. Aun con él, `Android/data` sigue bloqueado ([CommonsWare](https://commonsware.com/R/pages/chap-scoped-007)).

| Permiso | Versión | Para qué | Cómo se obtiene |
| --- | --- | --- | --- |
| `MANAGE_EXTERNAL_STORAGE` | Android 11+ (API 30) | Leer/escribir todo el almacenamiento compartido | Intent `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` |
| `READ/WRITE_EXTERNAL_STORAGE` | Android 8–10 (API 26–29) | Igual, modelo antiguo | Diálogo runtime + `requestLegacyExternalStorage` en API 29 |
| `PACKAGE_USAGE_STATS` | Todas | Tamaño de apps instaladas | Ajustes › Acceso a uso |
| `REQUEST_INSTALL_PACKAGES` | Todas | Instalar la actualización descargada | Ajustes › Instalar apps desconocidas |
| `POST_NOTIFICATIONS` | Android 13+ | Progreso de operaciones, aviso de update | Diálogo runtime |

**Onboarding de permisos**: pantalla propia que explica cada permiso y abre el ajuste correcto, con comprobación al volver (`Environment.isExternalStorageManager()`). Siempre con `try/catch` del intent: la documentación avisa de que la Activity puede no existir.

**Modo degradado**: si no hay acceso total, la app funciona solo con `MediaStore` (multimedia y descargas) y lo indica claramente en el dashboard.

**Distribución**: Google Play solo permite `MANAGE_EXTERNAL_STORAGE` a apps cuyo núcleo es gestionar archivos (esta lo es, pero requiere declaración y revisión). Como la distribución principal es GitHub, no es bloqueante; si algún día se publica en Play, habrá un *flavor* `play` sin updater propio.

## Android TV

El mayor riesgo técnico del proyecto está aquí: en Android TV la pantalla de «Acceso a todos los archivos» puede no existir. Hay reportes de `ActivityNotFoundException` al lanzar el intent, y pruebas en emulador indican que solo aparece a partir de Android TV 13, no en 11 ni 12 ([Stack Overflow](https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/75298603/manage-external-storage-not-working-on-android-tv)). Además, el Storage Access Framework no está disponible en muchos TV.

**Mitigaciones, por orden**

1. Intentar el intent de acceso total; si falla, probar el genérico `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`.
2. Si ninguno existe, pantalla con instrucciones para concederlo por ADB: `adb shell appops set es.sergiosahuquillo.burnfiles MANAGE_EXTERNAL_STORAGE allow`.
3. Si no, modo degradado con `MediaStore` (cubre vídeos, audio, imágenes y descargas, que es lo que más ocupa en un TV).
4. Pendrives USB: verificar en el spike si `MANAGE_EXTERNAL_STORAGE` da acceso de escritura al volumen USB en cada versión.

**Integración con el sistema**

- Manifest: `<uses-feature android:name="android.software.leanback" android:required="false"/>` y `android.hardware.touchscreen` con `required="false"`, para que una misma APK sirva en móvil y TV.
- Activity con categoría `LEANBACK_LAUNCHER` y `android:banner` de 320×180 px.

**UX en TV**

- Detección en runtime (`UiModeManager` = `UI_MODE_TYPE_TELEVISION`) para cargar el tema y layouts de TV.
- Todo accesible con D-pad: foco visible y estado inicial de foco definido en cada pantalla; nada depende de gestos (swipe, long-press).
- Selección múltiple con botón central + menú contextual con la tecla Menú o un botón en pantalla.
- Navegación lateral (NavigationDrawer de tv-material) en vez de bottom bar; textos y objetivos más grandes para distancia de 3 m; márgenes de *overscan* del 5 %.
- Probar en tu Fire TV, en emuladores Android TV 11 y 14 y, si es posible, en un Google TV real.

**Fire TV (tu dispositivo de pruebas)**

- Solo los Fire TV con **Fire OS** ejecutan APKs. Los modelos nuevos con **Vega OS** (Fire TV Stick 4K Select, Fire TV Stick HD de 2026) no son Android y no pueden instalar la app ([AFTVnews](https://www.aftvnews.com/these-are-the-fire-tvs-that-dont-support-sideloading-or-downloader-due-to-vega-os/)). Se comprueba en Ajustes › Mi Fire TV › Acerca de: «Fire OS 7/8/14» vale, «OS 1.x» es Vega.
- Fire OS 7 = Android 9: modelo de permisos antiguo, no hay acceso total que pedir. Fire OS 8 = Android 11: justo la versión donde la pantalla de acceso total puede faltar; el fallback por ADB funciona porque Fire TV permite depuración ADB por red desde Opciones de desarrollador.
- Sin Google Play Services: nada de GMS ni Firebase. Sentry, Room y la API de GitHub no los necesitan.
- Para que el updater instale, el usuario debe permitir «Apps de origen desconocido» para Burn Files en los ajustes de Fire OS.
- Fire OS no es un dispositivo certificado por Google, así que previsiblemente la verificación de desarrollador no aplica ahí (a confirmar).

## Requisitos transversales

Los tres requisitos se montan en la fase 1, antes de cualquier funcionalidad, porque tocan todas las pantallas.

**Tema claro/oscuro**

- Opciones: Sistema (por defecto), Claro, Oscuro; guardado en DataStore.
- Material 3 con *dynamic color* en Android 12+ como opción; paleta propia como fallback.
- En TV, tema oscuro por defecto (convención de la plataforma) y `tv-material` con su propio `MaterialTheme` alimentado por los mismos tokens.

**Idiomas español e inglés**

- `strings.xml` en `values/` (inglés, base) y `values-es/`; plurales con `<plurals>`.
- Selector de idioma en la app con *per-app language* (`AppCompatDelegate.setApplicationLocales`) y `locales_config.xml`, para que también aparezca en Ajustes del sistema en Android 13+.
- Tamaños y fechas formateados con `Formatter.formatShortFileSize` y `DateTimeFormatter` según locale.
- Check de CI: falla si una clave existe en un idioma y no en el otro.

**Actualizaciones automáticas desde GitHub Releases**

1. Al arrancar (máx. una vez cada 24 h, vía WorkManager) se consulta `GET /repos/{owner}/burn-files/releases/latest`.
2. Se compara el `tag_name` (semver, p. ej. `v1.2.0`) con la versión instalada; las *pre-releases* se ignoran siempre (no hay canal beta).
3. Si hay versión nueva: aviso con el changelog (cuerpo de la release, renderizado como Markdown) y botón Actualizar.
4. Descarga del asset `.apk` con progreso; verificación del SHA-256 publicado en la release y de que el certificado de firma coincide con el instalado.
5. Instalación con `PackageInstaller` (sesión). En Android 12+ con `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` podrían ser silenciosas, pero por defecto la app siempre avisa y pide confirmación.
6. Ajustes: comprobar automáticamente (sí/no) y comprobar ahora.

**Release pipeline**: tag `v*` → GitHub Actions compila la APK *release*, la firma con el keystore guardado en *secrets*, calcula el SHA-256 y crea la release con APK + checksum + changelog generado de los commits.

**Verificación de desarrollador de Google**: desde el 30 de septiembre de 2026 los dispositivos certificados de Brasil, Indonesia, Singapur y Tailandia solo instalan apps de desarrolladores verificados, y Google prevé extenderlo a todo el mundo en 2027 ([Android Authority](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/)). Hay que verificar tu identidad y registrar `es.sergiosahuquillo.burnfiles` en la Android Developer Console antes de la primera release pública, o los usuarios tendrán que usar el flujo avanzado de sideloading o ADB.

## Arquitectura

Una sola APK con dos capas de UI (móvil y TV) que comparten ViewModels, dominio y datos; la UI se elige en runtime según el tipo de dispositivo.

&#91;embedded content: arquitectura · 4 capas, UI móvil y TV\]

Todo lo que hay por debajo de la UI es común, así que una funcionalidad nueva solo exige diseñar dos pantallas, no dos implementaciones.

**Módulos Gradle** (organizados por dominio)

- `app`: punto de entrada, DI, navegación y selección móvil/TV.
- `core:ui`: tokens de tema, componentes compartidos, strings.
- `core:data`: Room, DataStore.
- `core:fs`: acceso a archivos, volúmenes, permisos, cola de operaciones.
- `feature:analysis`, `feature:explorer`, `feature:trash`, `feature:settings`, `feature:updater`: cada uno con sus subcarpetas `ui/mobile`, `ui/tv`, `domain`, `data`.

WorkManager orquesta las tareas en segundo plano (escaneo programado, purga de papelera, comprobación de updates).

## Roadmap

Cinco fases; la primera es un spike de una semana que valida los tres puntos que podrían tumbar el proyecto antes de escribir UI.

&#91;embedded content: roadmap · 5 fases, 4 puertas\]

El updater va en F1 a propósito: desde la v0.1 cada build de prueba llega sola a tus dispositivos, incluido el TV.

**Esfuerzo orientativo** (semanas de trabajo efectivo): F0 ≈ 1, F1 ≈ 1–2, F2 ≈ 3–4, F3 ≈ 4–6, F4 continuo.

**Entregables del spike F0**

- [ ] App mínima que pide acceso total en móvil, emuladores Android TV 11 y 14, y tu Fire TV; resultado documentado por versión.
- [ ] Benchmark de recorrido NIO vs `File.listFiles` vs MediaStore con 100k+ archivos.
- [ ] Instalación de una APK descargada con `PackageInstaller` en móvil y TV.
- [ ] Escritura en pendrive USB desde TV.

## Riesgos y decisiones

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Sin pantalla de acceso total en Android TV 11/12 y Fire OS 8 | Alto: la app queda limitada a multimedia en esos TV | Spike en fase 0; fallback ADB + modo MediaStore |
| Sin TV real con Android 11+ (tu Fire TV es Fire OS 7) | Medio: el caso de permisos más difícil en TV solo se prueba en emulador | Emuladores Android TV 11, 12 y 14; valorar un Google TV barato antes de v1.0 |
| Borrado accidental de algo importante | Alto: pérdida de confianza | Papelera de 7 días por defecto, nada sensible preseleccionado, confirmación con tamaño total |
| Escaneo lento en TV de gama baja | Medio | Índice incremental, paralelismo limitado, resultados parciales en streaming |
| Verificación de desarrollador de Google | Medio: instalación bloqueada en países afectados | Verificarse y registrar el paquete antes de v1.0 |
| Pérdida del keystore de firma | Alto: las updates dejan de instalarse sobre la versión previa | Copia cifrada fuera de GitHub + secret en Actions |
| Rate limit de la API de GitHub (60 peticiones/h sin token) | Bajo | Una comprobación cada 24 h, caché con `ETag` |

**Decisiones tomadas**

| Decisión | Elección |
| --- | --- |
| Stack | Kotlin + Jetpack Compose + Compose for TV |
| Repositorio | Público, `<tu-usuario>/burn-files` |
| Licencia | GPL-3.0 |
| `minSdk` | 26 (Android 8) |
| Google Play | Sin decidir: el updater va en su propio módulo para poder sacar un flavor sin él, pero no se construye aún |
| Papelera | 7 días por defecto, configurable |
| Informes de errores | Sentry opt-in, sin rutas ni nombres de archivo |
| Actualizaciones | Avisar con changelog y pedir confirmación |
| Canal beta | No; solo releases estables |
| Dispositivos de prueba | Móvil Android 13+, Amazon Fire TV (Fire OS 7, Android 9), emuladores Android TV 11 y 14 |

**Cobertura de pruebas**: el Fire TV con Fire OS 7 cubre el modelo de permisos antiguo (`READ/WRITE_EXTERNAL_STORAGE`, sin scoped storage), la UX con mando, los pendrives USB y la instalación de updates en Fire OS. El móvil Android 13+ cubre el acceso total moderno. El hueco es un TV con Android 11 o superior sin pantalla de acceso total: solo se probará en emulador.

## Fuentes

- [Compose for TV — notas de versión](https://developer.android.com/jetpack/androidx/releases/tv?hl=es)
- [Build Expo apps for TV](https://docs.expo.dev/guides/building-for-tv)
- [CommonsWare — All Files Access](https://commonsware.com/R/pages/chap-scoped-007)
- [MANAGE\_EXTERNAL\_STORAGE en Android TV (Stack Overflow)](https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/questions/75298603/manage-external-storage-not-working-on-android-tv)
- [Android Authority — calendario de verificación de desarrolladores](https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/)

* [AFTVnews — Fire TV con Vega OS sin sideloading](https://www.aftvnews.com/these-are-the-fire-tvs-that-dont-support-sideloading-or-downloader-due-to-vega-os/)
