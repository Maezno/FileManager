# FileManager – Correcciones del rediseño (parte 3)

Instrucciones para Gemini en Android Studio. Complementa `REDISENO_UI.md` y `REDISENO_UI_CORRECCIONES.md`. Esos dos siguen valiendo; este solo cubre **lo que quedó sin hacer o mal hecho** después de aplicar la parte 2.

Reglas: **solo UI**. No tocar ViewModel, servicios, modelos ni lógica. Compilá (`./gradlew assembleDebug`) al terminar cada paso y mostrá los cambios por archivo.

## 0. Estado verificado (31/10/2026, revisión de solo lectura)

**Ya está bien, no lo toques:**
- `Theme.kt`: existen `GlassTint`, `PopupSurface`, `PopupBorder`.
- `AppBackground` usa `hazeSource` y dibuja el degradado dentro de la fuente. `GlassSurface` usa `hazeEffect` con `GlassTint` por defecto.
- Existen `GlassAlertDialog`, `GlassDropdownMenu`, `GlassPanel` y `DialogWindowBlur`. Todos los `AlertDialog` de las pantallas ya usan `GlassAlertDialog`.
- `DropdownField` ya es una lista en línea con blur.
- `GlassBottomBar` tiene `navigationBarsPadding()`.
- `SettingsScreen.kt`: ya sin `BgDark`, márgenes 16/12, `GlassPanel`, botones de política y licencias en columna, `AppProgressBar` con degradado.

## 1. Lo que sigue fallando

### A. Los botones no tienen blur (causa principal del "100 % transparente")
En `Components.kt` tres componentes quedaron con `blur = false`:
- `PrimaryButton` (~línea 321): `fillColor = Transparent`, `blur = false`, y el degradado va con alphas de 0.50/0.38. Sin blur ni tinte, es solo un degradado semitransparente sobre el fondo: se ve transparente.
- `SecondaryButton` (~línea 399): `fillColor = GlassTint`, `blur = false`. Es solo un 10 % de blanco.
- `GlassIconButton` (~línea 442): igual.

La parte 2 pedía `blur = true` en `PrimaryButton` con el degradado **dentro** del vidrio. Se cambió a `false`. Los botones son pocos y chicos, así que el costo de rendimiento es despreciable.

### B. Dos menús siguen con la API vieja y transparentes
`SmartSearchScreen.kt`, líneas ~840 y ~950: siguen siendo `DropdownMenu(` con `containerColor = CardDark` (8 % de blanco) y `border = BorderStroke(1.dp, GlassBorder)`. Son el selector de ubicación y el selector de opciones del buscador. Son los únicos `containerColor` con alias translúcidos que quedan en todo el proyecto.

### C. Sin respaldo visual para Android < 12
`minSdk = 26`, pero `hazeEffect` solo desenfoca en Android 12+ (API 31). Con `blur = true` y un tinte del 10 % en un dispositivo más viejo, el vidrio sigue siendo casi invisible. Hoy `GlassSurface` no tiene ningún respaldo (el único `SDK_INT` del archivo es el de `DialogWindowBlur`).

### D. Textos menores de 12 sp que quedan
- `SettingsScreen.kt:447`: el texto de licencia está en 11 sp.
- `ExplorerScreen.kt`: 8 textos en 9–11 sp.
- `SmartSearchScreen.kt`: 4 textos en 9–11 sp.
- `NetworkScreen.kt`: ninguno.

### E. Superficies planas en pantallas
Estas `GlassSurface` usan `blur = false` con `GlassFill` (6 %), y se leen casi transparentes:
`ExplorerScreen.kt:1180` y `1281` (ítems de archivo, lista larga: **está bien dejarlos sin blur**), `SmartSearchScreen.kt:603` y `626` (selectores de carpetas: conviene blur), `988` y `1098` (ítems de resultados, lista larga: dejar sin blur pero subir el tinte).

## 2. Pasos

### Paso 1 – Botones con blur (`Components.kt`)

