# Feature Specification: Spike F0 — Validación de viabilidad

**Feature Branch**: `001-f0-feasibility-spike`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Spike F0: validar acceso total al almacenamiento en móvil y TV, benchmark de recorrido de archivos, instalación de APK con PackageInstaller y escritura en USB desde TV" (fuente: `docs/plan-del-proyecto.md`, sección *Roadmap › Entregables del spike F0*)

## Contexto

Antes de construir ninguna pantalla de producto hay que confirmar cuatro supuestos de los que depende el
proyecto. Si alguno falla, el plan cambia (alcance en TV, motor de escaneo o mecanismo de actualización).
El spike es una **herramienta de diagnóstico para el desarrollador**, instalada por sideload en los
dispositivos de prueba; no se distribuye a usuarios finales. Su entregable principal es un **informe de
resultados** que alimenta la puerta F0 → F1.

**Matriz de dispositivos** (constitución, principio I):

| ID | Dispositivo | Versión | Entrada |
|----|-------------|---------|---------|
| D1 | Móvil físico | Android 13+ | Táctil |
| D2 | Fire TV físico | Fire OS 7 (Android 9, API 28) | Mando D-pad |
| D3 | Emulador Android TV | Android 11 (API 30) | D-pad |
| D4 | Emulador Android TV | Android 14 (API 34) | D-pad |
| D5 | Emulador Android TV *(opcional)* | Android 12 (API 31) | D-pad |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Diagnóstico de acceso al almacenamiento por dispositivo (Priority: P1)

Como desarrollador, instalo la herramienta en cada dispositivo de la matriz y le pido que obtenga el
máximo nivel de acceso al almacenamiento compartido posible. La herramienta recorre la cadena de
fallback definida en la constitución (acceso total → instrucciones de concesión por ADB → modo limitado
multimedia), me guía en cada paso, comprueba el resultado real al volver a primer plano y registra qué
nivel se consiguió y por qué vía.

**Why this priority**: es el riesgo más alto del proyecto. Si en Android TV 11/12 o Fire OS no hay forma
razonable de obtener acceso total, el alcance de la app en TV cambia por completo.

**Independent Test**: instalar solo esta parte en D1–D4, ejecutar el diagnóstico y obtener para cada
dispositivo un registro con el nivel de acceso obtenido y la vía usada.

**Acceptance Scenarios**:

1. **Given** un dispositivo con la pantalla de ajustes de acceso total disponible, **When** el
   desarrollador inicia el diagnóstico y concede el acceso en ajustes, **Then** al volver a la
   herramienta esta detecta el acceso total sin reiniciar y lo registra con la vía «ajustes».
2. **Given** un dispositivo donde la pantalla específica de acceso total no existe, **When** se inicia
   el diagnóstico, **Then** la herramienta intenta la pantalla genérica alternativa y, si tampoco existe,
   muestra las instrucciones de concesión por ADB con el comando exacto, sin cerrarse ni mostrar errores
   del sistema.
3. **Given** que el acceso se ha concedido por ADB con la herramienta abierta o en segundo plano,
   **When** la herramienta vuelve a primer plano, **Then** detecta el nuevo nivel y lo registra con la
   vía «ADB».
4. **Given** un dispositivo con modelo de permisos antiguo (API < 30, p. ej. D2), **When** se inicia el
   diagnóstico, **Then** la herramienta solicita el permiso clásico de lectura/escritura mediante el
   diálogo del sistema y registra el resultado.
5. **Given** que el desarrollador rechaza o no puede conceder el acceso total, **When** termina el
   diagnóstico, **Then** la herramienta comprueba y registra qué cubre el modo limitado (imágenes,
   vídeo, audio, descargas) contando los elementos visibles en cada categoría.

**Criterios móvil (D1)**: cada paso se activa con un toque; las instrucciones de ADB se pueden copiar
al portapapeles.

**Criterios TV (D2–D5)**:
- Foco inicial: botón «Iniciar diagnóstico».
- Orden de foco: de arriba abajo en el orden visual de los pasos; ningún elemento interactivo queda
  inalcanzable con el D-pad.
- Botón central: ejecuta el paso enfocado.
- Salida: «Atrás» vuelve a la pantalla principal de la herramienta; desde ella, «Atrás» sale de la app.
- Las instrucciones de ADB son legibles a 3 m (texto grande, comando en una sola línea si cabe) y
  quedan dentro de los márgenes de overscan.

