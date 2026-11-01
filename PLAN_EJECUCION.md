# FileManager – Plan de ejecución

Plan para Gemini en Android Studio. Está organizado en **hitos pequeños y ordenados**; cada uno se puede hacer en una sola sesión, compilar y revisar por separado.

## Reglas para Gemini (valen para todos los hitos)

1. **Un hito por vez.** No adelantes trabajo de hitos siguientes, aunque parezca natural.
2. **Antes de empezar un hito:** hacé `git commit` del estado actual (ver Hito 0). **Al terminarlo y comprobar que compila:** otro commit con el nombre del hito.
3. Compilá (`./gradlew assembleDebug`) al terminar cada hito y mostrame los cambios por archivo.
4. **Respetá el sistema visual** de `REDISENO_UI.md` y sus correcciones (`GlassSurface`, `GlassPanel`, `GlassAlertDialog`, `GlassDropdownMenu`, `PrimaryButton`, etc.). Tamaño mínimo de texto: 12 sp. No pongas blur en ítems de listas con scroll.
5. **No cambies lo que no esté en el hito.** Si ves un bug ajeno, anotalo al final de tu respuesta sin arreglarlo.
6. Textos de usuario en español y con tono simple. Los nuevos van directamente en el código, como el resto de la app (migrar a `strings.xml` es un hito aparte, el 14).
7. Para cada tarea larga (copiar, comprimir, escanear) usá `Dispatchers.IO`, comprobá `isActive` en los bucles y nunca bloquees el hilo principal.

## Estado verificado del código (31/10/2026, revisión de solo lectura)

**Ya existe, no hay que reconstruirlo:**
- Breadcrumb horizontal con scroll (`parseBreadcrumbs` y `horizontalScroll` en `ExplorerScreen.kt`, ~líneas 133 y 453).
- Selección por arrastre tras mantener presionado (`detectDragGesturesAfterLongPress`, `ExplorerScreen.kt:~730`, con `setSelectionRange` en el ViewModel).
- Selección múltiple en **duplicados** (`selectedDuplicatePaths`, `toggleDuplicateSelection`, `deleteSelectedDuplicates`).
- Caché de miniaturas en memoria (`LruCache` de 120) y descarga de archivos remotos a `cacheDir/remote_thumbs` (`FileThumbnails.kt:~74–108`).
- `VfsManager` con `listDirectory`, `readBytesAt`, `transferFile`, `downloadToLocalFile`, `createFolder`, `renameItem`, `deleteItem`.
- Filtro de tamaño mínimo (`minSizeMB`) en el buscador, que el motor aplica en `SmartSearchEngine.kt:~74`.

**No existe hoy:**
- Orden de resultados del buscador: el motor devuelve **siempre por fecha de modificación descendente** (`SmartSearchEngine.kt:~119`) y la UI no ofrece otro orden.
- Selección múltiple en los resultados del buscador normal (`MediaSearchResultCard` solo tiene compartir, abrir carpeta y borrar de a uno).
- ZIP / tar.gz, visor de texto (el visor integrado solo maneja imagen, video y audio), servicio en primer plano, progreso o cancelación en copiar/mover (hoy `pasteClipboardHere` lanza una corrutina y avisa con un Toast), papelera o deshacer, favoritos de red, SFTP.
- Contraseñas: se guardan con XOR y una clave fija en el código (`ConnectionStorage.kt`), lo que es ofuscación y no cifrado.

**Sobre la funcionalidad "perdida" del buscador:** el repositorio solo tiene un commit inicial (`e1e0c53`). Ni ese commit ni la versión actual tienen orden por tamaño ni selección en los resultados normales; solo los duplicados la tienen. Si la tenías en una versión anterior sin commitear, no queda rastro en git. Por eso el Hito 0 pide commitear, y el Hito 2 la deja de nuevo implementada.

## Resumen de hitos

