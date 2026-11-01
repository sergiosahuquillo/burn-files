# Spike F0 — Informe de resultados

Informe consolidado del spike de viabilidad ([spec](../../specs/001-f0-feasibility-spike/spec.md),
[plan](../../specs/001-f0-feasibility-spike/plan.md)). Recoge los resultados de la herramienta de diagnóstico
`es.sergiosahuquillo.burnfiles.spike` en la matriz de dispositivos y decide, por cada riesgo, si la fase 1
puede empezar.

- **Fecha**: 2026-11-01
- **Herramienta**: 0.0.1 (versionCode 1), con las correcciones de este informe; 0.0.2 publicada como
  pre-release `spike-v0.0.2` para la prueba de actualización.
- **Registros**: `results-<dispositivo>.jsonl` recuperados por ADB, válidos contra
  [results-log.schema.json](../../specs/001-f0-feasibility-spike/contracts/results-log.schema.json)
  (89 entradas, 0 inválidas; copia en [results/](results/)) y sin rutas ni nombres de archivo.

## Decisión por riesgo

| Riesgo | Decisión | Justificación |
|--------|----------|---------------|
| R1 · Acceso total al almacenamiento en TV | **GO** | La cadena de permisos llega a acceso total en las tres versiones de Android TV probadas (9, 11 y 14). Donde la pantalla del sistema no existe (TV 11), el fallback por ADB funciona y se detecta al volver. Nunca se cierra la app. |
| R2 · Rendimiento del escaneo | **GO** | Con 4 hilos se recorren 100.001 archivos en 3,5–4,8 s; extrapolado a 200.000, 7–10 s frente a los 30 s (móvil) y 60 s (TV) del objetivo. Margen amplio aunque el hardware real sea 3–5 veces más lento. |
| R3 · Autoactualización con `PackageInstaller` | **GO** | La 0.0.1 se actualiza sola a la 0.0.2 desde GitHub Releases en los cuatro dispositivos, con SHA-256 y firma verificados. Dos fallos encontrados y resueltos o documentados: en Android 9 la firma del APK descargado no se leía (corregido) y en Android 11 conceder el permiso de instalación mata la app (F1 debe retomar la instalación). |
| R4 · Escritura en pendrive USB desde TV | **GO_WITH_CHANGES** | Con acceso total (API ≥ 30) la escritura directa funciona. En Android TV no hay selector de carpetas del sistema (SAF) en ninguna versión, y en API < 30 la escritura directa da `EACCES`: en TV con Android 9 (Fire OS 7) el USB será **solo lectura** salvo que el Fire TV real demuestre lo contrario. |

**Conclusión**: la fase 1 puede empezar. Ningún riesgo bloquea; R4 obliga a definir el USB de solo lectura en
TV con API < 30, y R3 deja dos requisitos concretos para el updater de F1.

## Matriz ejecutada

No había dispositivos físicos disponibles durante la ejecución, así que la matriz se cubrió con emuladores
(x86, sobre un PC de 16 hilos). La columna *Sustituye a* indica qué dispositivo de la spec representa cada uno.

| Id en informe | Dispositivo | Android (API) | Interfaz | Sustituye a |
|---------------|-------------|---------------|----------|-------------|
| D1 | Emulador Pixel (`sdk_gphone64_x86_64`) | 16 (36) | Táctil | D1 · móvil Android 13+ |
| TV9 | Emulador Android TV (`sdk_google_atv_x86`) | 9 (28) | D-pad | D2 · Fire TV con Fire OS 7 (mismo nivel de API) |
| D3 | Emulador Android TV (`AOSP TV on x86`) | 11 (30) | D-pad | D3 |
| D4 | Emulador Android TV (`AOSP TV on x86`) | 14 (34) | D-pad | D4 |

El volumen extraíble de cada emulador de TV es una tarjeta SD virtual de 512 MB montada como volumen público,
que el sistema trata igual que un pendrive.

## Resultados por dispositivo y prueba

