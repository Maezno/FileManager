# Manual de Identidad Visual, Estética y Sistema de Animaciones (UI/UX)
> **FileManager Studio Mobile Design System — Estilo Charcoal / GitHub Dark Sólido**  
> Guía técnica completa de diseño, tokens, componentes, interacciones y especificaciones de animación para ser replicado en otra aplicación (Jetpack Compose, Flutter, React Native, Web, etc.).

---

## 1. Filosofía y Dirección de Arte

El lenguaje visual de la aplicación se define como **Dark Charcoal / GitHub Dark Sólido de Alto Contraste**:
- **Superficies 100% Sólidas (Sin Glassmorfismo):** Se eliminan por completo los efectos de desenfoque translúcido (*blur / frosted glass*), transparencias complejas, sombras difusas y orbes resplandecientes. Toda la interfaz se asienta sobre superficies oscuras opacas, limpias y de alto rendimiento.
- **Definición por Bordes Nítidos (1 dp Stroke):** La profundidad y separación entre capas no se logra mediante sombras ni refracción, sino mediante una jerarquía tonal sutil en los fondos acompañada de bordes nítidos de `1 dp` (`#262C38`).
- **Semántica Limpia sin Emojis:** En interfaces, etiquetas, diálogos y botones solo se utilizan **iconos vectoriales Google Material** limpios y precisos, evitando por completo el uso de emojis.
- **Micro-interacciones Orgánicas:** Animaciones ágiles basadas en físicas de resorte (*Spring physics*) para elementos interactivos como la barra de navegación, asegurando una respuesta táctil inmediata sin lag gráfico.

---

## 2. Paleta Cromática y Tokens de Color

### 2.1 Colores de Fondo y Superficies Sólidas

| Token | Hex / Valor | Tipo | Descripción / Caso de uso |
|---|---|---|---|
| `BgTop` | `#0F1117` | Sólido | Fondo degradado vertical (extremo superior) |
| `BgMid` / `BgDark` | `#0D0F14` | Sólido | Fondo degradado vertical (centro) / fondo global |
| `BgBottom` | `#090A0D` | Sólido | Fondo degradado vertical (extremo inferior) |
| `SurfaceDark` / `CardDark` | `#161922` | Sólido | Superficie principal de tarjetas, secciones y listados |
| `InputDark` | `#11141A` | Sólido | Fondo oscuro para inputs y campos de búsqueda |
| `BgElevated` | `#1E232F` | Sólido | Fondo de botones secundarios y paneles interiores |
| `BorderDark` | `#262C38` | Sólido | Borde sutil y definido de `1 dp` para tarjetas y contenedores |
| `BorderElevated` | `#2E384D` | Sólido | Borde reforzado de `1 dp` para botones y controles interactivos |
| `NavBg` | `#12151D` | Sólido (95%) | Fondo de la barra de navegación inferior flotante |
| `PopupSurface` | `#161922` | Sólido | Fondo de diálogos modales, menús flotantes y popups |
| `PopupBorder` | `#2E3545` | Sólido | Borde definido de `1 dp` para ventanas emergentes |

### 2.2 Colores de Acento y Semántica

| Token | Hex | Descripción / Uso |
|---|---|---|
| `Violet` | `#818CF8` | Indigo sobrio. Acento primario, etiquetas de sección, iconos destacados |
| `Aqua` | `#38BDF8` | Sky Blue sobrio. Acento secundario, cursor, switches activos, estados OK |
| `Danger` | `#F87171` | Rojo suave no estridente para advertencias, errores y borrado |
| `WarningAmber` | `#FBBF24` | Ámbar para avisos, permisos pendientes o conflictos |
| `PrimaryButtonBg` | `#4F46E5` | Fondo del botón principal de acción |
| `PrimaryButtonBorder` | `#6366F1` (50% op.) | Borde del botón principal para resaltar sobre el fondo oscuro |

### 2.3 Estados de Selección (Píldoras, Chips y Tabs)

| Token | Fondo Hex | Borde Hex | Uso |
|---|---|---|---|
| `SelectedViolet` | `#282D3D` | `#4F46E5` | Chips de filtro y selecciones principales activas |
| `SelectedAqua` | `#162B3D` | `#0284C7` | Opciones secundarias o ítems seleccionados en dropdowns |
| `NavSelectedPill` | `#232938` | `#4F46E5` | Píldora indicadora deslizante en la barra de navegación |