| # | Hito | Prioridad | Esfuerzo | Depende de |
|---|------|-----------|----------|------------|
| 0 | Commit y cierre del rediseño (partes 3 y 4) | Alta | Bajo | – |
| 1 | Contraseñas con Android Keystore | Alta | Bajo | 0 |
| 2 | Buscador: orden, selección múltiple y «Archivos pesados» | Alta | Medio | 0 |
| 3 | Transferencias en segundo plano (`TransferService`) | Alta | Alto | 0 |
| 4 | Papelera y deshacer | Media | Medio | 3 |
| 5 | Comprimir y descomprimir (.zip, .tar.gz) | Alta | Medio | 3 |
| 6 | Visor de texto y logs | Alta | Medio | 0 |
| 7 | Favoritos de red | Media | Bajo | 0 |
| 8 | Caché en disco de miniaturas remotas | Media | Medio | 0 |
| 9 | Mejoras del Explorador (ruta y selección) | Baja | Bajo | 0 |
| 10 | Limpiador de carpetas vacías | Media | Medio | 2 |
| 11 | Compartir con PC (servidor HTTP) | Media | Alto | 3 |
| 12 | SFTP | Baja | Alto | 1 |
| 13 | Calidad: R8, tests y accesibilidad | Media | Medio | – |
| 14 | Partir archivos grandes y mover textos a `strings.xml` | Baja | Alto | todos |

Orden recomendado: **0 → 2 → 1 → 3 → 5 → 6 → 7 → 4 → 8 → 9 → 10 → 11 → 13 → 12 → 14.**

---

## Hito 0 – Commit y cierre del rediseño

1. Hacé `git add -A && git commit -m "Rediseño UI: estado actual"` (o desde Android Studio, Git → Commit).
2. Terminá lo pendiente de `REDISENO_UI_CORRECCIONES_3.md` y `REDISENO_UI_CORRECCIONES_4.md`: botones con blur, los dos `DropdownMenu` de `SmartSearchScreen.kt` (~líneas 840 y 950), el respaldo para Android < 12, el interruptor «Efectos de desenfoque» en Ajustes y que `GlassSurface` lea `LocalBlurEnabled`.
3. Commit: «Rediseño UI: correcciones y opción de desenfoque».

**Aceptación:** los criterios de aceptación de esos dos documentos; el árbol de git queda limpio.

---

## Hito 1 – Contraseñas con Android Keystore

**Archivos:** `services/ConnectionStorage.kt` (solo cifrado y descifrado).

