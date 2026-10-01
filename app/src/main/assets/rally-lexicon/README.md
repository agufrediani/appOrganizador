# 🏁 Frediani Rally Lexicon Standard (FIA / FIM / OpenRally)

Biblioteca de activos vectoriales unificada y catálogo indexado de simbología oficial para roadbooks digitales de rally.

---

## 📁 Estructura del Léxico

```text
rally-lexicon/
├── lexicon.json                # Manifiesto maestro con índices, categorías, tags y metadata
├── catalog_preview.html        # Visualizador interactivo de todos los iconos con buscador
├── controles/                  # Controles horarios y reglamentarios FIA
│   ├── tc_clock.svg
│   ├── start_flag.svg
│   ├── finish_flag.svg
│   ├── stop_sign.svg
│   ├── reset_0000.svg
│   ├── speed_dz.svg
│   ├── speed_fz.svg
│   ├── fuel_surtidor.svg
│   ├── assistance_wrench.svg
│   ├── medical_cross.svg
│   └── wp_visible.svg
├── peligros/                   # Grados de peligro y advertencia oficial
│   ├── danger_1.svg            # !
│   ├── danger_2.svg            # !!
│   ├── danger_3.svg            # !!!
│   └── warning_triangle.svg    # Precaución
├── landmarks/                  # Referencias del camino
│   ├── tranquera.svg
│   ├── guardaganado.svg
│   ├── puente.svg
│   ├── alcantarilla.svg
│   ├── vias_tren.svg
│   ├── antena.svg
│   ├── molino.svg
│   ├── casa_poblado.svg
│   └── iglesia.svg
├── terreno/                    # Accidentes y naturaleza del suelo
│   ├── vado_rio.svg
│   ├── salto_lomo.svg
│   ├── zanja.svg
│   ├── dunas.svg
│   ├── piedras.svg
│   └── barro_huellas.svg
└── tulipas/                    # Maniobras y trazados (Ball & Arrow)
    ├── recta.svg
    ├── curva_der.svg
    ├── curva_izq.svg
    ├── der_90.svg
    ├── izq_90.svg
    ├── horquilla_der.svg
    ├── horquilla_izq.svg
    ├── cruce_x.svg
    ├── bifurcacion_y.svg
    └── rotonda.svg
```

---

## ⚙️ Esquema en Base de Datos (PostgreSQL / SQLite / JSON)

Para garantizar un peso mínimo en la red y almacenamiento óptimo, la base de datos **nunca guarda código SVG crudo ni imágenes Base64**. Solo almacena los IDs de los iconos y sus coordenadas relativas normalizadas (`0.0` a `1.0`).

### Ejemplo de Viñeta en Base de Datos:
```json
{
  "numero": 23,
  "distanciaTotal": 18.750,
  "distanciaParcial": 1.420,
  "tulipa": {
    "codigo": "der_90",
    "cap": 142
  },
  "iconos_anotaciones": [
    { "id": "danger_2", "x": 0.25, "y": 0.30, "escala": 1.0 },
    { "id": "tranquera", "x": 0.70, "y": 0.35, "escala": 1.1 },
    { "id": "vado_rio", "x": 0.70, "y": 0.70, "escala": 0.9 }
  ],
  "texto": "Tranquera angosta, luego vado con agua profunda"
}
```
* **Peso por viñeta**: ~120 bytes.
* **Peso de un tramo de 300 viñetas**: ~36 KB (sincronización instantánea).

---

## 📱 Implementación en Android (Kotlin / Jetpack Compose)

En Android, los iconos se cargan directamente desde `assets/rally-lexicon/` usando cualquier cargador SVG (o Coil / AndroidSVG / Canvas nativo):

```kotlin
// Cargar SVG desde assets por ID de símbolo
fun loadLexiconSvg(context: Context, category: String, symbolId: String): InputStream {
    return context.assets.open("rally-lexicon/$category/$symbolId.svg")
}
```

---

## 🌐 Implementación en la Plataforma Web (Frontend)

En la plataforma web, los iconos residen en `/static/rally-lexicon/`.

```html
<!-- Ejemplo en HTML/React -->
<img src="/static/rally-lexicon/landmarks/tranquera.svg" width="48" height="48" alt="Tranquera" />
```

---

## 🤝 Compatibilidad OpenRally (.GPX)
El archivo `lexicon.json` mapea cada símbolo con su correspondiente `openrally_code` para que al exportar a `.gpx` estándar de OpenRally, cualquier odómetro comercial o software externo reconozca el símbolo.