---

### User Story 2 - Benchmark de estrategias de recorrido (Priority: P2)

Como desarrollador, quiero medir en cada dispositivo cuánto tarda y cuánto cubre cada estrategia
candidata para descubrir los archivos del almacenamiento compartido: (a) recorrido completo del árbol
con la API moderna de sistema de archivos, (b) recorrido completo con la API clásica de listado de
directorios y (c) consulta al índice multimedia del sistema. Para que sea comparable entre dispositivos,
la herramienta puede generar un conjunto sintético de al menos 100.000 archivos en una carpeta propia
claramente identificada, y también puede ejecutarse en modo solo lectura sobre los datos reales.

**Why this priority**: decide la arquitectura del motor de escaneo y si los objetivos de rendimiento de
la constitución (200.000 archivos en < 30 s móvil, < 60 s TV) son alcanzables.

**Independent Test**: en un dispositivo con acceso total, generar el conjunto sintético, ejecutar las
tres estrategias y obtener una tabla con tiempo, número de archivos, número de carpetas, bytes totales
y archivos por segundo para cada una.

**Acceptance Scenarios**:

1. **Given** acceso total concedido, **When** el desarrollador genera el conjunto sintético, **Then** se
   crean al menos 100.000 archivos con una distribución de profundidades y tamaños documentada, dentro
   de una única carpeta raíz del spike, y la herramienta informa del progreso y del total al terminar.
2. **Given** el conjunto sintético generado, **When** se ejecuta el benchmark, **Then** cada estrategia
   se ejecuta al menos 3 veces y se informa de la mediana y de la peor ejecución.
3. **Given** un benchmark en curso, **When** el desarrollador lo cancela, **Then** la ejecución se
   detiene en menos de 2 s y la interfaz sigue respondiendo en todo momento.
4. **Given** un benchmark en curso, **Then** se muestra el progreso (archivos contados y tiempo
   transcurrido) al menos una vez por segundo.
5. **Given** modo limitado (sin acceso total), **When** se ejecuta el benchmark, **Then** solo se
   ejecuta la estrategia (c) y el informe lo indica explícitamente.
6. **Given** el modo solo lectura sobre datos reales, **When** se ejecuta, **Then** no se crea, modifica
   ni elimina ningún archivo fuera de la carpeta del spike.

**Criterios móvil (D1)**: botones «Generar datos», «Ejecutar benchmark», «Cancelar» y «Limpiar datos»
accesibles con un toque.

**Criterios TV (D2–D5)**:
- Foco inicial: «Ejecutar benchmark» (o «Generar datos» si no existe el conjunto sintético).
- Orden de foco: acciones en fila de izquierda a derecha y, debajo, la tabla de resultados desplazable
  con el D-pad.
- Botón central: activa la acción enfocada; durante una ejecución el foco pasa a «Cancelar».
- Salida: «Atrás» durante una ejecución pide confirmación para cancelar; sin ejecución, vuelve a la
  pantalla principal.

---

### User Story 3 - Instalación de una actualización descargada (Priority: P3)

Como desarrollador, quiero comprobar en móvil y TV que la herramienta puede descargar un paquete de
instalación publicado como release del repositorio del proyecto, verificar su suma SHA-256 y que está
firmado con el mismo certificado que la versión instalada, y lanzar su instalación con confirmación del
usuario, de forma que la versión instalada quede actualizada.

**Why this priority**: el updater va en F1 para que cada build de prueba llegue sola a los
dispositivos; si la instalación no funciona en Fire OS o en Android TV, hay que cambiar de estrategia
antes de construirlo.

**Independent Test**: con la versión N instalada y la versión N+1 publicada en una release de prueba,
ejecutar el flujo y comprobar que la versión instalada pasa a ser N+1.

**Acceptance Scenarios**:

1. **Given** la versión N instalada y una release con la versión N+1 y su checksum, **When** el
   desarrollador inicia la prueba, **Then** el paquete se descarga con progreso visible y se verifica su
   SHA-256 contra el publicado.
2. **Given** que el checksum no coincide o el certificado de firma es distinto del instalado, **When** se
   verifica, **Then** la instalación se aborta, el paquete descargado se descarta y se registra el
   motivo.
3. **Given** que la herramienta no tiene permiso para instalar paquetes, **When** se intenta instalar,
   **Then** se guía al desarrollador al ajuste correspondiente (con protección si la pantalla no
   existe) y, al volver, se reanuda el flujo.
