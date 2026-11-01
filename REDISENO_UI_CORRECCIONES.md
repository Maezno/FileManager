# FileManager – Correcciones del rediseño (parte 2)

Instrucciones para Gemini en Android Studio. Este documento **complementa** `REDISENO_UI.md`: ese archivo sigue valiendo para el estilo; este corrige lo que quedó mal. Mismas reglas: **solo UI**, no tocar ViewModel, servicios, modelos ni lógica. Compilá (`./gradlew assembleDebug`) al terminar cada paso.

## 1. Qué se revisó y qué falla

Se leyeron `Components.kt`, `Theme.kt`, `MainActivity.kt`, `SettingsScreen.kt` y `libs.versions.toml` (Haze 1.3.1).

### A. Los botones y tarjetas no desenfocan nada
1. **`Components.kt`, `AppBackground` (~línea 125-150):** el degradado se dibuja con `.drawBehind { ... }` y **después** viene `.haze(state = hazeState)`. Haze solo captura el contenido que queda *dentro* del nodo; lo que se dibuja con un modifier anterior queda fuera. Resultado: la fuente del desenfoque está vacía y el vidrio muestra "transparente".
2. **El fondo es demasiado liso.** Aunque el blur funcione, desenfocar un degradado suave casi no se nota. Hace falta algo con forma detrás (orbes de luz) y un tinte de vidrio algo más fuerte.
3. **`PrimaryButton`:** el degradado se aplica como `.background(Brush...)` en el modifier de `GlassSurface`, es decir *antes* del efecto Haze, y además se pasa `fillColor = Transparent`. El efecto lo tapa o lo anula.
4. **API vieja:** se usan `Modifier.haze()` y `Modifier.hazeChild()`, deprecadas en Haze 1.x. La API actual es `hazeSource()` y `hazeEffect()`.

### B. Los desplegables (y los diálogos) se ven 100 % transparentes
1. **`Theme.kt`:** los alias de compatibilidad quedaron translúcidos: `CardDark = #14FFFFFF` (8 % blanco), `BgElevated = #1EFFFFFF` (12 %), `InputDark = #0CFFFFFF` (5 %).
2. Esos alias se usan como `containerColor` de **todos** los `DropdownMenu` y `AlertDialog`: `Components.kt:797`; `ExplorerScreen.kt:624, 887, 923, 964, 1029, 1065, 1105, 1384`; `NetworkScreen.kt:304`; `SmartSearchScreen.kt:233, 277, 844, 954, 1202`; `SettingsScreen.kt:378, 443`; `MainActivity.kt:198`.
3. `DropdownMenu`, `AlertDialog` y `Dialog` son **ventanas aparte**: Haze no puede difuminar el fondo de otra ventana. Con un 8 % de blanco, el menú queda casi invisible sobre el contenido.

### C. Ajustes (`SettingsScreen.kt`)
1. Línea 94: `Box(...).background(BgDark)` pinta un color liso sobre toda la pantalla y tapa el degradado de fondo.
2. Líneas 143, 278, 328, 476: paneles internos con `.background(InputDark)` (opacos/planos), no son vidrio.
3. Márgenes distintos al resto de pantallas: Ajustes usa `padding(horizontal = 14.dp, vertical = 10.dp)` y separación 14 dp; Explorador, Buscador y Red LAN usan `16.dp / 12.dp` y separación 10-12 dp.
4. Tamaños de texto demasiado chicos: títulos de fila 13 sp, subtítulos y valores 10-11 sp, textos de panel 12 sp. La especificación pide 14 sp / 12 sp mínimo.
5. Los dos `SecondaryButton` de «Política de Privacidad» y «Licencias Open Source» van en una `Row` con `maxLines = 1`, altura fija de 44 dp y relleno de 14 dp: el texto se corta.
6. La fila «Caché de vistas previas remotas» + «Limpiar ahora» no reserva espacio para el botón y el texto se aprieta.
7. Colores de la pantalla aún con alias viejos (`AccentBlueLight`, `AccentGreenLight`, `AccentPurple`): hoy todos valen violeta o aqua, así que los íconos quedan monótonos.

### D. Barra inferior
`GlassBottomBar` no aplica `navigationBarsPadding()`; con edge-to-edge queda pegada a la barra de gestos.

