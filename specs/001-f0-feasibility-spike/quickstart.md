# Quickstart: validar el spike F0

Guía para ejecutar el spike de punta a punta en la matriz de dispositivos y comprobar que cumple la
[spec](./spec.md). Los detalles de cada prueba están en [contracts/](./contracts/) y
[data-model.md](./data-model.md).

## 1. Preparar el entorno (una vez)

Requisitos: JDK 17, Android SDK con `platforms;android-36` y `build-tools;36.0.0` (ya instalados), ADB.

Instalar las imágenes de Android TV y crear los emuladores D3/D4 (y D5 opcional; API 28 sustituye al Fire TV
si no está disponible). Las herramientas vienen en *Android SDK Command-line Tools* (Android Studio › SDK
Manager › SDK Tools); también se pueden crear desde Android Studio › Device Manager eligiendo un perfil de TV.

En Windows, `sdkmanager.bat` y `avdmanager.bat` parten los argumentos por `;` cuando se llaman desde Git
Bash. Usar en su lugar la CLI `android` que viene en la misma carpeta:

```bash
android sdk install "system-images;android-28;android-tv;x86" "system-images;android-30;android-tv;x86" "system-images;android-34;android-tv;x86"
```

Los AVD necesitan `hw.dPad = yes`, `hw.sdCard = yes` (la SD virtual hace de pendrive) y una partición de datos
de al menos 4 GB para el conjunto sintético. Arrancar cada uno con:

```bash
emulator -avd BurnFiles_TV_API_30 -no-snapshot -no-boot-anim
```

Conectar el Fire TV (D2) por red (Opciones de desarrollador › Depuración ADB activada):

```bash
adb connect <ip-del-fire-tv>:5555
```

## 2. Compilar y comprobar calidad

Desde `spike/`:

```bash
./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug
```

Desde la raíz del repositorio:

```bash
python tools/check_strings.py spike/app/src/main/res
```

Tests de UI (diálogos destructivos, móvil y TV) con un emulador o dispositivo conectado:

```bash
./gradlew connectedDebugAndroidTest
```

**Esperado**: todo en verde; `spike/app/build/outputs/apk/debug/app-debug.apk` generado y < 15 MB.

## 3. Instalar en cada dispositivo

```bash
adb -s <serial> install -r spike/app/build/outputs/apk/debug/app-debug.apk
```

En la primera ejecución, elegir el identificador de matriz (D1–D5).

**Esperado**: aparece en el lanzador del móvil y en la fila de apps del TV con banner.

## 4. Escenarios de validación

| # | Escenario | Dispositivos | Resultado esperado |
|---|-----------|--------------|--------------------|
| V1 | Access › Iniciar diagnóstico; seguir la cadena hasta el final | D1–D5 | Nivel final registrado; ninguna pantalla ausente cierra la app |
| V2 | En un TV sin pantalla de acceso total, ejecutar el comando ADB mostrado y volver a la app | D3 (y D5) | Nivel `FULL` detectado al volver, vía `ADB` |
| V3 | Benchmark › Generar datos; Ejecutar benchmark | D1–D4 con acceso total | ≥ 100.000 archivos; tabla con mediana/peor por estrategia |
| V4 | Cancelar durante el benchmark | Cualquiera | Se detiene en < 2 s; la UI responde durante toda la ejecución |
| V5 | Benchmark en modo limitado | Uno sin acceso total | Solo `MEDIASTORE`, indicado en pantalla y en el registro |
| V6 | Update con release `spike-v0.0.2` publicada e instalada la 0.0.1 | D1, D2 (y D3/D4) | Tras confirmar, versión 0.0.2 instalada |
| V7 | Update con un `.sha256` manipulado (release de prueba alternativa) | D1 | `VERIFY_SHA256 / MISMATCH`, APK descartado |
| V8 | Usb con pendrive conectado | D2 | Resultado por operación en ambas sondas |
| V9 | Results › Exportar y recuperación por ADB | Todos | Archivo válido contra el esquema; sin rutas ni nombres de archivo |
| V10 | Recorrer toda la app solo con el mando | D2–D4 | Foco según [ui-navigation.md](./contracts/ui-navigation.md) |
| V11 | Cambiar el sistema a tema oscuro/claro y a español/inglés | D1 y D4 | Todas las pantallas legibles y traducidas |
| V12 | Benchmark › Limpiar datos | Todos | Diálogo con nº de elementos y tamaño; tras confirmar no queda `BurnFilesSpike/dataset/` |

Publicar la release de prueba (V6), con la APK compilada con `-PspikeVersionCode=2 -PspikeVersionName=0.0.2`.
El `.sha256` lleva dos espacios entre hash y nombre; en Windows `sha256sum` escribe `hash *nombre`, así que
conviene generarlo con `printf '%s  %s\n' "$(sha256sum f.apk | cut -d' ' -f1)" f.apk`:

```bash
gh release create spike-v0.0.2 --prerelease --title "Spike F0 test build 0.0.2" burnfiles-spike-0.0.2.apk burnfiles-spike-0.0.2.apk.sha256
```

Recuperar el registro por ADB (V9):

```bash
adb -s <serial> shell run-as es.sergiosahuquillo.burnfiles.spike cat files/results/results.jsonl > results-<matrixId>.jsonl
```

## 5. Cerrar el spike

Consolidar los `results-*.jsonl` en `docs/spike/f0-results.md` (tabla dispositivo × prueba, resumen del
benchmark y decisión por riesgo). La fase 0 se da por cerrada cuando el informe está en `main`.