4. **Given** paquete verificado y permiso concedido, **When** el desarrollador confirma, **Then** se
   muestra la confirmación del sistema y, tras aceptarla, la versión instalada es N+1 y se registra el
   resultado.
5. **Given** que el desarrollador cancela la confirmación del sistema, **Then** la versión N sigue
   instalada y funcional y se registra el resultado «cancelado».

**Criterios móvil (D1)**: la prueba completa se realiza solo con toques.

**Criterios TV (D2–D5)**:
- Foco inicial: «Comprobar release de prueba».
- Orden de foco: comprobar → descargar → instalar, en el orden del flujo.
- Botón central: ejecuta el paso enfocado; las confirmaciones del sistema se pueden aceptar y cancelar
  con el mando.
- Salida: «Atrás» cancela la descarga en curso (con confirmación) y vuelve a la pantalla principal.

---

### User Story 4 - Escritura en una memoria USB desde TV (Priority: P4)

Como desarrollador, quiero comprobar si con el nivel de acceso obtenido la herramienta puede detectar
una memoria USB conectada al TV, escribir en ella un archivo de prueba, leerlo de vuelta comparando su
contenido y eliminarlo, registrando qué operaciones funcionan en cada versión.

**Why this priority**: muchos usuarios de TV liberan espacio moviendo contenido a pendrives; determina
si la gestión de USB entra en v1.0 tal como está planificada.

**Independent Test**: con un pendrive conectado a D2 (y, si el emulador lo permite, a D3/D4 mediante un
volumen extraíble simulado), ejecutar la prueba y obtener un resultado por operación (detectar,
escribir, leer y verificar, eliminar).

**Acceptance Scenarios**:

1. **Given** un pendrive conectado, **When** se inicia la prueba, **Then** la herramienta lista los
   volúmenes extraíbles detectados con su nombre, capacidad y espacio libre.
2. **Given** un volumen seleccionado, **When** se ejecuta la prueba, **Then** se escribe un único archivo
   de prueba con nombre identificable del spike, se lee y se compara byte a byte, y se informa del
   resultado de cada operación por separado.
3. **Given** que alguna operación falla, **Then** se registra la operación que falló y el motivo
   informado por el sistema, sin cerrar la herramienta.
4. **Given** ningún volumen extraíble conectado, **Then** la herramienta lo indica y no ofrece ejecutar la
   prueba.
5. **Given** que el pendrive se desconecta durante la prueba, **Then** la operación en curso falla de
   forma controlada y se registra como «volumen retirado».

**Criterios móvil (D1)**: opcional (USB OTG); si se prueba, mismos escenarios con táctil.

**Criterios TV (D2–D5)**:
- Foco inicial: primer volumen de la lista (o «Volver a buscar» si no hay ninguno).
- Orden de foco: lista de volúmenes y, debajo, «Ejecutar prueba».
- Botón central: selecciona el volumen enfocado / ejecuta la prueba.
- Salida: «Atrás» vuelve a la pantalla principal.

---

### User Story 5 - Informe de resultados y decisión de puerta F0 → F1 (Priority: P1)

Como desarrollador, quiero que cada prueba deje un registro estructurado que pueda exportar desde el
dispositivo y consolidar en un único informe del spike en el repositorio, con una conclusión explícita
por riesgo (seguir según plan / seguir con cambios / bloquear) que sirva para cerrar la fase 0.

**Why this priority**: sin informe consolidado el spike no cumple su función; comparte prioridad con
la US1 porque es lo que convierte las pruebas en decisiones.

**Independent Test**: tras ejecutar al menos una prueba en un dispositivo, exportar el registro y
comprobar que contiene dispositivo, versión del sistema, fecha, prueba, resultado y métricas.

**Acceptance Scenarios**:

1. **Given** pruebas ejecutadas, **When** el desarrollador exporta los resultados, **Then** obtiene un
   archivo legible por máquina y una vista resumida legible por humanos.
2. **Given** el registro exportado, **Then** no contiene rutas personales, nombres de archivos del
   usuario ni contenido de archivos fuera de la carpeta del spike (principio VI).
3. **Given** resultados de toda la matriz, **When** se consolida el informe en el repositorio, **Then**
   contiene una tabla dispositivo × prueba y una decisión con justificación para cada uno de los
   cuatro riesgos.