## 2. Pasos de corrección

### Paso 1 – Tokens nuevos en `Theme.kt` (sin borrar los alias)

```kotlin
val GlassTint = Color(0x1AFFFFFF)        // vidrio con blur: blanco 10 %
val PopupSurface = Color(0xF21A1238)     // menús y diálogos: violeta oscuro 95 %
val PopupBorder = Color(0x33FFFFFF)
```

No uses más `CardDark`, `BgElevated` ni `InputDark` como `containerColor`. Dejalos definidos para no romper otros usos.

### Paso 2 – Migrar Haze y rehacer el fondo (`Components.kt`)

Imports nuevos: `dev.chrisbanes.haze.hazeSource`, `dev.chrisbanes.haze.hazeEffect`, `dev.chrisbanes.haze.HazeTint`. Quitá `haze` y `hazeChild`. Si el autocompletado del IDE muestra otros nombres de parámetros en 1.3.1, usá los que sugiera.

**`AppBackground`**: el fondo pasa a ser un **hermano** que hace de fuente, con `hazeSource` **antes** de `drawBehind`. La UI va encima.

```kotlin
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)   // 1º la fuente...
                    .drawBehind {                    // 2º ...y dentro de ella, el dibujo
                        drawRect(Brush.linearGradient(
                            listOf(BgTop, BgMid, BgBottom),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.3f, size.height)))
                        fun orb(c: Color, cx: Float, cy: Float, r: Float, a: Float) =
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to c.copy(alpha = a),
                                    0.6f to c.copy(alpha = a * 0.55f),
                                    1f to Color.Transparent,
                                    center = Offset(cx, cy), radius = r),
                                radius = r, center = Offset(cx, cy))
                        orb(Aqua,   size.width * 0.88f, size.height * 0.10f, size.width * 0.55f, 0.32f)
                        orb(Violet, size.width * 0.08f, size.height * 0.50f, size.width * 0.60f, 0.38f)
                        orb(Aqua,   size.width * 0.80f, size.height * 0.88f, size.width * 0.38f, 0.20f)
                    }
            )
            content()
        }
    }
}
```

Los orbes dan forma al fondo para que el desenfoque se note. Ajustá los `alpha` si quedan fuertes; el fondo debe seguir siendo tenue.

**`GlassSurface`**: mantené la firma actual (los llamadores usan `fillColor`, `borderColor`, `blur`, `shape`) y cambiá el cuerpo.

```kotlin
Box(
    modifier
        .clip(shape)
        .then(
            if (blur && hazeState != null)
                Modifier.hazeEffect(state = hazeState) {
                    blurRadius = 24.dp
                    noiseFactor = 0.04f
                    backgroundColor = Color.Transparent
                    tints = listOf(HazeTint(fillColor))
                }
            else Modifier.background(fillColor)
        )
        .border(borderWidth, borderColor, shape)
) { content() }
```

Cambiá el valor por defecto de `fillColor` a `GlassTint` cuando `blur = true` (el 6 % de `GlassFill` es muy flojo con blur; se conserva para `blur = false`). Si el efecto no existe en Android < 12, Haze cae al tinte: está bien, pero para ese caso subí el tinte (por ejemplo `fallbackTint` o usar `Color(0x66181030)` si `Build.VERSION.SDK_INT < 31`).

**`PrimaryButton`**: el degradado va **dentro** del vidrio, por encima del blur, no en el modifier exterior.

```kotlin
GlassSurface(
    shape = GlassButtonShape,
    fillColor = Color.Transparent,
    borderColor = Color.White.copy(alpha = borderAlpha),
    blur = true,
    modifier = modifier.heightIn(min = 54.dp).clickable(enabled = enabled, onClick = onClick)
) {
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(gradientColors))) {
        // el Row actual con icono + texto, centrado
    }
}
```

Aplicá la misma idea a cualquier otro componente que ponga `.background(...)` sobre un `GlassSurface` desde afuera.

**Prueba de que el blur funciona (temporal):** dibujá franjas de colores fuertes en el `drawBehind` del fondo. Si las tarjetas y botones las muestran difuminadas, está bien; después quitá las franjas. Probalo en un dispositivo/emulador con Android 12+ (API 31+).

### Paso 3 – Barra inferior