### 2.4 Tipografía y Textos

| Token | Hex | Uso |
|---|---|---|
| `TextPrimary` | `#F3F4F6` | Blanco nítido de alto contraste para títulos principales y datos |
| `TextMuted` / `TextSecondary` | `#9CA3AF` | Gris neutro para subtítulos, metadatos, detalles y placeholders |

---

## 3. Tipografía y Jerarquía Textual

Se recomienda el uso de tipografías geométricas o neo-grotescas limpias (ej. *Manrope*, *Inter*, *Roboto* o tipografía de sistema):

| Nivel Tipográfico | Tamaño | Peso (`FontWeight`) | Color | Casos de uso |
|---|---|---|---|---|
| **App Title / Cabecera** | `22.sp` | `Bold` (700) | `TextPrimary` | Título del encabezado de la app |
| **Título de Tarjeta / Modal** | `15.sp` - `16.sp` | `SemiBold` (600) | `TextPrimary` | Título en tarjetas de sección y alertas |
| **Etiqueta de Sección** | `13.sp` | `SemiBold` (600) | `Violet` | Subtítulos de sección en minúsculas |
| **Cuerpo / Descripción** | `13.sp` | `Normal` (400) | `TextMuted` | Subtítulos de tarjeta, rutas y metadatos |
| **Botón Primario** | `13.5.sp` | `SemiBold` (600) | `#FFFFFF` | Texto de botón principal |
| **Botón Secundario / Chips** | `13.sp` | `SemiBold` / `Medium` | `Aqua` / `TextPrimary` | Botones secundarios y chips de filtros |
| **Texto Pestaña Activa** | `11.5.sp` | `Bold` (700) | `#FFFFFF` | Etiqueta en barra de navegación inferior |
| **Tags / Badges** | `11.sp` | `SemiBold` (600) | Color del acento | Badges compactos de formato, cámara o estado |

---

## 4. Radios de Borde (`Shapes`)

La jerarquía visual define esquinas redondeadas pero contenidas, alejadas del estilo burbuja:

| Componente | Radio (`dp`) | Implementación |
|---|---|---|
| **Barra de Navegación Flotante** | `20.dp` | `RoundedCornerShape(20.dp)` |
| **Diálogos / Modales** | `20.dp` | `RoundedCornerShape(20.dp)` |
| **Tarjetas Principales (`SectionCard`)** | `16.dp` | `RoundedCornerShape(16.dp)` |
| **Botones de Acción (`Primary/Secondary`)** | `14.dp` | `RoundedCornerShape(14.dp)` |
| **Burbuja de Icono (`IconBubble`)** | `13.dp` | `RoundedCornerShape(13.dp)` |
| **Chips / Controles Segmentados / Inputs** | `12.dp` | `RoundedCornerShape(12.dp)` |
| **Tags / Micro-etiquetas** | `8.dp` | `RoundedCornerShape(8.dp)` |
| **Píldoras circulares / Badges de estado** | `50%` | `CircleShape` |

---

## 5. Especificaciones de Componentes Clave

### 5.1 Barra de Navegación Inferior Flotante (`AppBottomNavigationBar`)
- **Dimensiones:** Altura fija de `64.dp`, padding lateral de `12.dp`, separación inferior de `8.dp` + padding de barra del sistema (`navigationBarsPadding`).
- **Contenedor Sólido:** 
  - Fondo sólido `#12151D` (sin blur).
  - Borde de `1.dp` en `#262C38`.
  - Radio exterior de `20.dp`.
- **Mecánica de Pestañas Proporcionales:**
  - Las pestañas inactivas tienen una proporción de ancho base de `1.0f`.
  - La pestaña activa seleccionada recibe un factor de `1.45f` de ancho, permitiendo mostrar el icono junto a su texto de forma cómoda.
- **Píldora Indicadora Deslizante:**
  - Fondo sólido `#232938`, borde de `1.dp` en `#4F46E5`, radio de `16.dp`.
  - Ocupa la altura completa interior menos `4.dp` de margen vertical.