**Criterios móvil y TV**: la exportación se puede lanzar con un toque o con el botón central; en TV,
cuando no haya forma de compartir el archivo, el registro también se puede recuperar por ADB desde una
ubicación documentada.

---

### Edge Cases

- La pantalla de ajustes de acceso total existe pero no tiene efecto (el interruptor no se puede
  activar): se registra como «pantalla presente, concesión imposible» y se continúa con el fallback.
- El usuario revoca el acceso mientras hay un benchmark en curso: la ejecución se detiene de forma
  controlada y se registra como interrumpida.
- Espacio libre insuficiente para el conjunto sintético: la herramienta calcula el espacio necesario
  antes de empezar y no inicia la generación si no cabe con un margen del 10 %.
- La herramienta se cierra o el sistema la mata durante la generación del conjunto sintético: al
  reabrir detecta el conjunto incompleto y ofrece completarlo o limpiarlo.
- Sin red al probar la actualización: error claro y posibilidad de reintentar; no se deja ningún
  archivo parcial.
- Límite de peticiones de la API de releases alcanzado: se informa del tiempo de espera indicado por
  el servidor.
- El pendrive está en un formato de solo lectura para el sistema: se registra «solo lectura».
- Fire TV con Vega OS: fuera de alcance (no instala APKs); se documenta en el informe.
- Idioma del sistema distinto de español e inglés: la herramienta se muestra en inglés.

## Requirements *(mandatory)*

### Functional Requirements

**Acceso al almacenamiento (US1)**

- **FR-001**: La herramienta MUST determinar en cada arranque y cada vez que vuelve a primer plano el
  nivel de acceso actual: total, clásico (API < 30), limitado multimedia o ninguno.
- **FR-002**: La herramienta MUST aplicar la cadena de fallback en este orden: pantalla específica de
  acceso total → pantalla genérica de acceso total → instrucciones de concesión por ADB → modo limitado.
- **FR-003**: Toda apertura de pantallas de ajustes del sistema MUST estar protegida para que su
  ausencia no cierre la herramienta y se registre como «pantalla no disponible».
- **FR-004**: Las instrucciones de ADB MUST mostrar el comando exacto con el identificador del paquete
  de la herramienta y permitir copiarlo cuando el dispositivo tenga portapapeles.
- **FR-005**: En modo limitado, la herramienta MUST contar los elementos accesibles por categoría
  (imágenes, vídeo, audio, descargas) para cuantificar su cobertura.

**Benchmark (US2)**

- **FR-006**: La herramienta MUST ofrecer tres estrategias de descubrimiento de archivos ejecutables por
  separado y comparables con las mismas métricas: duración, archivos, carpetas, bytes totales y archivos
  por segundo.
- **FR-007**: La herramienta MUST poder generar un conjunto sintético reproducible (misma semilla →
  mismo árbol) de al menos 100.000 archivos con profundidad y distribución de tamaños documentadas.
- **FR-008**: Todo dato generado por el spike MUST residir en una única carpeta raíz propia del spike,
  y la herramienta MUST NOT crear, modificar ni eliminar nada fuera de ella (salvo el archivo de prueba
  en el volumen USB de la US4).
- **FR-009**: Toda operación de disco o red MUST ejecutarse fuera del hilo de interfaz, ser cancelable
  y emitir progreso al menos una vez por segundo.
- **FR-010**: La limpieza del conjunto sintético MUST pedir confirmación indicando número de elementos
  y tamaño total, y MUST limitarse a la carpeta raíz del spike.
- **FR-011**: La herramienta MUST NOT acceder ni intentar acceder a rutas del sistema, `Android/data`,
  `Android/obb` ni datos privados de otras apps; las estrategias de recorrido MUST excluir esas rutas.

**Actualización (US3)**

- **FR-012**: La herramienta MUST consultar la release de prueba del repositorio del proyecto, descargar
  el paquete adjunto con progreso y verificar el SHA-256 publicado antes de cualquier instalación.
- **FR-013**: La herramienta MUST verificar que el certificado de firma del paquete descargado coincide
  con el de la versión instalada y abortar si no coincide.
- **FR-014**: La instalación MUST requerir siempre la confirmación explícita del usuario.
- **FR-015**: Las únicas comunicaciones de red MUST ser con la API y las descargas de GitHub Releases.

**USB (US4)**

- **FR-016**: La herramienta MUST listar los volúmenes extraíbles montados con nombre, capacidad y
  espacio libre.