En `GlassBottomBar`, agregá `.navigationBarsPadding()` al `Box` exterior, **antes** del `padding(horizontal = 12.dp, vertical = 8.dp)`. Verificá que el contenido de las pantallas no quede tapado (el `innerPadding` del `Scaffold` ya incluye la altura de la barra).

### Paso 4 – Menús y diálogos (ventanas aparte)

Como Haze no llega a otras ventanas, la solución es doble: superficie casi opaca violeta + desenfoque del sistema detrás de la ventana en Android 12+.

Creá en `Components.kt`:

```kotlin
/** Desenfoque de sistema detrás de un Dialog (API 31+). En versiones viejas solo oscurece. */
@Composable
fun DialogWindowBlur(radius: Int = 32, dim: Float = 0.45f) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setDimAmount(dim)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = radius }
        }
    }
}
```

1. **`GlassAlertDialog`**: mismos parámetros que usan hoy los `AlertDialog` (`onDismissRequest`, `title`, `text`, `confirmButton`, `dismissButton`). Internamente llama a `AlertDialog` con `containerColor = PopupSurface`, `shape = RoundedCornerShape(20.dp)`, `tonalElevation = 0.dp`, y mete `DialogWindowBlur()` dentro del lambda `text`. Reemplazá **todos** los `AlertDialog(` de la lista del punto B.2 por `GlassAlertDialog(`, sin cambiar sus textos ni sus acciones.
2. **`MediaPreviewDialog.kt:94` (`Dialog(`)**: agregá `DialogWindowBlur()` dentro del contenido y que su superficie use `PopupSurface` en lugar de colores translúcidos.
3. **`GlassDropdownMenu`**: envoltorio de `DropdownMenu` con `containerColor = PopupSurface`, `shape = RoundedCornerShape(16.dp)`, `border = BorderStroke(1.dp, PopupBorder)`, `tonalElevation = 0.dp`, `shadowElevation = 8.dp`. Reemplazá todos los `DropdownMenu(` (`ExplorerScreen.kt:620, 1380`; `SmartSearchScreen.kt:840, 950`) por `GlassDropdownMenu(`. Ítem seleccionado en aqua, el resto en `TextPrimary`.

Opcional, solo si querés blur también en estos menús: dentro del contenido del menú, en un `SideEffect` con API ≥ 31, tomar `LocalView.current.rootView.layoutParams as? WindowManager.LayoutParams`, activar `FLAG_BLUR_BEHIND` con `blurBehindRadius = 24` y llamar a `updateViewLayout`. Probalo en un dispositivo real; si no funciona, dejá la superficie casi opaca, que es correcta igual.

### Paso 5 – `DropdownField` con blur real (`Components.kt:~740-820`)

El `DropdownMenu` interno se reemplaza por una **lista desplegable en línea** (dentro de la misma ventana, así Haze sí funciona):

- Estructura: `Column(modifier)` con el campo (como está hoy) y debajo `AnimatedVisibility(expanded) { GlassSurface(blur = true, fillColor = GlassTint, shape = GlassChipShape, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Column(Modifier.padding(6.dp)) { ... } } }`.
- Cada opción: fila de 44 dp mínimo, texto 14 sp, seleccionada con fondo `SelectedAqua`, borde `SelectedAquaBorder` y texto aqua; al tocar, llama a `onSelect(opt)` y cierra.
- La flecha rota (↑/↓) como ahora. Mantené la misma firma de `DropdownField` y el mismo borde aqua cuando está abierto.

### Paso 6 – `SettingsScreen.kt`

