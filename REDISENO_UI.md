# FileManager – Rediseño de UI (violeta oscuro + glass)

Instrucciones para Gemini en Android Studio. Trabajá por pasos, en orden, y compilá al terminar cada uno (`./gradlew assembleDebug`).

## 0. Regla de oro

Esto es un **rediseño visual**. No se toca la lógica.

- **NO modificar:** `MainViewModel.kt`, `VfsManager.kt`, `ConnectionStorage.kt`, `SmartSearchEngine.kt`, `FileModels.kt`, permisos, navegación, nombres de funciones/parámetros públicos ni el comportamiento de ninguna pantalla.
- **SÍ modificar:** `ui/theme/Theme.kt`, `ui/components/*`, `ui/screens/*` (solo el aspecto), `MainActivity.kt` (solo contenedor visual, fondo y barra inferior) y el `build.gradle.kts` del módulo `app` (solo para agregar la dependencia de blur y las fuentes).
- Conservá **todas** las acciones, botones y textos que ya existen en la app. Si un texto del app difiere del prototipo, gana el del app.
- Stack actual: Kotlin, Jetpack Compose, Material 3, minSdk 26. No agregues otras librerías.
- Los datos que aparecen en las capturas/prototipo (346,35 GB, DESKTOP-M, etc.) son ejemplos: usá siempre el estado real.

## 1. Estilo

Moderno, minimalista, cómodo y funcional. Un único modo oscuro. Fondo con degradado tenue, superficies de vidrio con desenfoque, violeta oscuro como base y verde agua en resaltes.

### Tokens de color (agregar a `Theme.kt`)

| Token | Valor | Uso |
|---|---|---|
| `BgTop` / `BgMid` / `BgBottom` | `#1B1238` / `#120B26` / `#0D1424` | degradado de fondo (diagonal, ~165°) |
| `GlowAqua` | `#5EEAD4` al 16 % | resplandor arriba a la derecha |
| `GlowViolet` | `#A78BFA` al 20 % | resplandor abajo a la izquierda |
| `Violet` | `#A78BFA` | acento principal, etiquetas de sección |
| `Aqua` | `#5EEAD4` | resaltes, valores, switches, progreso |
| `TextPrimary` | `#ECE8FB` | texto |
| `TextMuted` | `#9A92B8` | texto secundario |
| `GlassFill` | blanco al 6 % | relleno de vidrio |
| `GlassBorder` | blanco al 11 % | borde de vidrio (1 dp) |
| `SelectedViolet` | `#A78BFA` al 30 % + borde 45 % | ítem seleccionado (pestañas, chips, segmentos) |
| `SelectedAqua` | `#5EEAD4` al 14 % + borde 50 % | opción elegida |
| `NavBg` | `#181030` al 55 % | barra inferior |
| `Danger` | `#FF9AA8` | eliminar / limpiar |

Antes de remapear colores, buscá con "Find in Files" dónde se usan `AccentBlueLight`, `AccentGreenLight`, `BgDark`, `CardDark`, etc. Remapeá `primary = Violet`, `secondary = Aqua`, `background = BgMid`, `onBackground = TextPrimary`, y mantené los nombres existentes para que nada se rompa.

### Formas y medidas

Tarjetas 18 dp · chips 14 dp · botones 16–18 dp · burbuja de ícono 42 dp con radio 13 dp · botón principal 54 dp de alto · campo de búsqueda 48 dp · barra inferior flotante con radio 24 dp. Padding de pantalla 16 dp. Etiquetas de sección en minúsculas (sin MAYÚSCULAS ni espaciado de letras), 13 sp semibold violeta.

### Tipografía

Manrope (Regular, Medium, SemiBold, Bold) en `res/font/`, aplicada en `Typography` del tema. Título de pantalla 22 sp bold; título de tarjeta 15 sp semibold; texto de apoyo 13 sp en `TextMuted`.

### Desenfoque (glass)

Compose no trae blur de fondo nativo. Usá la librería **Haze** (`dev.chrisbanes.haze`): agregala al catálogo `libs.versions.toml` con su última versión estable y verificá la API de esa versión (`hazeSource` / `hazeEffect` en 1.x). En Android < 12 hace un fallback translúcido sin desenfoque; está bien.

Reglas:
- El fondo es la **fuente** del blur y los elementos de vidrio van **encima**, nunca dentro de la fuente.
- Compartí el estado con un `CompositionLocal` (`LocalHaze`) provisto en la raíz.
- Rendimiento: blur real solo en cabecera, barra inferior, tarjetas y botones principales. En ítems de listas largas (`LazyColumn`/`LazyVerticalGrid` de archivos) usá vidrio translúcido **sin** blur (`blur = false`).

## 2. Componentes reutilizables (en `ui/components/Components.kt`)

Creá estos composables y reemplazá con ellos los equivalentes existentes, sin cambiar sus firmas funcionales:

1. `AppBackground` – degradado + dos resplandores radiales (`drawBehind`) + `hazeSource`. Envuelve toda la app.
2. `GlassSurface(shape, blur)` – clip + `hazeEffect` + relleno `GlassFill` + borde `GlassBorder`.
3. `GlassCard`, `GlassIconButton` (48 dp), `GlassChip` (con estado seleccionado violeta).
4. `PrimaryGlassButton` – 54 dp, degradado horizontal violeta 50 % → aqua 38 %, borde blanco 22 %, texto blanco bold, estado deshabilitado al 40 %.
5. `SecondaryGlassButton` – vidrio con texto aqua.
6. `GlassSegmentedControl` – contenedor de vidrio, segmento activo `SelectedViolet`.
7. `GlassSwitchRow` – interruptor propio: pista apagada blanco 12 %, encendida aqua; pulgar blanco (apagado) u oscuro `#0D1424` (encendido).
8. `GlassSearchField` – 48 dp, ícono de lupa, sin borde grueso; foco con borde aqua.
9. `IconBubble(tint)` – 42 dp, fondo del tinte al 18 %.
10. `EmptyState(icon, title, text)` – burbuja de 64 dp aqua 14 %, título 17 sp, texto muted centrado.
11. `GlassBottomBar` – píldora flotante con padding de las barras del sistema; ítem activo con fondo `SelectedViolet`, ícono aqua y texto blanco.