1. Creá una clave AES-256 en el `AndroidKeyStore` (alias `filemanager_pw_key`, `KeyGenParameterSpec` con `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, sin autenticación de usuario).
2. `encryptPassword`: AES/GCM/NoPadding con IV aleatorio de 12 bytes; guardá `"ENC2:" + base64(iv + cifrado)`.
3. `decryptPassword` entiende tres formatos: `ENC2:` (nuevo), `ENC:` (XOR viejo) y texto plano.
4. **Migración:** al leer una conexión con formato viejo, se descifra y se vuelve a guardar en `ENC2:` en la siguiente escritura (por ejemplo al llamar a `saveConnections`). Nunca se pierde una contraseña.
5. Eliminá `ENC_KEY` del código solo cuando ya no haga falta leer `ENC:`. Dejá una función `decryptLegacyXor` aislada y comentada.
6. Si el Keystore falla (`KeyPermanentlyInvalidatedException` u otro), devolvé cadena vacía y que la UI pida de nuevo la contraseña; no crashees.

**Aceptación:** conexiones guardadas antes del cambio siguen conectando; en `shared_prefs/filemanager_connections.xml` ya no aparece nada reversible con la clave vieja.

---

## Hito 2 – Buscador: orden, selección múltiple y «Archivos pesados»

**Archivos:** `viewmodel/MainViewModel.kt` (estado de búsqueda), `ui/screens/SmartSearchScreen.kt`, `ui/components/Components.kt` si hace falta un componente. **No cambies** `SmartSearchEngine.kt` (el orden se aplica en el ViewModel).

### 2.1 Estado en el ViewModel
Junto a `selectedDuplicatePaths` (usalo como modelo):

```kotlin
enum class SearchSortField { DATE, SIZE, NAME, TYPE }

private val _searchSortField = MutableStateFlow(SearchSortField.DATE)
private val _searchSortAscending = MutableStateFlow(false)
private val _selectedSearchPaths = MutableStateFlow<Set<String>>(emptySet())
```

- `sortedSearchResults`: `combine(searchResults, field, ascending)` ordenado según el campo (DATE → `lastModified`, SIZE → `sizeBytes`, NAME → nombre sin distinguir mayúsculas, TYPE → categoría y luego nombre). La pantalla muestra **esta** lista, ya filtrada por lo que hoy haga `filteredSearchResults`.
- Funciones: `setSearchSort(field, ascending)`, `toggleSearchSelection(path)`, `selectAllSearchResults()`, `clearSearchSelection()`, `deleteSelectedSearchResults()` (misma lógica que `deleteSelectedDuplicates`, quitando también de `_searchResults`), y `shareSelectedSearchResults()` reutilizando `shareItems`.
- Al iniciar un escaneo nuevo o cambiar de herramienta, limpiá la selección.

### 2.2 Interfaz
- **Orden:** un control de ícono (reutilizá `IconOnlyDropdownFilter`) sobre la lista con Fecha, Tamaño, Nombre y Tipo, más un botón que alterna ascendente y descendente. Mostrá el estado actual («Tamaño ↓»).
- **Selección:** mantener presionado un resultado entra en modo selección; después, tocar alterna. Cada tarjeta muestra un indicador de selección (`CheckCircle` en `Aqua` cuando está seleccionada; fondo `SelectedViolet`), igual que `DuplicateFileCard`.
- **Barra de acciones** (solo con selección activa, arriba o abajo de la lista): «N seleccionados · X GB», botones Seleccionar todo, Compartir, Eliminar y Cancelar. Eliminar pide confirmación con `GlassAlertDialog` y muestra cuánto espacio se liberará.
- **Resumen:** una línea sobre la lista con «N archivos · X GB en total».
- **Archivos pesados:** tres chips (`> 100 MB`, `> 500 MB`, `> 1 GB`) junto al control de tamaño mínimo. Al tocar uno: `setMinSizeMB(...)`, orden por Tamaño descendente y, si ya hay resultados, volver a aplicar el filtro (si el motor necesita reescanear, llamá a `startSmartScan()`). El slider actual de `minSizeMB` sigue disponible.
- **Rendimiento:** las tarjetas de resultados siguen con `blur = false`; el indicador de selección no debe provocar recomposición de toda la lista (usá `key` por ruta y pasá `isSelected` calculado por ítem).

**Aceptación:**
- Se puede ordenar por Tamaño, Fecha, Nombre y Tipo en ambos sentidos, y el orden se conserva al hacer scroll y al eliminar.
- Se pueden seleccionar varios resultados, ver el total seleccionado y eliminarlos o compartirlos juntos.
- Los chips de 100 MB, 500 MB y 1 GB dejan la lista de mayor a menor tamaño.
- Los duplicados siguen funcionando igual.

---

## Hito 3 – Transferencias en segundo plano (`TransferService`)

**Archivos nuevos:** `services/TransferService.kt`, `services/TransferManager.kt`, modelo `TransferJob` en `model/`. **Se modifica:** `AndroidManifest.xml`, `MainViewModel.kt` (`pasteClipboardHere`), `MainActivity.kt` (permiso de notificaciones), una pantalla o diálogo de transferencias.

1. **Manifest:** permisos `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` y `POST_NOTIFICATIONS`. Declarar `<service android:name=".services.TransferService" android:exported="false" android:foregroundServiceType="dataSync" />`.
2. **`TransferManager`** (objeto único, sin DI): `StateFlow<List<TransferJob>>` con `id`, tipo (copiar, mover, comprimir, descomprimir), origen, destino, bytes totales y transferidos, velocidad, estado (en cola, activo, pausado, completado, error, cancelado) y mensaje de error. Funciones `enqueue(job)`, `cancel(id)`, `clearFinished()`. Ejecuta con un `CoroutineScope(SupervisorJob() + Dispatchers.IO)` propio, **no** el del ViewModel, para que sobreviva a la pantalla.
3. **`TransferService`:** arranca como foreground cuando hay trabajos activos y se detiene solo cuando termina el último. Notificación con título, barra de progreso, velocidad y botón **Cancelar**. Canal de notificación propio (`transfers`, importancia baja para que no suene en cada actualización; actualizá la notificación como máximo 2 veces por segundo). En Android 15, un servicio `dataSync` tiene un límite de tiempo: implementá `onTimeout` para detener el servicio y marcar los trabajos como pausados.
4. **Permiso de notificaciones (Android 13+):** pedirlo la primera vez que se inicia una transferencia; si lo niegan, la transferencia funciona igual con su progreso dentro de la app.
5. **Conflictos de nombre:** diálogo con Reemplazar, Omitir, Mantener ambos (renombrar con « (1)») y una casilla «Aplicar a todos».
6. **Transferencia:** `VfsManager.transferFile` queda como motor; agregale un callback de progreso y chequeo de cancelación si no los tiene. Mover entre ubicaciones distintas = copiar y luego borrar el original **solo si la copia terminó sin errores**.
7. **UI:** indicador de progreso global en el Explorador (por ejemplo un chip «2 transferencias» que abre una hoja con la lista, progreso y cancelar). Al terminar: Toast o Snackbar «Copiado: N archivos».
8. `pasteClipboardHere` pasa a **encolar** un trabajo en `TransferManager` en vez de lanzar una corrutina propia.

**Aceptación:** copiar un archivo grande por SMB, minimizar la app y apagar la pantalla no corta la transferencia; la notificación muestra progreso y permite cancelar; cancelar deja el destino sin archivos a medias (se borra el parcial).

---

## Hito 4 – Papelera y deshacer

**Archivos:** `MainViewModel.kt`, `ui/screens/ExplorerScreen.kt`, `ConnectionStorage` o un archivo de índice JSON, Ajustes.

1. **Local:** al eliminar, en vez de borrar, mover a `Android/data/<paquete>/files/.trash/` (o `filesDir/trash`) manteniendo un índice JSON (ruta original, fecha de borrado, tamaño). Restaurar devuelve el archivo a su ruta original (o pregunta si ya no existe).
2. **Remoto (SMB/FTP):** sin papelera; mantené la confirmación actual y avisá que es definitivo.
3. **Deshacer:** tras eliminar o renombrar local, un `Snackbar` «Eliminado · Deshacer» durante ~6 segundos.
4. **Ajustes:** «Usar papelera» (activado), «Vaciar papelera automáticamente a los 30 días» y botón «Vaciar ahora» con el tamaño que ocupa.
5. Acceso a la papelera desde el Explorador (menú), con Restaurar y Eliminar definitivamente, y selección múltiple.
6. Si mover a la papelera es lento (carpetas grandes), usá `TransferManager` del Hito 3.

**Aceptación:** borrar un archivo local lo manda a la papelera y se puede restaurar; el espacio liberado solo cuenta cuando se vacía la papelera; la confirmación sigue respetando el ajuste actual.

---

## Hito 5 – Comprimir y descomprimir (.zip y .tar.gz)

**Archivos nuevos:** `services/ArchiveManager.kt`. **Se modifica:** `ExplorerScreen.kt` (acciones de selección y de archivo), `MainViewModel.kt`, `app/build.gradle.kts` y `libs.versions.toml` (dependencia).

1. **Dependencia:** `org.apache.commons:commons-compress` (para tar.gz y para leer ZIP con codificación de nombres correcta). ZIP simple puede hacerse con `java.util.zip`, pero usá la misma librería para uniformidad.
2. **Comprimir:** con varios elementos seleccionados (archivos y carpetas, recursivo) aparece la acción **«Comprimir en ZIP»**. Pide el nombre (por defecto el de la carpeta o «Archivo.zip»), crea el ZIP en la carpeta actual y evita sobrescribir (usa el diálogo de conflictos del Hito 3). Opción adicional «Comprimir en .tar.gz».
3. **Descomprimir:** al tocar o mantener un `.zip`, `.tar`, `.tar.gz`, `.tgz` aparece **«Extraer aquí»** y «Extraer en carpeta nueva» (nombre del archivo sin extensión).
4. **Seguridad (obligatorio):** protegé contra *zip slip*. Antes de escribir cada entrada, resolvé la ruta destino con `canonicalPath` y rechazá cualquier entrada cuya ruta salga de la carpeta de extracción (`..`, rutas absolutas). Poné un límite razonable de entradas y de tamaño total descomprimido (avisar al usuario al superarlo) para evitar bombas de compresión.
5. **Ubicaciones remotas:** en SMB/FTP descargá a `cacheDir`, procesá en local y subí el resultado; mostrá el progreso de ambas fases. (Primero local; remoto puede quedar para una segunda pasada dentro del hito.)
6. Todo corre como trabajo del `TransferManager` (progreso, cancelación, notificación). Al cancelar, borrar el archivo parcial.
7. Zip con contraseña: fuera de alcance; si se detecta, mostrar «Archivo protegido con contraseña, no soportado todavía».

**Aceptación:** comprimir 3 archivos y 1 carpeta genera un ZIP que abre bien en el PC; extraer un ZIP de prueba con entradas `../evil.txt` **no** escribe fuera de la carpeta; se puede cancelar a la mitad sin dejar basura.

---

## Hito 6 – Visor de texto y logs

**Archivos nuevos:** `ui/components/TextViewerDialog.kt` (o pantalla). **Se modifica:** `MediaPreviewDialog.kt` o el punto donde `openOrNavigateItem` decide qué abrir, `model/FileModels.kt` si agregás una categoría.

1. **Tipos:** `.txt`, `.log`, `.json`, `.xml`, `.md`, `.csv`, `.ini`, `.conf`, `.yml`, `.yaml`, `.properties`, `.sh`, `.bat`, `.kt`, `.java`, `.py`, `.html`, `.css`, `.js`. Detectalos por extensión (o con una nueva categoría `TEXT`); si el archivo es binario (bytes nulos en los primeros 4 KB), no abrirlo como texto.
2. **Lectura segura con `readBytesAt`:** leer por bloques de 256 KB; mostrar los primeros ~1 MB y un botón **«Cargar más»**. Para `.log` agregá **«Ir al final»**, que lee los últimos 256 KB (tail) sin descargar todo. Funciona igual para SMB/FTP porque `readBytesAt` ya existe.
3. **Codificación:** UTF-8 por defecto, con caída a ISO-8859-1 si hay caracteres inválidos; selector manual de codificación.
4. **Interfaz:** fuente monoespaciada 13 sp, números de línea opcionales, alternar «ajustar líneas», tamaño de letra +/-, **búsqueda dentro del texto** (con navegación entre coincidencias), copiar todo/selección, compartir. JSON: botón «Formatear» (indentado) si el archivo es pequeño (< 2 MB). Fondo con `GlassPanel` y texto de alto contraste.
5. Si el archivo no se puede mostrar o es demasiado grande, ofrecer «Abrir con otra app» (el comportamiento actual).
6. Solo lectura. Editar texto queda fuera del hito.

**Aceptación:** abrir un `.log` de 50 MB por SMB es instantáneo (solo carga un bloque), «Ir al final» muestra las últimas líneas, y la búsqueda encuentra texto en la parte cargada.

---

## Hito 7 – Favoritos de red

**Archivos:** `model/FileModels.kt` (`NetworkBookmark`), `ConnectionStorage.kt` (guardar en JSON), `MainViewModel.kt`, `ExplorerScreen.kt`, `NetworkScreen.kt`.

1. Modelo: `NetworkBookmark(id, connectionId, path, label)`. Guardado como lista JSON en `SharedPreferences`, igual que las conexiones. Si se borra una conexión, se borran sus favoritos.
2. **Agregar:** botón de estrella junto a la barra de ruta del Explorador; pide una etiqueta (por defecto el último segmento). Vale también para rutas locales.
3. **Acceder:** sección «Favoritos» en Red LAN y un chip o desplegable de favoritos en el Explorador. Un toque conecta (si hace falta) y navega directo a la ruta con `connectAndBrowseNetwork` y `loadDirectory`.
4. **Gestionar:** renombrar y eliminar con mantener presionado. Si la ruta ya no existe, avisar y ofrecer eliminar el favorito.

**Aceptación:** guardar `smb://192.168.1.100/Series/Anime` y llegar a esa carpeta en un solo toque; sobrevive a cerrar la app.

---

## Hito 8 – Caché en disco de miniaturas remotas

**Archivos:** `ui/components/FileThumbnails.kt`, `ConnectionStorage` o Ajustes (límite y limpieza).

1. **Qué cambia:** hoy se descarga el archivo a `cacheDir/remote_thumbs` para generar la miniatura. Pasá a guardar **la miniatura ya generada** (JPEG ~256 px, calidad 80) en `cacheDir/thumbs/<hash>.jpg`.
2. **Clave:** hash de `connectionId + ruta + tamaño + fecha de modificación`. Si cambia el archivo, cambia la clave.
3. **Orden de búsqueda:** `LruCache` en memoria → disco → generar. Al generar, guardá en disco y en memoria.
4. **Fotos JPEG remotas:** probá primero leer solo los primeros 64–128 KB con `readBytesAt` y extraer la miniatura EXIF (`ExifInterface`); descargá el archivo completo solo si no hay miniatura embebida. Videos: descarga parcial solo si es viable; si no, mantener el comportamiento actual.
5. **Límite y limpieza:** máximo 200 MB (borrar los menos recientes primero) y botón «Limpiar miniaturas» en Ajustes junto al de «Caché de vistas previas remotas», con el tamaño ocupado.
6. Limitá la concurrencia de generación de miniaturas remotas (por ejemplo 3 a la vez) para no saturar la red ni la UI.

**Aceptación:** entrar por segunda vez a una carpeta remota de fotos muestra las miniaturas sin descargar nada; la caché respeta el límite.

---

## Hito 9 – Mejoras del Explorador (ruta y selección)

Ya existen el breadcrumb y la selección por arrastre; este hito solo los pule.

1. **Breadcrumb:** comprobá que **cada segmento sea tocable** (salta directo a esa carpeta). Si ya lo es: que al cambiar de carpeta la barra haga scroll automático hasta el último segmento, y que mantener presionado un segmento copie la ruta al portapapeles. Último segmento resaltado, resto en `TextMuted`.
2. **Selección por arrastre:** verificá que funcione igual en lista y en cuadrícula; que al arrastrar cerca del borde superior o inferior la lista haga **scroll automático**; vibración corta (`HapticFeedback`) al entrar al modo selección; que el rango se pueda reducir arrastrando hacia atrás.
3. Contador de seleccionados visible en la barra («N seleccionados · X MB»).

**Aceptación:** saltar 3 niveles atrás con un toque; seleccionar 40 archivos arrastrando sin soltar y sin que el scroll se quede trabado.

---

## Hito 10 – Limpiador de carpetas vacías

**Archivos:** `services/SmartSearchEngine.kt` o un nuevo `services/EmptyFolderScanner.kt`, `SmartSearchScreen.kt` (nueva herramienta), `MainViewModel.kt`.

1. Nueva herramienta en el buscador: **«Carpetas vacías»**. Recorre el almacenamiento local de forma recursiva y lista las carpetas sin archivos (considerá vacía también una carpeta que solo contiene otras carpetas vacías, y marcá solo la "hoja" más externa para limpiar de afuera hacia adentro).
2. **Excluí** siempre: `Android/`, `.thumbnails`, carpetas del sistema, carpetas ocultas (`.`) y cualquier carpeta que contenga `.nomedia`. Mostrar el recuento de excluidas es opcional.
3. Lista con selección múltiple (mismo modelo del Hito 2), «Seleccionar todo» y eliminación con confirmación. Eliminar carpetas vacías es seguro; usá la papelera solo si el Hito 4 lo trata como caso especial.
4. **Límite honesto:** los «restos de apps desinstaladas» en `Android/data` y `Android/obb` **no son accesibles** desde apps normales en Android 11+, ni siquiera con All Files Access. Esta función se limita a carpetas vacías del almacenamiento compartido. Si querés ir más allá, solo como heurística opcional: carpetas de primer nivel cuyo nombre coincida con un paquete que ya no está instalado, **mostradas como sugerencia** y nunca borradas automáticamente.

**Aceptación:** el escaneo no toca nada hasta que el usuario confirma; nunca lista carpetas de la lista de exclusiones.

---

## Hito 11 – Compartir con PC (servidor HTTP)

**Archivos nuevos:** `services/ShareServerService.kt`, `ui/screens/ShareScreen.kt` (o diálogo). **Se modifica:** `build.gradle.kts` (dependencia), `AndroidManifest.xml`.

Es la función más delicada en seguridad: expone archivos por la red. Hacelo **al final** y con estas condiciones.

1. **Servidor:** HTTP con NanoHTTPD (`org.nanohttpd:nanohttpd`). Navegar carpetas, descargar archivos (soportar `Range` para videos) y **subir** archivos desde el navegador.
2. **Control del usuario:**
   - Inicio manual con un botón «Compartir con PC»; nunca arranca solo.
   - Elige **una carpeta raíz** para compartir (por defecto, una carpeta que el usuario selecciona, no todo el almacenamiento).
   - Se muestra `http://IP:PUERTO` y un código QR.
   - **Autenticación obligatoria:** usuario fijo y una contraseña aleatoria de 6–8 caracteres generada en cada inicio y mostrada en pantalla.
   - Solo escucha en la interfaz Wi-Fi/LAN, no en datos móviles.
   - Se apaga solo tras un tiempo de inactividad configurable (por defecto 15 min) y al tocar «Detener».
3. **Servicio en primer plano** con notificación persistente («Compartiendo X · Detener») y tipo `dataSync`/`connectedDevice` según corresponda al `targetSdk 35`.
4. **Protección de rutas:** resolver toda ruta con `canonicalPath` y rechazar lo que salga de la carpeta raíz (`../`). Límite de tamaño de subida y de conexiones simultáneas. Sin listado de directorios fuera de la raíz.
5. Aviso visible: «El tráfico no está cifrado. Usalo solo en tu red de confianza.»
6. **Después, opcional:** WebDAV o FTP para montar como unidad en el Explorador de Windows. Apache FtpServer es pesado en Android; WebDAV sobre el mismo servidor es más simple. No lo empieces dentro de este hito.

**Aceptación:** desde el navegador del PC, con usuario y contraseña, se navega la carpeta compartida, se descarga un video con seek y se sube un archivo; sin credenciales o con una ruta `../` devuelve error; se detiene solo al cumplirse el tiempo.

---

## Hito 12 – SFTP

**Archivos:** `model/FileModels.kt` (`StorageProtocol.SFTP`), `services/VfsManager.kt`, `NetworkScreen.kt` (formulario), `ConnectionStorage.kt`.

1. Dependencia: `com.hierynomus:sshj` (o `org.apache.sshd:sshd-sftp`). Verificá compatibilidad con `minSdk 26` y los *proguard rules* si se activa R8.
2. Agregá el protocolo y su formulario: host, puerto (22), usuario, contraseña **o** clave privada (importar archivo).
3. Implementá en `VfsManager` el mismo contrato que SMB/FTP: `listDirectory`, `readBytesAt`, `transferFile`, `downloadToLocalFile`, `createFolder`, `renameItem`, `deleteItem`.
4. **Huellas de host:** al conectar por primera vez, mostrar la huella y pedir confirmación; guardarla y avisar si cambia (evita ataques de intermediario). No usar `PromiscuousVerifier`.
5. Usá la contraseña cifrada con el Keystore del Hito 1.

**Aceptación:** navegar, previsualizar, copiar y borrar en un servidor SFTP real; aviso claro si cambia la huella.

---

## Hito 13 – Calidad: R8, tests y accesibilidad

1. **R8:** `isMinifyEnabled = true` en `release`, con `proguard-rules.pro` para smbj, jcifs-ng, commons-net y sshj si está. Probá la build release completa (SMB, FTP, miniaturas, ZIP).
2. **Tests unitarios** (`app/src/test`): `SmartSearchEngine` (filtros por tamaño, categoría y duplicados con archivos temporales), la parte local de `VfsManager`, el orden y la selección de resultados (Hito 2), el descifrado con migración (Hito 1) y la protección *zip slip* (Hito 5).
3. **Accesibilidad:** reemplazar los ~31 `contentDescription = null` de íconos con acciones por descripciones reales; los decorativos pueden quedar en `null`. Áreas táctiles de al menos 48 dp. Probar con fuente grande del sistema.
4. **Rendimiento:** probar en build release con el interruptor de desenfoque apagado y encendido; si queda lenta, revisar `FileThumbnails.kt` y recomposiciones de las listas.

---

## Hito 14 – Partir archivos grandes y mover textos a `strings.xml`

Hacerlo **al final**, cuando las funciones estén estables, porque toca muchas líneas.

1. Partir por responsabilidad, sin cambiar comportamiento:
   - `MainViewModel` (~1056 líneas) en `ExplorerViewModel`, `SearchViewModel`, `NetworkViewModel` y `SettingsViewModel`, compartiendo un repositorio simple.
   - `ExplorerScreen` (~1480), `SmartSearchScreen` (~1393) y `Components` (~1107) en archivos por componente o por sección.
   - `VfsManager` (~1185) en una clase por protocolo, detrás de una interfaz común.
2. Mover los textos de usuario a `res/values/strings.xml`. Solo después, si hace falta, agregar `values-en`.
3. Un commit por archivo partido; compilar y probar a mano después de cada uno.

---

## Prompt para empezar con Gemini

> Seguí `PLAN_EJECUCION.md`. Empezá por el **Hito 0** y no pases al siguiente hasta que yo lo confirme. Mostrame los cambios por archivo, compilá y hacé un commit por hito. Respetá las reglas del inicio del documento.