| Prueba | D1 · móvil 16 | TV9 · TV 9 | D3 · TV 11 | D4 · TV 14 |
|--------|---------------|------------|------------|------------|
| V1 · Cadena de acceso | ✅ `APP_SETTINGS` → FULL | ✅ `RUNTIME_DIALOG` → LEGACY_FULL | ✅ ajustes ✗, lista ✗ → `ADB` → FULL | ✅ `APP_SETTINGS` → FULL |
| V2 · Concesión por ADB detectada al volver | — | — | ✅ | — |
| V3 · Benchmark 100k | ✅ | — (decisión, ver desviaciones) | — (decisión) | ✅ |
| V4 · Cancelación < 2 s | — | — | — | ✅ 76 ms |
| V5 · Modo limitado (solo MediaStore) | — | — | ✅ | — |
| V6 · Actualización 0.0.1 → 0.0.2 | ✅ | ✅ tras corregir la firma | ✅ (2.º intento) | ✅ |
| V8 · USB acceso directo | — (sin volumen) | ❌ escritura `EACCES` | ✅ 4/4 operaciones | ✅ 4/4 operaciones |
| V8 · USB selector del sistema | — | ❌ no hay actividad | ❌ no hay actividad | ❌ stub del sistema |
| V9 · Exportar y recuperar por ADB | ✅ | ✅ | ✅ | ✅ exportación = copia ADB |
| V10 · Recorrido solo con mando | — | ✅ | ✅ | ✅ |
| V11 · Claro/oscuro y en/es | ✅ oscuro + es | ✅ claro | ✅ claro | ✅ oscuro + es |
| V12 · Limpieza del conjunto | ✅ (sesión anterior) | — | — | ✅ sin restos |
| SC-002 · Sin cierres inesperados | ✅ | ✅ | ✅ | ✅ |

Tests de UI de Compose de los diálogos destructivos (móvil y TV): 3/3 en verde en D1 y en TV9.

## Benchmark

Conjunto sintético `default-100k` (semilla `0xB0FF11E5`): 100.000 archivos en 4.000 carpetas, 202 MB.
Cada estrategia se ejecuta 3 veces; la primera de la primera estrategia es en frío.

| Estrategia | D1 mediana | D1 peor | D4 mediana | D4 peor | 200k estimado (D1 / D4) |
|------------|-----------:|--------:|-----------:|--------:|------------------------:|
| `NIO_P1` · `Files.walkFileTree`, 1 hilo | 12,2 s | 12,3 s | 11,3 s | 13,2 s | 24,5 s / 22,5 s |
| `NIO_P4` · `Files.walkFileTree`, 4 hilos | 4,6 s | 5,3 s | 4,0 s | 4,3 s | 9,1 s / 8,0 s |
| `IO_P1` · `File.listFiles`, 1 hilo | 10,8 s | 11,8 s | 9,3 s | 10,2 s | 21,6 s / 18,5 s |
| `IO_P4` · `File.listFiles`, 4 hilos | 4,8 s | 4,8 s | **3,5 s** | 3,6 s | 9,6 s / **7,1 s** |
| `MEDIASTORE` · consulta a `MediaStore.Files` | 1,4 s | 1,9 s | 2,4 s | 3,2 s | 2,8 s / 4,6 s |

- **Paralelismo**: 4 hilos multiplican por 2,5–2,9 la velocidad; es la palanca principal.
- **`File.listFiles` frente a NIO**: equivalentes en paralelo y `File.listFiles` algo más rápido en serie.
  No hay motivo para preferir NIO.
- **MediaStore** es el más rápido, pero devuelve 104.001 filas porque incluye las carpetas, y solo ve lo que
  el escáner de medios ha indexado. Sirve para el modo limitado, no como sustituto del recorrido completo.
- **Coste fijo de E/S**: generar el conjunto llevó 11–13 min (≈ 130–150 archivos/s) y borrarlo 10,5 min en
  D4. Escribir y borrar a través de FUSE es dos órdenes de magnitud más lento que leer: lo tendrá en cuenta
  la papelera de F3 (mover dentro del mismo volumen, no copiar).

**Limitación**: un emulador mide la CPU y el disco del PC anfitrión, no los de un televisor. Las cifras
confirman que el algoritmo cabe holgadamente en el objetivo; la cifra en hardware de TV real queda pendiente
del Fire TV (ver huecos).

## Acceso al almacenamiento

- **Android TV 14 (D4)**: `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` existe, pero abre la **lista** de
  apps con acceso total, no la ficha de la app. El usuario tiene que localizar la app en la lista: el texto de
  onboarding de F1 debe decirlo.
