# Pendientes de la especificación de la viñeta (app Organizador)

Anotado el 19/10/2026 (revisiones 7 y 8) y el 21/10/2026 (revisiones 9 y 10). **No implementado todavía**: se hace en el próximo pedido.
**La lista oficial de cambios es `RallyRoadbook.Web/especificacion/CAMBIOS_PARA_APPS.md`**:
hacer todo lo que no diga "Hecho en apps" y, al terminar cada uno, marcarlo como hecho ahí.
El detalle completo está en `ESPECIFICACION_VINETA.md`, `simbolos.json` y
`flechas_armadas.json` (misma carpeta). Lo de abajo es mi resumen; si no coincide, manda
el archivo de la plataforma.

## Revisión 7 — formato del dibujo versión 2 (puntos 1, 3, 8, 9 y 12)

- Hoja como el rally mundial: cabecera 180 × 30 mm. Columnas Total 25 | Parcial 25 |
  Dirección 50 | Informaciones 50 | Regresiva 25 mm. Viñeta de 45 mm de alto. Número en
  cajita abajo a la izquierda de Dirección.
- Dibujo: 34 × 29 mm (100 × 85,29 unidades), centrado en Dirección con 8 mm de margen.
  Capa de notas: toda la celda de Informaciones, 50 × 45 mm (100 × 90 unidades).
- Se guarda `"v": 2`. Un dibujo con v=1 o sin v se convierte al leerlo:
  dibujo x' = 0,0735 + 0,8529·x, y' = y, tamaños ×0,8529; capa de notas y' = y·64/90;
  notasLetra ×72/50.
- Valores iniciales: símbolos scale 0,85 en el dibujo y 0,58 en notas; flechas armadas
  puestas como un cuadrado de 29 mm; texto 7 (dibujo) / 4,5 (notas).
- Letra automática de notas: entre 6,5 y 20 unidades; manual 4 a 28.
  Franja de carteles: 20 unidades.

## Revisión 8 — formato del dibujo versión 3 (puntos 1, 2, 3, 8, 8 bis y 9)

- El dibujo ocupa TODA la celda de Dirección: 50 × 45 mm = 100 × 90 unidades (igual que la
  capa de notas). Ya no hay margen de 8 mm. El número de viñeta va encima del dibujo,
  abajo a la izquierda.
- Se guarda `"v": 3`.
- Un dibujo con v=2 se convierte al leerlo (queda donde estaba):
  x' = 0,16 + 0,68·x ; y' = 0,1778 + 0,6444·y ; width, scale y size ×0,68.
  La capa de notas y notasLetra no cambian.
- Un dibujo con v=1 o sin v: primero se pasa a v2 (fórmulas de la revisión 7) y después a v3.
- Valores iniciales iguales en los dos recuadros: símbolos scale 0,58; texto 4,5;
  flechas armadas puestas como un cuadrado de 29 mm.

## Revisión 9 — el formato sigue en v3 (puntos 6 y 7)

- Palito del GPS (punto 6): campo opcional nuevo `gps.largo` = largo del palito en
  unidades (4 a 40). Si no está, el largo por defecto ahora es max(11, 2,2 × grosor de la
  flecha) (antes max(7, 1,5 × grosor)). En el editor, la bolita de la punta se arrastra
  para alargarlo o acortarlo.
- Goma de borrar (punto 7): corta los trazos de lápiz; un trazo cortado al medio se guarda
  como dos trazos. No cambia nada del JSON.

## Revisión 10 — el formato sigue en v3 (punto 6 bis: caminos prohibidos)

- Campo opcional nuevo en cada flecha: `"prohibidos": [ { "t": 0.5, "ang": -60, "largo": 17.4 }, ... ]`
  (los que hagan falta). t = de dónde sale sobre la flecha (0 a 1); ang = grados desde el
  sentido de la flecha en ese punto (+ derecha, − izquierda); largo = unidades.
- Dibujo: palito recto desde Q (punto en t) en la dirección de la tangente girada ang, de
  largo `largo`; grosor max(1,4; 0,45·w), color de la flecha. Dos rayitas a 0,75·largo ± s/2
  (s = max(2; 0,75·w)), de largo 2·max(2,8; 1,1·w), giradas −60° respecto del palito.
  Se dibujan antes que la flecha.
- `flechas_armadas.json` tiene 2 nuevas: `recta_1p` y `recta_2p` (13 en total).
- En las conversiones v1→v2 y v2→v3 también se multiplican `gps.largo` y el `largo` de
  cada prohibido.

## Notas para cuando se implemente

- Formato viejo de la app (columnas tulipSvgData / notasSvgData, punto 9 de antes): pasa
  primero a v1 (ConversionViejo), después a v2 y a v3.
- Recuadros en pantalla: dibujo y notas pasan a 100 × 90 (hoy 100 × 100 y 100 × 64).
- Revisar: palito del GPS, carteles (franja 20), letra de notas (rangos nuevos), margen del
  texto de notas, escala 0,56 en notas (queda reemplazada por 0,58), flechas armadas en un
  cuadrado de 29 mm, filas del editor con las proporciones de la hoja nueva.
- Palito: hoy la app usa max(7, 1,5 × grosor) y el arrastre de la bolita solo cambia
  t y lado; hay que sumar el largo (gps.largo) y conservarlo al leer y guardar.
  OJO: hoy la app, al guardar, reescribe gps solo con t y lado, así que un gps.largo que
  venga de la plataforma se perdería si se edita esa viñeta en la tablet.
- Prohibidos: hoy la app no los conoce; quedan guardados en `extra` de la flecha (no se
  pierden), pero no se dibujan. Falta modelo, dibujo, editor y volver a copiar
  flechas_armadas.json (13). Leer el punto 6 bis completo (cómo se agregan/editan).
- Goma: herramienta nueva en la barra del editor (no existe todavía en la app).
- Pregunta pendiente de la revisión 6 (0,56 vs 0,73) queda superada por los valores nuevos.
