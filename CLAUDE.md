# Burn Files

Gestor de archivos para Android y Android TV centrado en liberar espacio.
Package `es.sergiosahuquillo.burnfiles` · Kotlin 2.x · Jetpack Compose · GPL-3.0.

## Constitución (prevalece sobre todo lo demás)

@.specify/memory/constitution.md

## Flujo de trabajo

- Spec-Driven Development con Spec Kit: `/speckit.specify` → `/speckit.plan` → `/speckit.tasks` → `/speckit.implement`. No escribir código de producción sin spec y plan aprobados.
- Todo plan incluye *Constitution Check* contra los principios I–VII; las excepciones van en *Complexity Tracking*.
- Commits en formato Conventional Commits. Una rama por feature (`NNN-nombre`, la crea Spec Kit).

## Reglas rápidas para Claude

- Cada spec lleva criterios de aceptación separados para móvil (táctil) y TV (D-pad: foco inicial, orden de foco, botón central, salida).
- Nada de GMS/Firebase. Nada de texto literal en código: `strings.xml` en `values/` (en) y `values-es/` (es).
- Colores solo vía tokens del tema; verificar claro y oscuro.
- Cero I/O en el hilo principal.
- Toda dependencia nueva: sin GMS, compatible GPL-3.0, impacto en APK justificado (límite 15 MB).

## Comandos

<!-- Rellenar cuando exista el proyecto Gradle -->
- Build: `./gradlew assembleDebug`
- Tests: `./gradlew test`
- Lint: `./gradlew ktlintCheck detekt`
