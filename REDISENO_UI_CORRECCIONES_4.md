# FileManager – Rendimiento y opción para desactivar desenfoques (parte 4)

Instrucciones para Gemini en Android Studio. Complementa `REDISENO_UI.md`, `REDISENO_UI_CORRECCIONES.md` y `REDISENO_UI_CORRECCIONES_3.md`. Si algo de la parte 3 todavía no está aplicado (botones con `blur = true`, respaldo para Android < 12), aplicalo primero y integralo con lo de acá.

**Excepción a la regla "solo UI":** esta vez se permite tocar, **únicamente** para guardar y leer el interruptor, `ConnectionStorage.kt` (2 funciones nuevas) y `MainViewModel.kt` (1 flujo y 1 setter nuevos). Nada más de esos dos archivos. Compilá (`./gradlew assembleDebug`) al terminar cada paso.

## 1. Diagnóstico (revisión de solo lectura del código; no se midió en dispositivo)

Cada `GlassSurface` con `blur = true` aplica un `hazeEffect` propio: un pase de desenfoque (y de ruido, `noiseFactor = 0.04f`) sobre la capa de fondo. El costo crece con la cantidad de superficies y con su tamaño, y se paga **en cada frame mientras algo se mueve o anima**. Lo que hoy hay en pantalla:

- **Dentro de listas con scroll:** `SavedConnectionCard` (`NetworkScreen.kt:446`) y `DiscoveredHostCard` (`NetworkScreen.kt:505`) son ítems de `LazyColumn` con blur. Es el caso más caro: varios efectos por pantalla, moviéndose.
- **Muchas superficies chicas:** `SelectChip` (`Components.kt:~527`, usado en Explorador y Red LAN) y `renderNetworkChip` (`ExplorerScreen.kt:410, 486, 551`) llevan blur cada una. Un chip chico gana casi nada visualmente con el desenfoque.
- **Blur dentro de blur:** `DropdownField` y los controles internos de Ajustes están dentro de `SectionCard`, que ya tiene blur.
- **Siempre activas y de pantalla ancha:** barra inferior (`GlassBottomBar`) y cabecera (`MainActivity.kt:342`).
- **Parámetros pesados:** `blurRadius = 24.dp` y `noiseFactor = 0.04f` en todas las superficies.
- **Los ítems de archivos ya están bien:** `ExplorerListRow`, `ExplorerGridCard`, `MediaSearchResultCard` y `DuplicateFileCard` usan `blur = false`. No los toques.

Además, una build **debug** de Compose es notablemente más lenta que una release. Parte de la lentitud percibida puede venir de ahí (ver paso 6).

## 2. Lo que se va a hacer

1. Agregar en Ajustes un interruptor **«Efectos de desenfoque»** que lo apaga en toda la app.
2. Reducir el costo del blur aunque esté activado (menos superficies, menos radio).
3. Dejar un método simple para medir si el blur era la causa.

## 3. Pasos

### Paso 1 – Guardar la preferencia (`ConnectionStorage.kt`)

Agregá dos funciones siguiendo **exactamente** el mismo patrón que `getAutoClearRemoteCache()` / `setAutoClearRemoteCache()`:

```kotlin
fun getBlurEnabled(): Boolean =
    prefs.getBoolean("blur_enabled", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

fun setBlurEnabled(value: Boolean) {
    prefs.edit().putBoolean("blur_enabled", value).apply()
}
```

Valor por defecto: activado solo en Android 12+ (en versiones anteriores no hay desenfoque real). Importá `android.os.Build` si hace falta.

### Paso 2 – Exponerla en el ViewModel (`MainViewModel.kt`)

Junto a `_autoClearRemoteCache` (~línea 97), con el mismo patrón:

```kotlin
private val _blurEnabled = MutableStateFlow(connectionStorage.getBlurEnabled())
val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

fun setBlurEnabled(value: Boolean) {
    _blurEnabled.value = value
    connectionStorage.setBlurEnabled(value)
}
```

Se lee de forma sincrónica en el constructor para que el primer frame ya use el valor guardado.

### Paso 3 – Un solo punto de control en la UI (`Components.kt` y `MainActivity.kt`)

En `Components.kt`, junto a `LocalHazeState`:

```kotlin
val LocalBlurEnabled = staticCompositionLocalOf { true }
```

En `MainActivity.kt`, donde se llama a `AppBackground(...)` (~línea 302), envolvelo:

```kotlin
val blurEnabled by viewModel.blurEnabled.collectAsState()
CompositionLocalProvider(LocalBlurEnabled provides blurEnabled) {
    AppBackground(modifier = Modifier) { /* contenido actual, sin cambios */ }
}
```

Así **ninguna** pantalla ni componente tiene que cambiar su firma: todos leen el valor desde `GlassSurface`.

### Paso 4 – Que `GlassSurface`, `AppBackground` y `DialogWindowBlur` respeten el interruptor (`Components.kt`)

**`GlassSurface`:** calculá una sola vez:

```kotlin
val blurOn = blur &&
    LocalBlurEnabled.current &&
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
    hazeState != null
val effectiveFill =
    if (blur && !blurOn) fillColor.copy(alpha = maxOf(fillColor.alpha, 0.22f)) else fillColor
```