- **Android TV 11 (D3)**: ni el intent específico ni el genérico tienen actividad (`ACTIVITY_NOT_FOUND`
  en ambos). El paso ADB muestra el comando exacto; tras ejecutarlo, «Comprobar de nuevo» detecta el acceso
  total. Confirma el riesgo del plan y valida su mitigación.
- **Android TV 9 (TV9)**: modelo antiguo; el diálogo del sistema se maneja con el mando y concede
  `LEGACY_FULL`.
- **Retirar el permiso** con `appops` mata el proceso de la app (comportamiento del sistema). F1 debe
  reevaluar el permiso al volver a primer plano, como ya exige la constitución, y no asumir que el proceso
  sobrevive.

## Actualización

Pre-release `spike-v0.0.2` en GitHub (APK de 12,6 MB y su `.sha256`). Con la 0.0.1 instalada: «Comprobar
release de prueba» → «Descargar» → «Instalar» → permitir «instalar apps desconocidas» → confirmar en el
instalador del sistema. El éxito lo registra la propia 0.0.2 al arrancar (`appVersionCode: 2`).

| Paso | D1 · móvil 16 | TV9 · TV 9 | D3 · TV 11 | D4 · TV 14 |
|------|---------------|------------|------------|------------|
| Consulta a la API de GitHub | OK | OK | OK | OK |
| Descarga (redirecciones solo a hosts permitidos) | OK · 2,7 s | OK | OK | OK · 1,5 s |
| SHA-256 | OK | OK | OK | OK |
| Certificado de firma | OK | `MISMATCH` → corregido → OK | OK | OK |
| Permiso de instalación | OK | OK | la app muere al concederlo | OK |
| Instalación con `PackageInstaller` | OK | OK | OK | `USER_CANCELLED` → OK |

- **Android 9 · firma no leída**: `getPackageArchiveInfo(…, GET_SIGNING_CERTIFICATES)` devuelve
  `signingInfo = null` para un APK descargado, así que el conjunto de certificados quedaba vacío y la
  comprobación fallaba (de forma segura: no se instalaba nada). Corregido en `ApkInspector`: si un archivo no
  devuelve firmantes, se consulta con `GET_SIGNATURES`. Afecta directamente a Fire OS 7.
- **Android 11 · el proceso muere** al conceder «instalar apps desconocidas» (`Killing … REQUEST_INSTALL_PACKAGES
  changed`). La herramienta borra las descargas al arrancar, así que hubo que repetir la descarga. El updater
  de F1 debe conservar el APK ya verificado y ofrecer «Instalar» directamente al volver.
- **El diálogo del instalador** abre con el foco en «Cancelar» en todas las TV; con el mando hay que moverse
  a «Actualizar». En D4 una pulsación directa canceló y la herramienta lo registró como `USER_CANCELLED`
  con `installerStatus` 3, que valida también ese camino.
- **Comparación de certificados**: la app instalada se compara con su historial de firmas
  (`signingCertificateHistory`). Con una clave rotada en el futuro, F1 deberá aceptar el certificado actual
  del historial, no exigir conjuntos idénticos.

## USB

| | API ≥ 30 con acceso total | API < 30 (modelo antiguo) |
|--|--|--|
| Escritura directa en `/storage/<uuid>` | ✅ crear, releer, verificar y borrar | ❌ `EACCES`: el permiso de escritura no alcanza a volúmenes extraíbles |
| Selector de carpetas del sistema (SAF) | ❌ en TV no existe (TV 11) o es un *stub* que cierra al instante (TV 14) | ❌ la DocumentsUI mínima de TV no implementa `OPEN_DOCUMENT_TREE` |

Consecuencias:

- En **TV con API ≥ 30** el USB es de lectura y escritura con acceso total.
- En **TV con API < 30** (Fire OS 7) ninguna vía permite escribir: el USB será **solo lectura** (analizar sí,
  limpiar o mover no), a confirmar en el Fire TV real, cuyo montaje de USB puede diferir del emulador.
- En **móvil** SAF sí existe (DocumentsUI completa) y sirve de alternativa en API < 30.
- La herramienta no distingue el *stub* de TV 14 de una cancelación del usuario (registra `NOT_RUN`).
  F1 debe detectarlo resolviendo el intent y descartando el paquete `com.android.tv.frameworkpackagestubs`.

## Navegación con mando y temas

