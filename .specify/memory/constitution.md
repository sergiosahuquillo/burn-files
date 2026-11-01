# Burn Files — Constitución del proyecto

Gestor de archivos para Android y Android TV centrado en liberar espacio. Este documento define los principios no negociables que toda especificación, plan y tarea debe cumplir.

## Principios fundamentales

### I. Paridad móvil y TV (NO NEGOCIABLE)

- Toda funcionalidad DEBE funcionar en móvil (táctil) y en Android TV / Fire OS (mando D-pad) desde la misma APK.
- Toda spec DEBE incluir criterios de aceptación separados para móvil y TV. En TV: foco inicial, orden de foco, acción del botón central y forma de salir de la pantalla.
- Ninguna interacción puede depender solo de swipe, long-press, multitouch o teclado en pantalla sin una alternativa equivalente con D-pad.
- Matriz mínima de verificación: móvil Android 13+, Fire TV con Fire OS 7 (API 28), emuladores Android TV API 30 y API 34.

**Motivo**: la compatibilidad con Android TV es un requisito obligatorio del proyecto; si no se especifica desde el principio, se rompe por omisión.

### II. Seguridad del borrado (NO NEGOCIABLE)

- Ningún archivo se elimina definitivamente sin pasar por la papelera interna, salvo que el usuario la desactive de forma explícita.
- La retención por defecto de la papelera es de 7 días y es configurable.
- Toda acción destructiva muestra confirmación con el número de elementos y el tamaño total.
- Los detectores NUNCA preseleccionan contenido de carpetas sensibles (`DCIM`, `Pictures`, `Documents`, carpetas de apps de mensajería, copias de seguridad). Solo se preseleccionan: APKs ya instalados, archivos temporales, miniaturas regenerables y duplicados conservando siempre una copia.
- Copiar y mover son recuperables ante cancelación o fallo: el origen no se elimina hasta verificar el destino.

**Motivo**: una app que libera espacio pierde toda la confianza del usuario con un solo borrado indebido.

### III. Perímetro del sistema

- Solo se analiza y modifica el almacenamiento compartido del usuario y los volúmenes extraíbles (SD, USB).
- PROHIBIDO acceder o intentar acceder a rutas del sistema, `Android/data`, `Android/obb` o datos privados de otras apps.
- Sin root. Cualquier integración con Shizuku requiere enmienda de esta constitución.

### IV. Degradación elegante de permisos

- Cada funcionalidad que dependa de un permiso especial DEBE definir su comportamiento cuando ese permiso no está concedido.
- Orden de fallback del acceso al almacenamiento: acceso total (`MANAGE_EXTERNAL_STORAGE` o permisos legacy en API < 30) → instrucciones para concederlo por ADB → modo limitado con `MediaStore`.
- Todo intent hacia ajustes del sistema se protege frente a `ActivityNotFoundException`.
- El estado de permisos se reevalúa cada vez que la app vuelve a primer plano.

**Motivo**: en Android TV 11/12 y Fire OS 8 la pantalla de acceso total puede no existir.

### V. Internacionalización y temas

- Ningún texto visible al usuario en el código: todo en `strings.xml` / `<plurals>`, con inglés como base (`values/`) y español (`values-es/`).
- Una funcionalidad no está terminada sin ambos idiomas completos; la CI falla si una clave falta en algún idioma.
- Tamaños, fechas y números se formatean según el locale activo.
- Toda pantalla DEBE verse correctamente en tema claro y oscuro. Los colores solo se usan a través de tokens del tema, nunca como valores literales. Contraste mínimo WCAG AA.

### VI. Independencia de Google y privacidad

- PROHIBIDO usar Google Play Services, Firebase o cualquier dependencia de GMS (Fire OS no los incluye).
- Únicas comunicaciones de red permitidas: la API y las descargas de GitHub Releases, y Sentry solo si el usuario lo activa.
- Sentry es opt-in y está desactivado por defecto. Los eventos NUNCA incluyen rutas, nombres de archivo ni contenido de archivos.
- Sin analítica ni telemetría adicional.

### VII. Rendimiento en hardware modesto

- Ninguna operación de I/O en el hilo principal. El escaneo es cancelable y emite progreso.
- Objetivos: 200.000 archivos en menos de 30 s en un móvil de gama media y menos de 60 s en TV.
- El dashboard muestra datos del índice previo en menos de 1 s desde el arranque.
- Presupuesto de tamaño de APK: 15 MB como máximo.

## Restricciones técnicas

- Kotlin 2.x, Jetpack Compose, Material 3 (móvil) y `androidx.tv:tv-material` (TV).
- Arquitectura MVVM con flujo de datos unidireccional; Hilt, Room, DataStore y WorkManager.
- `minSdk` 26; `targetSdk` el mínimo exigido vigente (36).
- Package: `es.sergiosahuquillo.burnfiles`. Una sola APK; la capa de UI (móvil o TV) se selecciona en runtime.
- Módulos organizados por dominio: `app`, `core:*`, `feature:*`. El updater vive aislado en `feature:updater` para poder generar en el futuro una variante sin él.
- Toda dependencia nueva debe: no requerir GMS, ser compatible con GPL-3.0 y justificar su impacto en el tamaño de la APK.
- Licencia del proyecto: GPL-3.0.

## Distribución y actualizaciones

- Las releases solo se generan desde tags `vX.Y.Z` mediante GitHub Actions: APK firmada con el keystore del proyecto y checksum SHA-256 publicado junto a ella.
- Versionado semántico; `versionCode` derivado del tag (`major * 10000 + minor * 100 + patch`).
- El updater verifica el SHA-256 y el certificado de firma antes de instalar, avisa con el changelog y pide confirmación. Las pre-releases se ignoran.
- Una release que impida actualizar desde la versión anterior es un bloqueante.

## Flujo de desarrollo y calidad

- Spec-Driven Development con Spec Kit: spec → plan → tareas antes de escribir código.
- Todo plan pasa el *Constitution Check* contra los siete principios. Cualquier excepción se documenta en *Complexity Tracking* con su justificación.
- Tests obligatorios: unitarios para detectores, motor de escaneo y lógica del updater (comparación de versiones y verificación); tests de UI de Compose para los flujos destructivos (incluida la papelera) en móvil y TV.
- ktlint y detekt sin errores; CI en verde obligatoria para hacer merge.
- Commits con formato Conventional Commits.

## Gobernanza

- Esta constitución prevalece sobre cualquier otra práctica o preferencia del proyecto.
- Las enmiendas se hacen mediante PR que modifica este archivo e incluye: motivo, nueva versión y revisión de las specs afectadas.
- Versionado de la constitución: MAJOR al eliminar o redefinir un principio, MINOR al añadir un principio o sección, PATCH para aclaraciones.
- Se revisa al cerrar cada fase del roadmap.

**Versión**: 1.0.0 | **Ratificada**: 2026-10-31 | **Última enmienda**: 2026-10-31