- Si `blurOn` → `hazeEffect(...)` como está.
- Si no → `Modifier.background(effectiveFill)`.

El tinte mínimo de 0.22 evita que, sin blur, el vidrio se vea transparente. Si la parte 3 ya agregó un cálculo parecido (`canBlur`), **reemplazalo por este** para no tener dos.

**`AppBackground`:** cuando `LocalBlurEnabled.current` sea `false` (o la API sea < 31), **no apliques** `.hazeSource(...)`. Dejá igual el degradado y los orbes. Sin fuente registrada Haze no captura nada, y es menos trabajo por frame.

**`DialogWindowBlur`:** si `LocalBlurEnabled.current` es `false`, aplicá solo el `setDimAmount(dim)`, sin `FLAG_BLUR_BEHIND`. Si venía activo, llamá `window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)`.

### Paso 5 – El interruptor en Ajustes (`SettingsScreen.kt`)

Agregá un `SectionCard` nuevo **«Apariencia y rendimiento»** (arriba de todo o después de Almacenamiento) con una fila `SettingSwitchRow` ya existente:

- Título: **«Efectos de desenfoque»**.
- Subtítulo: «Desactivalo si la app va lenta o querés ahorrar batería.»
- Estado: `viewModel.blurEnabled.collectAsState()`; al cambiar, `viewModel.setBlurEnabled(it)`.
- En Android < 12 (`SDK_INT < 31`): switch **deshabilitado y apagado**, con el subtítulo «Requiere Android 12 o superior».
- Ícono: `Icons.Default.BlurOn` si está disponible (`material-icons-extended`); si no, `Icons.Default.Tune`. Color `Aqua`.
- Mismos tamaños que el resto de Ajustes: título 14 sp, subtítulo 12 sp con `lineHeight = 16.sp`, alto mínimo 56 dp, márgenes 16/12.

El cambio debe verse **al instante** en toda la app, sin reiniciar.

### Paso 6 – Bajar el costo aunque el blur esté activado

1. **Ítems de listas sin blur.** `SavedConnectionCard` (`NetworkScreen.kt:446`) y `DiscoveredHostCard` (`NetworkScreen.kt:505`): `blur = false` y `fillColor = Color.White.copy(alpha = 0.09f)`, igual que ya se hizo con los ítems del Explorador.
2. **Chips sin blur.** `SelectChip` (`Components.kt:~527`) y los tres `renderNetworkChip` (`ExplorerScreen.kt:410, 486, 551`): `blur = false`, conservando sus colores de seleccionado y borde.
3. **No apilar blur.** Dentro de un `SectionCard`, los controles internos (`DropdownField`, `SegmentedTabs`, `GlassSearchField`) pasan a `blur = false` con `fillColor = Color.White.copy(alpha = 0.10f)`. Solo la tarjeta exterior desenfoca. Si la lista desplegable en línea de `DropdownField` queda sobre el fondo de la pantalla (no dentro de una tarjeta), conserva blur.
4. **Menos radio y sin ruido.** En `GlassSurface`: `blurRadius = 16.dp` y `noiseFactor = 0f`. Compará visualmente; si se ve demasiado «plano», probá `0.02f`.
5. **Dónde queda el blur:** barra inferior, cabecera, campos de búsqueda, `SectionCard` de Ajustes, botones (parte 3) y el desplegable en línea. Todo lo demás, sin blur. Regla práctica: no más de ~8 superficies con blur visibles a la vez.

### Paso 7 – Cómo comprobar si el blur era el problema

1. Probá primero con el interruptor **apagado**: si la fluidez vuelve, el blur era la causa y el paso 6 la reduce cuando está encendido.
2. Probá en una **build release** (variante `release` en Android Studio, con la firma de debug si hace falta) y sin el debugger conectado. Compose en debug es mucho más lento y puede dar una falsa impresión.
3. Si con el blur apagado y en release **sigue lenta**, la causa no es el blur. Revisá entonces `FileThumbnails.kt` y las listas del Explorador (miniaturas, recomposiciones) y avisá.

## 4. Criterios de aceptación

- Existe «Efectos de desenfoque» en Ajustes; el cambio se aplica al instante en barra, tarjetas, botones, campos y diálogos, y **se conserva al cerrar y abrir la app**.
- Con el interruptor apagado no hay ningún `hazeEffect` activo, y todo el vidrio se ve tintado (no transparente).
- En Android < 12 el interruptor aparece deshabilitado con su explicación y la UI usa el vidrio tintado.
- Ningún ítem de `LazyColumn` ni `LazyVerticalGrid` usa blur.
- No hay blur dentro de otra superficie con blur.
- Sin cambios de lógica: solo se agregaron las dos funciones de `ConnectionStorage` y el flujo y setter del ViewModel. Compila sin warnings nuevos.

## 5. Prompt para pegar en Gemini

> Seguí `REDISENO_UI_CORRECCIONES_4.md`. Está permitido tocar `ConnectionStorage.kt` y `MainViewModel.kt` solo para el interruptor de desenfoques; todo lo demás es corrección visual, sin tocar lógica. Hacé un paso por vez, mostrame los cambios por archivo y compilá antes de pasar al siguiente. Empezá por los pasos 1 a 4.