### 5.2 Botones

#### Botón Primario (`PrimaryButton`)
- **Altura:** Mínima `44.dp` (hasta `54.dp` en acciones clave de pantalla completa).
- **Fondo:** Sólido `#4F46E5` (en estado inactivo: `#1F2430`).
- **Borde:** `1.dp` con `#6366F1` al 50% de opacidad (inactivo: `#2A303C`).
- **Radio:** `14.dp`.
- **Icono:** `18.dp`, color blanco, separación de `8.dp` al texto.

#### Botón Secundario (`SecondaryButton`)
- **Altura:** Mínima `44.dp`.
- **Fondo:** Sólido `#1E2330`.
- **Borde:** `1.dp` con `#2E384D`.
- **Radio:** `14.dp`.
- **Icono y Texto:** Color `Aqua` (`#38BDF8`), icono de `17.dp`.

#### Botón Cuadrado de Icono (`GlassIconButton` / `AppIconButton`)
- **Dimensiones:** `48.dp x 48.dp`.
- **Fondo:** Sólido `#161922`, borde `1.dp` `#262C38`.
- **Radio:** `14.dp`.
- **Icono:** `20.dp` en color `TextPrimary`.

### 5.3 Selector Segmentado (`SegmentedTabs`)
- **Contenedor:** Fondo sólido `#161922`, borde `1.dp` `#262C38`, radio `16.dp`, padding interno de `4.dp`.
- **Ítem de opción:** Altura `38.dp`, radio `12.dp`.
- **Opción Inactiva:** Fondo transparente, icono y texto en `TextMuted` (`#9CA3AF`).
- **Opción Activa:** Fondo sólido `#232938`, borde `1.dp` `#4F46E5`, icono `Aqua` (`#38BDF8`), texto `TextPrimary` (`#F3F4F6`).

### 5.4 Campo de Búsqueda (`CompactSearchField`)
- **Dimensiones:** Altura de `48.dp`.
- **Fondo:** Sólido `#161922`.
- **Borde:** `1.dp` `#262C38` en reposo; se anima suavemente a `Aqua` (`#38BDF8`) al obtener foco.
- **Contenido:** Icono `Search` de `18.dp`, cursor `Aqua`, y botón de limpieza circular de `22.dp` en el extremo derecho cuando hay texto escrito.

### 5.5 Burbuja de Icono (`IconBubble`)
- **Dimensiones:** `42.dp x 42.dp` en tarjetas estándar (se amplía a `64.dp` en pantallas de estado vacío `EmptyState`).
- **Fondo:** Tinte correspondiente al 18% de opacidad sobre fondo oscuro (`tint.copy(alpha = 0.18f)`).
- **Radio:** `13.dp`.
- **Icono:** Tamaño equivalente al 55% de la burbuja (aprox. `23.dp`).

---

## 6. Sistema de Animaciones y Físicas

### 6.1 Animación de Píldora de Navegación (Physics-based Spring)
La píldora no aparece ni desaparece estáticamente; **se desliza continuamente** entre las pestañas mediante físicas de resorte real:

```kotlin
// Cálculo dinámico del desplazamiento horizontal según el índice activo
fun getTabOffset(index: Int): Dp {
    return if (index <= selectedTab) {
        baseInactiveWidth * index
    } else {
        baseInactiveWidth * (index - 1) + activeWidth
    }
}

val indicatorOffset by animateDpAsState(
    targetValue = getTabOffset(selectedTab),
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy, // 0.75f - Leve rebote natural orgánico
        stiffness = Spring.StiffnessMediumLow       // 400f - Respuesta inmediata sin retraso
    ),
    label = "indicatorOffset"
)
```

- **Aparición de la etiqueta de texto:** Al activarse la pestaña, el texto se revela mediante:
  `enter = fadeIn() + expandHorizontally()`  
  `exit = fadeOut() + shrinkHorizontally()`
- **Transición de color de iconos:** Cambio suave de `TextMuted` a `Aqua` mediante `animateColorAsState()`.

### 6.2 Transición entre Pantallas Principales (Lateral Carousel Slide)
El cambio entre pantallas principales en el contenedor `AnimatedContent` acompaña la dirección del movimiento del usuario:

- **Hacia la derecha (`targetTab > initialTab`):**
  - Entrante: Desplazamiento desde la derecha un 15% del ancho de pantalla + fade-in:  
    `slideInHorizontally(tween(220)) { (it * 0.15f).toInt() } + fadeIn(tween(220))`
  - Saliente: Desplazamiento hacia la izquierda un -15% del ancho + fade-out:  
    `slideOutHorizontally(tween(180)) { (-it * 0.15f).toInt() } + fadeOut(tween(180))`
- **Hacia la izquierda (`targetTab < initialTab`):**
  - Invierte las direcciones manteniendo las duraciones (`220ms` entrada, `180ms` salida).

### 6.3 Modales y Diálogos Emergentes
- **Entrada:** `fadeIn(tween(200))` combinado con escala desde `0.92f`:
  `scaleIn(tween(200, delayMillis = 50), initialScale = 0.92f)`
- **Salida:** `fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f)`.

### 6.4 Barra de Progreso (`AppProgressBar`)
- **Dimensiones:** Altura de `8.dp`, terminaciones completamente redondeadas (`CircleShape`).
- **Gradiente:** Relleno horizontal interpolado desde `Violet` (`#818CF8`) hasta `Aqua` (`#38BDF8`).
- **Animación:** Variación fluida del ancho mediante `animateFloatAsState((progress / 100f).coerceIn(0f, 1f))`.

### 6.5 Desplegables y Paneles que se Expanden
Aplica a menús desplegables, registros plegables y cualquier panel con `AnimatedVisibility`.

- **Apertura:** Puede rebotar, porque el resorte se pasa del tamaño final y vuelve:  
  `expandVertically(spring(DampingRatioMediumBouncy, StiffnessMediumLow), expandFrom = Alignment.Top) + fadeIn(tween(180))`
- **Cierre:** Nunca usa resorte con rebote: el tamaño final es cero y no hay adónde pasarse, así que la animación se rompe. Tampoco usa una curva que frene mucho al final (cola lenta) ni una que solo acelere (parada en seco):  
  `shrinkVertically(tween(220, easing = CubicBezierEasing(0.4f, 0f, 0.7f, 1f)), shrinkTowards = Alignment.Top) + fadeOut(tween(220))`
- **Misma duración:** El encogimiento y el fundido del cierre duran lo mismo, para que no quede un panel vacío moviéndose al final.
- **Separación dentro del panel:** Prohibido separar el panel de su botón con `spacedBy` en el contenedor. Ese espacio queda fuera de la animación y desaparece de golpe al terminar de cerrar, lo que se ve como un "mini corte". La separación va como `padding(top = ...)` dentro del contenido animado:

```kotlin
Column(modifier = modifier) { // Sin spacedBy
    Boton()
    AnimatedVisibility(visible = expanded, enter = panelEnter, exit = panelExit) {
        Panel(Modifier.padding(top = 6.dp).fillMaxWidth())
    }
}
```

- **Verificación:** Grabar la pantalla y revisar cuadro por cuadro que el contenido de abajo llegue a su lugar sin saltos en el último cuadro.
- **Encogimiento horizontal:** Las mismas reglas valen para `shrinkHorizontally`, por ejemplo en las etiquetas de la navegación (sección 6.1).

---

## 7. Directrices de Rendimiento y Arquitectura

1. **Sin Pasos de Desenfoque GPU:** Al prescindir del glassmorfismo, se elimina por completo el uso de librerías de blur (`haze`, `RenderEffect` en Android 12+), garantizando 120 FPS estables sin sobrecalentamiento ni caídas de cuadros durante desplazamientos rápidos.
2. **Contraste Seguro en Fondos Oscuros:** Todos los textos `TextPrimary` (`#F3F4F6`) sobre superficies `#161922` superan holgadamente la relación de contraste WCAG AAA (> 7:1).
3. **Reutilización:** Todos los bordes mantienen el grosor estándar de `1 dp` para preservar la coherencia geométrica en toda la app.

### Progreso en paneles

El flotante de copiar/mover se expande para mostrar el progreso y se contrae al terminar. Usa `Motion.progressEnter` / `Motion.progressExit` (`softBounce`): es la única excepción a la regla de no rebotar al encogerse.
