Actúa como un desarrollador Senior de Android experto en Kotlin y Jetpack Compose. Necesito simplificar y rediseñar la pestaña de Buscador (`ui/screens/SmartSearchScreen.kt`) y su ViewModel (`viewmodel/MainViewModel.kt`) en la aplicación `com.maez.filemanager` (FileManager Studio).

REGLAS DE DISEÑO:
- No utilices emojis en textos, títulos ni botones. Utiliza exclusivamente iconos vectoriales de Google Material (`androidx.compose.material.icons.Icons`).
- Mantén la paleta GitHub Dark definida en `ui/theme/Theme.kt` (`BgDark`, `CardDark`, `BgElevated`, `InputDark`, `BorderDark`, `TextPrimary`, `TextSecondary`, `AccentGreenLight`, `AccentBlueLight`, `AccentPurple`, `DangerRedLight`, `WarningAmber`).
- Reutiliza los componentes de `ui/components/Components.kt` (`SegmentedTabs`, `SelectChip`, `CompactSearchField`, `PrimaryButton`, `SecondaryButton`, `AppProgressBar`, `EmptyState`, `FileThumbnailBox`, `Tag`).

OBJETIVO DEL REDISEÑO:
Reducir la complejidad visual de 3 sub-herramientas a solo 2 modos de trabajo claros y directos:
1. "Búsqueda Inteligente" (Unifica la búsqueda por texto, categorías, orientación y el filtro de cámaras/dispositivos en chips horizontales compactos estilo Google Drive).
2. "Limpiador de Duplicados" (Enfocado a liberar espacio con accesos rápidos a carpetas frecuentes y comparación avanzada opcional).

APLICA LOS SIGUIENTES CAMBIOS:

### 1. Actualización en `viewmodel/MainViewModel.kt`
- Cambia `searchToolIndex` para manejar 2 modos:
  - `0`: Búsqueda Inteligente
  - `1`: Limpiador de Duplicados
- Añade atajos rápidos para establecer la carpeta de análisis de duplicados en un toque:
  - `setDuplicateTargetShortcut(folderType: String)`: donde `folderType` puede ser `"DOWNLOADS"`, `"CAMERA"`, `"INTERNAL"` o `"CUSTOM"`.
- Añade una función auxiliar para marcar automáticamente los duplicados sobrantes dejando el original intacto:
  - `fun selectDuplicateCopiesOnly()`: que marque en un conjunto de selección todas las entradas donde `isPrimary == false`, permitiendo borrarlas en bloque de forma segura.
- Conserva el motor de extracción EXIF y agrupación de dispositivos de `SmartSearchEngine.kt`, pero expón la lista de modelos de cámara encontrados como una lista de opciones para un filtro desplegable de dispositivo dentro de la búsqueda general.

### 2. Rediseño de `ui/screens/SmartSearchScreen.kt`

#### A. Cabecera y Selector de Modo
- Reemplaza el selector de 3 pestañas por un `SegmentedTabs` de 2 opciones:
  - "Búsqueda Inteligente" con icono `Icons.Default.Search`
  - "Limpiar Duplicados" con icono `Icons.Default.ContentCopy`

#### B. Modo 1: "Búsqueda Inteligente"
- Elimina las tarjetas verticales apiladas que ocupaban toda la pantalla antes de ver los resultados.
- Diseña una barra superior limpia y compacta:
  1. `CompactSearchField` con icono `Icons.Default.Search` para escribir nombre, extensión o palabra clave.
  2. Debajo del campo, una fila horizontal deslizable (`Row` con `horizontalScroll`) con chips compactos desplegables:
     - Chip Ubicación: muestra el nombre de la carpeta analizada con icono `Icons.Default.Folder`. Al pulsar, permite alternar entre almacenamiento local o la carpeta de red actual.
     - Chip Tipo: icono `Icons.Default.Category` ("Todos", "Imágenes", "Videos", "Audios", "Documentos", "APKs").
     - Chip Orientación: icono `Icons.Default.ScreenRotation` ("Todas", "Vertical", "Horizontal").
     - Chip Cámara / Dispositivo: icono `Icons.Default.CameraAlt`. Muestra la lista de modelos detectados en el escaneo para filtrar sin necesidad de una pestaña aparte.
     - Chip Tamaño: icono `Icons.Default.FilterList` ("Cualquiera", "Mayor a 10 MB", "Mayor a 50 MB", "Mayor a 100 MB").
- Botón de acción rápido:
  - Fila compacta con botón "Escanear Carpeta" (`PrimaryButton` con `Icons.Default.ManageSearch`) y botón "Exportar CSV" (`SecondaryButton` con `Icons.Default.Description`) si hay resultados.
  - Si el escaneo está en curso, muestra `AppProgressBar` y botón "Detener" (`Icons.Default.Stop`).
- Lista de resultados:
  - Ocupa todo el alto restante de la pantalla usando `LazyColumn`.
  - Cada elemento usa una versión optimizada de `MediaSearchResultCard` con miniatura, nombre, tamaño, resolución y badges de orientación y cámara.

#### C. Modo 2: "Limpiador de Duplicados"
- Diseña una pantalla orientada a la acción directa de limpieza de almacenamiento:
  1. **Tarjeta de Selección Rápida**:
     - Título: "Ubicación a Analizar" con icono `Icons.Default.FolderOpen`.
     - Chips de acceso directo de un toque:
       - "Descargas" con icono `Icons.Default.Download`
       - "Fotos y Cámara" con icono `Icons.Default.CameraAlt`
       - "Todo el Almacenamiento" con icono `Icons.Default.PhoneAndroid`
     - Interruptor compacto (`SettingSwitchRow`): "Comparar con una segunda carpeta (A vs B)". Solo si se activa, muestra la fila para asignar y seleccionar la "Carpeta B" (útil para sincronizar PC/NAS contra el móvil).
  2. **Botón Principal de Análisis**:
     - `PrimaryButton` con icono `Icons.Default.FindReplace` ("Buscar Archivos Repetidos").
  3. **Barra de Acciones de Duplicados (visible cuando hay resultados)**:
     - Resumen del espacio desperdiciado: "X duplicados encontrados (Y MB ocupados)".
     - Botón `SecondaryButton` "Marcar copias sobrantes" con icono `Icons.Default.Checklist` (selecciona automáticamente las copias que no son la original).
     - Botón `PrimaryButton` con colores de peligro (`DangerRed`) "Eliminar seleccionados" con icono `Icons.Default.Delete`.
  4. **Lista de duplicados agrupados**:
     - Visualización limpia con `DuplicateFileCard` donde el archivo original tiene un borde verde tenue (`AccentGreenLight`) con etiqueta "Original", y las réplicas tienen borde ámbar (`WarningAmber`) con etiqueta "Copia".

### 3. Diálogos de Seguridad
- Cada vez que se pulse "Eliminar" en el limpiador de duplicados o en la búsqueda, muestra un `AlertDialog` de confirmación claro:
  - Título: "Confirmar eliminación" con icono `Icons.Default.Warning`.
  - Mensaje: "¿Deseas eliminar permanentemente los archivos seleccionados? Esta acción liberará espacio pero no se puede deshacer."
  - Botones: "Eliminar" (en color `DangerRedLight`) y "Cancelar".

Asegúrate de que el código compile sin errores, importe correctamente todos los iconos de `androidx.compose.material.icons` y no altere las firmas existentes que utiliza `MainActivity.kt`.