El recorrido con mando destapó seis fallos de foco en TV, todos corregidos en esta rama y verificados en
emulador. Son reglas que la UI de TV de F1 debe aplicar desde el principio:

1. **Pedir el foco inicial un frame después** de componer (`withFrameNanos`). Sin esperar, el foco no se ve
   al arrancar y el `NavigationDrawer` no se despliega porque aún no ha terminado de inicializarse.
2. **Los botones deshabilitados no deben ser enfocables** (`focusProperties { canFocus = enabled }`):
   tv-material los deja enfocables sin indicador y el foco parece perderse. Excepción: los que solo se
   deshabilitan mientras dura una operación, que perderían el foco que tienen.
3. **El foco inicial se recalcula cuando cambia el objetivo**, no solo al entrar: el estado se carga después
   del primer frame y puede deshabilitar el botón que tenía el foco.
4. **Las listas a todo el ancho necesitan destino explícito** al bajar (`focusProperties { down = … }`); la
   búsqueda geométrica elige el botón más cercano al centro, no el primero.
5. **El texto dentro de superficies enfocables usa `LocalContentColor`**, no colores fijos: la superficie
   invierte su fondo al enfocarse y el texto quedaba ilegible.
6. **`inverseSurface`/`inverseOnSurface` salen de los tokens** (`onSurface`/`surface`); por defecto
   tv-material usa su propia paleta, fuera del tema y sin contraste garantizado.

Otros detalles corregidos: exportar a Descargas añadía `.txt` al nombre (MediaStore con `text/plain` y
extensión desconocida) y el resumen decía «1 correctos de 1».

Temas e idiomas: tema claro (D3, TV9) y oscuro (D4, D1) legibles en todas las pantallas; español completo con
números y decimales según el locale («11,0 s», «100.001 archivos»).

## Desviaciones respecto a la spec

- **Matriz con emuladores** en lugar de móvil y Fire TV físicos (SC-001, SC-005 piden D1 y D2). El Fire TV se
  sustituye por un emulador Android TV con el mismo nivel de API.
- **Benchmark de 100k solo en D1 y D4** (SC-003 lo pide en todo dispositivo con acceso total). En emuladores
  la cifra mide el PC, no el televisor, y generar 100.000 archivos cuesta 11–13 min por dispositivo. En D3 y
  TV9 se priorizaron permisos, USB y navegación.
- **V5** registra solo MediaStore como se espera, pero la pantalla no lo rotula como «modo limitado».
- La limpieza (V12) deja la carpeta `BurnFilesSpike/` vacía; no queda ningún archivo generado.

## Huecos conocidos

- **Fire TV real (Fire OS 7)**: permisos, rendimiento en hardware de TV, montaje del pendrive y escritura USB,
  e instalación con `PackageInstaller` en Fire OS (con la corrección de firma de Android 9). Es la principal
  validación pendiente; basta con repetir V1, V3, V6 y V8 con la herramienta y añadir sus resultados a este
  informe.
- **Coste de inserción en el índice (Room)**: se mide en F2.
- **TV real con Android 11+ sin pantalla de acceso total**: solo emulador (riesgo ya aceptado en el plan).
- **Verificación de desarrollador de Google en Fire OS**: no se puede validar en el spike.

## Consecuencias para F1

- Cadena de acceso: reutilizar la del spike (`AccessChain`, `SettingsLauncher`, `AccessMonitor`), con texto
  específico para la lista de TV 14 y el paso ADB de TV 11.
- Escaneo: `File.listFiles` con 4 hilos repartidos por carpeta de primer nivel; MediaStore solo en modo
  limitado.
- Volúmenes: capacidad de escritura por volumen (`READ_WRITE` / `READ_ONLY`) calculada al detectarlo; en TV
  con API < 30, solo lectura.
- UI de TV: las seis reglas de foco de este informe como criterios de aceptación de cada pantalla.
- Updater: promover `ApkInspector` con el fallback de Android 9, persistir la descarga verificada para
  sobrevivir a la muerte del proceso en Android 11 y comparar firmas por historial.
- Herramientas: los emuladores `BurnFiles_TV_API_28/30/34` quedan creados para las pruebas de F1.
- Constitución: no se proponen enmiendas. El USB de solo lectura en TV con API < 30 es una limitación de la
  plataforma que el principio IV (degradación elegante) ya cubre.