1. `PrimaryButton`: `blur = true`. Mantené `fillColor = Color.Transparent` y el `Box` interior con el degradado. Subí los alphas por defecto de `colors` a `Violet.copy(alpha = 0.60f)` y `Aqua.copy(alpha = 0.48f)` para que el botón se lea como botón y no como vidrio vacío.
2. `SecondaryButton`: `blur = true`, `fillColor = GlassTint`. Si el texto del botón es largo, quitá `maxLines = 1` cuando el botón sea `fillMaxWidth()` y dejá `overflow = Ellipsis` solo en los botones de ancho fijo.
3. `GlassIconButton`: `blur = true`, `fillColor = GlassTint`.
4. No uses `clickable` sobre el `modifier` externo si eso recorta el ripple fuera del `clip`. Si hay problemas, mové `clickable` al `Box` interior.

### Paso 2 – Respaldo para Android < 12 (`GlassSurface`)

Calculá el tinte efectivo una sola vez:

```kotlin
val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val effectiveFill = if (blur && !canBlur) fillColor.copy(alpha = maxOf(fillColor.alpha, 0.22f)) else fillColor
```

- Si `blur && hazeState != null && canBlur` → `hazeEffect` (como está).
- En cualquier otro caso → `Modifier.background(effectiveFill)`.

No cambies la firma de `GlassSurface`. Los componentes que ya usan `blur = false` no se tocan.

### Paso 3 – Los dos `DropdownMenu` del buscador (`SmartSearchScreen.kt`)

Reemplazá los `DropdownMenu(` de las líneas ~840 y ~950 por `GlassDropdownMenu(`, sin cambiar sus ítems, textos ni acciones. Quitá `shape`, `containerColor` y `border` de esos dos (el wrapper ya los define). Después buscá en todo el proyecto con "Find in Files" `DropdownMenu(` y comprobá que solo quede el de dentro de `GlassDropdownMenu`.

### Paso 4 – Superficies planas (`SmartSearchScreen.kt`)

- Líneas ~603 y ~626 (selectores de carpetas comparadas): `blur = true`, `fillColor = GlassTint`.
- Líneas ~988 y ~1098 (ítems de resultados en lista): dejá `blur = false` por rendimiento, pero cambiá `GlassFill` por `Color.White.copy(alpha = 0.09f)` para que no se vean transparentes.
- Lo mismo para `ExplorerScreen.kt:1180` y `1281`: `blur = false`, pero con el estado no seleccionado en `Color.White.copy(alpha = 0.09f)`.

### Paso 5 – Textos de 12 sp mínimo

- `SettingsScreen.kt:447`: de 11 sp a 12 sp.
- En `ExplorerScreen.kt` y `SmartSearchScreen.kt`: buscá `fontSize = 9.sp`, `10.sp` y `11.sp` y subilos a 12 sp, **excepto** insignias o contadores dentro de círculos o chips de tamaño fijo, donde se permite 11 sp. En esos casos revisá que el texto no se corte.
- Cuando subas un texto, agregá `lineHeight` de 16 sp y comprobá que no se pise con el de al lado.

### Paso 6 – Revisión de Ajustes en pantalla

Ejecutá la app y revisá Ajustes contra esta lista. Si algo no se cumple, corregilo:
- El degradado de fondo se ve igual que en las demás pestañas.
- Márgenes laterales de 16 dp, igual que Explorador y Buscador.
- Ningún texto cortado, ni el de los botones, ni el de «Caché de vistas previas remotas».
- El último card no queda pegado a la barra inferior.
- La barra de progreso de almacenamiento muestra el degradado violeta → aqua.

## 3. Criterios de aceptación

- En Android 12+ (API 31+): botón principal, secundarios y de ícono muestran el fondo **desenfocado** detrás; el principal conserva el degradado violeta → aqua visible.
- En Android 8–11: los botones y tarjetas con blur se ven como vidrio tintado visible, nunca transparentes.
- Los selectores de ubicación y de opciones del buscador abren un menú violeta oscuro casi opaco con borde fino.
- No quedan `DropdownMenu(` ni `containerColor = CardDark|BgElevated|InputDark` fuera de `GlassDropdownMenu`.
- Ningún texto de menos de 12 sp, salvo las insignias indicadas en el paso 5.
- Sin cambios de lógica: compila sin warnings nuevos y todas las opciones siguen funcionando.

## 4. Prompt para pegar en Gemini

> Seguí `REDISENO_UI_CORRECCIONES_3.md`. Es solo corrección visual: no toques ViewModel, servicios, modelos ni lógica. Empezá por el paso 1 (botones con `blur = true`) y el paso 2 (respaldo para Android < 12). Mostrame los cambios por archivo y compilá antes de pasar al siguiente.
