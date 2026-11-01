# Contrato: pantallas, navegación y foco (móvil y TV)

Concreta los criterios móvil/TV de la spec (principio I). Es la referencia de los tests de UI.

## Mapa de pantallas

```text
Home ──► Access      (US1)
     ├─► Benchmark   (US2)
     ├─► Update      (US3)
     ├─► Usb         (US4)
     └─► Results     (US5: lista + exportar)
```

- **Móvil**: `Home` es una lista de tarjetas; cada pantalla con barra superior y flecha atrás.
- **TV**: `NavigationDrawer` de `tv-material` con las cinco entradas; contenido a la derecha. Márgenes
  de overscan del 5 % en todos los bordes; objetivos de foco ≥ 48 dp de alto y texto de cuerpo ≥ 18 sp.

## Foco en TV por pantalla

| Pantalla | Foco inicial | Orden | Botón central | Atrás |
|----------|--------------|-------|---------------|-------|
| Home (drawer) | Primera entrada sin resultado registrado; si todas tienen, `Access` | Vertical en el drawer; → entra al contenido | Abre la pantalla | Sale de la app |
| Access | «Iniciar diagnóstico» | Pasos de arriba abajo; «Copiar comando» tras el comando ADB | Ejecuta el paso | Vuelve a Home |
| Benchmark | «Ejecutar benchmark» o «Generar datos» si no hay conjunto | Fila de acciones ←→, tabla debajo ↓ | Ejecuta la acción; en ejecución el foco salta a «Cancelar» | En ejecución: diálogo «¿Cancelar?» (foco en «No»); si no, Home |
| Update | «Comprobar release de prueba» | Comprobar → Descargar → Instalar | Ejecuta el paso | En descarga: diálogo «¿Cancelar?»; si no, Home |
| Usb | Primer volumen, o «Volver a buscar» | Lista ↓, «Ejecutar prueba» al final | Selecciona / ejecuta | Home |
| Results | «Exportar» | «Exportar» → lista de entradas ↓ | Exporta / expande la entrada | Home |

## Diálogos destructivos

Únicos en el spike: **limpiar conjunto sintético** (US2) y **descartar descarga** (US3).

- Muestran número de elementos y tamaño total formateados según el locale (principio II).
- Foco inicial en TV y botón por defecto en móvil: **Cancelar**.
- Confirmar exige una pulsación explícita del botón «Eliminar»; no hay gesto alternativo.

## Gestos

Ninguna acción depende de swipe, long-press ni multitouch. Las listas son desplazables con D-pad y con
táctil. No se usa teclado en pantalla.