- **FR-017**: La prueba USB MUST informar del resultado de detectar, escribir, leer y verificar, y
  eliminar como operaciones independientes, usando un único archivo de prueba de nombre identificable.

**Registro e informe (US5)**

- **FR-018**: Cada prueba MUST generar una entrada de registro con: modelo de dispositivo, versión del
  sistema y nivel de API, tipo de interfaz (móvil/TV), fecha y hora, prueba, resultado y métricas.
- **FR-019**: El registro MUST NOT contener rutas, nombres ni contenido de archivos del usuario.
- **FR-020**: El registro MUST poder exportarse como archivo y recuperarse por ADB desde una ubicación
  documentada.
- **FR-021**: El repositorio MUST incluir un informe consolidado del spike con la tabla dispositivo ×
  prueba y una decisión justificada por riesgo.

**Transversales (constitución)**

- **FR-022**: La misma build MUST funcionar en móvil (táctil) y TV (D-pad) y aparecer en el lanzador de
  ambos tipos de dispositivo.
- **FR-023**: Todos los textos visibles MUST estar disponibles en inglés (base) y español.
- **FR-024**: La herramienta MUST verse correctamente en tema claro y oscuro.
- **FR-025**: La herramienta MUST NOT depender de servicios de Google ni enviar telemetría.

### Key Entities

- **Dispositivo de prueba**: identificador de la matriz (D1–D5), modelo, versión del sistema, nivel de
  API, tipo de interfaz.
- **Resultado de prueba**: dispositivo, prueba (acceso, benchmark, actualización, USB), resultado
  (éxito, fallo, no aplicable, cancelado), vía o motivo, métricas, fecha.
- **Ejecución de benchmark**: estrategia, conjunto de datos (sintético o real), número de repetición,
  duración, archivos, carpetas, bytes, archivos por segundo.
- **Conjunto sintético**: semilla, número de archivos y carpetas, tamaño total, distribución, estado
  (completo/incompleto).
- **Informe del spike**: tabla consolidada de resultados y decisión por riesgo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los dispositivos obligatorios de la matriz (D1–D4) tienen resultado
  registrado para las pruebas de acceso, benchmark y actualización; D2 además para USB.
- **SC-002**: En ningún dispositivo la herramienta se cierra inesperadamente durante el diagnóstico de
  acceso, incluidos los que no tienen pantalla de acceso total.
- **SC-003**: El benchmark recorre al menos 100.000 archivos en cada dispositivo con acceso total y
  permite estimar con datos medidos si 200.000 archivos caben en 30 s (móvil) y 60 s (TV).
- **SC-004**: La cancelación de cualquier operación larga se hace efectiva en menos de 2 s.
- **SC-005**: La actualización de la versión N a N+1 se completa con éxito en al menos D1 y D2, o se
  documenta la causa exacta del fallo.
- **SC-006**: Toda la herramienta se puede usar de principio a fin con el mando en D2–D4 sin tocar la
  pantalla ni usar teclado.
- **SC-007**: Tras la limpieza, no queda ningún archivo generado por el spike en ningún dispositivo
  fuera de los registros exportados.
- **SC-008**: El informe consolidado con decisión por riesgo está en el repositorio en un máximo de 7
  días de trabajo desde el inicio del spike.

## Assumptions

- El spike vive en el repositorio como una app de diagnóstico separada del producto; no forma parte de
  la APK de Burn Files ni se publica como release estable. El código que resulte útil (cadena de
  permisos, verificación de paquetes) podrá promoverse a los módulos de producto en F1 mediante su
  propia spec.
- La herramienta usa un identificador de paquete propio del spike para no interferir con la futura app
  ni con su firma.
- La release de prueba de la US3 se publica en el repositorio del proyecto como pre-release marcada
  como del spike; el updater de producto ignorará las pre-releases, pero el spike las consulta a
  propósito.
- Las versiones N y N+1 del spike se firman con una clave de pruebas del desarrollador, distinta del
  keystore de producción.
- El emulador de Android TV puede no permitir simular un pendrive; en ese caso la US4 se valida solo en
  D2 y se documenta.
- D5 (Android TV 12) es opcional; si no se prueba, el informe lo indica como hueco conocido.
- La distribución de profundidades y tamaños del conjunto sintético se basa en un perfil típico de
  almacenamiento móvil (muchas carpetas poco profundas, mayoría de archivos pequeños y pocos grandes).
- El desarrollador tiene acceso ADB por red al Fire TV y a los emuladores.