Sistema: activá edge-to-edge con barras transparentes e íconos claros si `MainActivity` aún no lo hace.

## 3. Pantallas

**Cabecera común:** logo actual (40 dp, bordes redondeados) + «FileManager» 22 sp bold + «Studio Mobile» 13 sp muted; a la derecha, píldora de vidrio con punto aqua y «Acceso total». Sin línea divisoria.

**Barra inferior:** `GlassBottomBar` con las mismas 4 pestañas (Explorador, Buscador, Red LAN, Ajustes).

### ExplorerScreen
- Fila de chips de accesos rápidos (Interno, Descargas, Cámara, Videos…) con scroll horizontal; seleccionado = `SelectedViolet`.
- Ruta (Raíz / emulated / 0): segmentos tocables en muted, el actual en negrita blanca. Sin tarjeta contenedora.
- Barra de herramientas: `GlassSearchField` «Filtrar en esta carpeta» + botones de orden y de vista (lista/cuadrícula). Mantené **actualizar** y **nueva carpeta** como `GlassIconButton` (en una fila con scroll o en un menú de desbordamiento si no entran).
- Contador de carpetas en muted.
- Ítems: tarjeta de vidrio (sin blur) con `IconBubble` violeta, nombre 15 sp semibold, subtítulo muted y menú ⋮. Vista cuadrícula de 3 columnas con ícono arriba y nombre centrado.

### SmartSearchScreen (Buscador)
- `GlassSegmentedControl`: «Búsqueda inteligente» / «Limpiar duplicados».
- `GlassSearchField` «Buscar por nombre».
- Los cuatro botones de filtro actuales (lista, formas, rotar, filtros) pasan a `GlassIconButton` o `GlassChip` en una fila con scroll, conservando su función.
- `PrimaryGlassButton` «Escanear carpeta».
- Sin resultados: `EmptyState`. Con resultados: lista de tarjetas de vidrio sin blur.

### NetworkScreen (Red LAN)
- Tarjeta de vidrio «Red local SMB y FTP» con descripción y dos botones: `PrimaryGlassButton` «Añadir servidor» y `SecondaryGlassButton` «Escanear Wi-Fi» (aqua).
- Etiqueta «Servidores guardados (n)» en violeta.
- Tarjeta por servidor: `IconBubble`, nombre 16 sp, dirección `smb://…` en aqua, «Usuario: …» muted, botones de editar y eliminar (eliminar en `Danger`), y debajo `PrimaryGlassButton` «Conectar y explorar».
- Diálogos de alta/edición: superficie de vidrio, campos con el estilo de `GlassSearchField`.

### SettingsScreen (Ajustes)
- Encabezado con `IconBubble` + «Ajustes» 19 sp y subtítulo muted.
- Tarjeta «Almacenamiento y caché»: panel interior de vidrio con «Interno» y «346,35 de 454,16 GB» en aqua, barra de 8 dp con degradado violeta → aqua, texto de libres en muted; fila «Vistas previas remotas» con botón de vidrio «Limpiar» en `Danger`; `GlassSwitchRow` «Limpiar caché remota al salir».
- Tarjeta «Explorador y visualización» con `GlassSwitchRow` por opción (por ejemplo «Mostrar archivos ocultos»).
- Cualquier otra opción que ya exista se restyliza igual, sin cambiar su comportamiento.

### Diálogos y vistas previas
`MediaPreviewDialog` y el resto de diálogos/menús usan superficie de vidrio sobre scrim oscuro, bordes de 18 dp y los mismos botones.

## 4. Orden de trabajo

1. Commit o rama nueva antes de empezar (`git checkout -b rediseno-ui`) o copia de la carpeta.
2. Dependencias: Haze + fuente Manrope. Sincronizar Gradle.
3. `Theme.kt`: tokens, `colorScheme`, tipografía.
4. `Components.kt`: los 11 componentes, cada uno con `@Preview`.
5. `MainActivity.kt`: `AppBackground`, cabecera y `GlassBottomBar`.
6. Pantallas, una por vez: Explorer → SmartSearch → Network → Settings → diálogos.
7. Revisión final.

### Prompt inicial para pegar en Gemini

> Este es un proyecto Android con Kotlin, Jetpack Compose y Material 3. Vamos a hacer solo un rediseño visual descripto en `REDISENO_UI.md`. No modifiques ViewModel, servicios, modelos, navegación ni lógica. Trabajá un paso por vez, mostrame los cambios por archivo y no avances al siguiente paso hasta que compile. Empezá por el paso 2.

## 5. Criterios de aceptación

- Compila sin warnings nuevos y todas las funciones siguen igual.
- Mismo fondo degradado y mismos componentes en todas las pantallas.
- Contraste legible (texto principal sobre vidrio ≥ 4.5:1); zonas táctiles ≥ 48 dp.
- Sin caídas de fluidez al desplazar listas largas (blur desactivado en ítems).
- Probado en un dispositivo/emulador API 26–30 (sin blur real) y en API 31+.
- Respeta las barras del sistema (edge-to-edge) y el teclado.