1. **Fondo:** quitá `.background(BgDark)` de la línea 94. La pantalla debe dejar ver el degradado.
2. **Márgenes:** `padding(horizontal = 16.dp, vertical = 12.dp)` y `verticalArrangement = spacedBy(12.dp)`, igual que Explorador/Buscador/Red LAN.
3. **Paneles internos** (líneas 143, 278, 328, 476): creá `GlassPanel` en `Components.kt` = `GlassSurface(shape = GlassChipShape, fillColor = Color.White.copy(alpha = 0.05f), borderColor = GlassBorder, blur = false)` con contenido de `padding(14.dp)`, y reemplazá cada `Column/Row(...).clip(ControlShape).background(InputDark).padding(...)` por `GlassPanel`. En las líneas 476 (licencias) usá padding 12 dp.
4. **Tamaños de texto:** título de pantalla 19 sp, subtítulo 13 sp; títulos de fila 14 sp SemiBold; subtítulos, valores y descripciones **mínimo 12 sp** con `lineHeight` 16 sp (cambiá todos los 10 sp y 11 sp); textos principales de paneles 13-14 sp.
5. **Almacenamiento:** «Almacenamiento Interno» 13 sp, el valor «X de Y» en aqua 12 sp bold, «libres disponibles» 12 sp. La barra de progreso: 8 dp con degradado violeta → aqua (en `AppProgressBar`, reemplazá el `LinearProgressIndicator` por un `Box` de pista `GlassFill` con otro `Box` interior de ancho proporcional y `Brush.horizontalGradient(listOf(Violet, Aqua))`, ambos con `CircleShape`).
6. **Fila «Caché de vistas previas remotas»:** `Column(Modifier.weight(1f))` + `Spacer(12.dp)` + botón con ancho según contenido; el texto puede ocupar 2 líneas. «Limpiar ahora» en `Danger`.
7. **Botones «Política de Privacidad» y «Licencias Open Source»:** pasalos a una `Column(spacedBy(8.dp))` con cada botón `fillMaxWidth()` (o `Row` solo si caben sin cortarse).
8. **`SecondaryButton`:** cambiá `height(44.dp)` por `heightIn(min = 44.dp)`, texto 14 sp, relleno horizontal 16 dp; no fuerces `maxLines = 1` cuando el botón es `fillMaxWidth`.
9. **`SettingSwitchRow`:** alto mínimo 56 dp, `padding(vertical = 8.dp, horizontal = 4.dp)`, título 14 sp, subtítulo 12 sp con `lineHeight = 16.sp`, `IconBubble` de 40 dp, y `Spacer(12.dp)` antes del switch.
10. **Íconos y acentos:** reemplazá `AccentBlueLight` / `AccentGreenLight` / `AccentPurple` por tokens reales: Almacenamiento y Acerca de → `Violet`; Explorador y Permisos concedidos → `Aqua`; Permisos pendientes → `WarningAmber`.
11. **Diálogos de la pantalla** (Privacidad y Licencias): `GlassAlertDialog` y filas de licencias con `GlassPanel`.
12. Mantené el `Spacer` final de 16 dp para que el último card no quede pegado a la barra inferior.

### Paso 7 – Revisión de otras pantallas

Buscá con "Find in Files" cualquier otro `.background(BgDark|CardDark|BgElevated|InputDark)` o `containerColor = CardDark|BgElevated|InputDark` que haya quedado fuera de la lista y aplicales la misma regla: superficies de la pantalla = `GlassSurface`; ventanas (menús, diálogos) = `PopupSurface`.

### Paso 8 (opcional) – Que el contenido pase por detrás de las barras

Para que el blur de la barra inferior se note con la lista desplazándose detrás, en `MainActivity` no apliques `innerPadding` al contenedor de pantallas: pasalo como `contentPadding` a cada `LazyColumn` / `LazyVerticalGrid`. Solo hacelo si no cambia comportamiento; si complica las pantallas, omitilo.

## 3. Criterios de aceptación

- Con las franjas de prueba, el vidrio las muestra difuminadas en tarjetas, botones, chips, campos, selector segmentado y barra inferior (Android 12+).
- Botón principal con degradado violeta → aqua visible **sobre** el blur.
- Menús, desplegables y diálogos ya no son transparentes: violeta oscuro casi opaco, con borde fino, y desenfoque del sistema detrás en Android 12+.
- Ajustes muestra el mismo fondo degradado que las demás pantallas, márgenes 16/12, ningún texto de menos de 12 sp y ningún botón con texto cortado.
- Barra inferior separada de la barra de gestos.
- Sin cambios de lógica: compila sin warnings nuevos y todas las opciones siguen funcionando.

## 4. Prompt para pegar en Gemini

> Seguí `REDISENO_UI_CORRECCIONES.md`. Es solo corrección visual: no toques ViewModel, servicios, modelos ni lógica. Hacé un paso por vez, mostrame los cambios por archivo y compilá antes de pasar al siguiente. Empezá por el paso 1 y el paso 2.
