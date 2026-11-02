# FileManager Studio — Revisión para producción

*Fecha: 2026-11-02 · Revisión estática del código en `E:\Documents\Proyectos Software\FileManager` (no se compiló ni ejecutó nada: todo lo marcado como bug sale de leer el código y conviene reproducirlo).*

Convención: **P0** = bloquea publicar o puede destruir datos · **P1** = antes de publicar · **P2** = calidad · **P3** = deseable.

---

## 0. Decisión de diseño: sin Blur

Pedido: evitar Blur en esta app. Estado real del código:

- La librería `haze` (`libs.haze`, `dev.chrisbanes.haze`) está en `app/build.gradle.kts` pero **no se usa para dibujar nada**: en `Components.kt` solo se crea un `HazeState` en `AppBackground` y se publica en `LocalHazeState`; no hay ningún `hazeSource`/`hazeEffect` en uso.
- `GlassSurface(blur = …)` recibe el parámetro `blur` y **lo ignora** (solo hace `clip + background + border`). Hay ~15 llamadas que pasan `blur = true/false` sin efecto.
- `LocalBlurEnabled`, `MainViewModel.blurEnabled/setBlurEnabled` y `ConnectionStorage.getBlurEnabled/setBlurEnabled` (`blur_enabled`) son código muerto: ninguna pantalla de Ajustes expone el interruptor.
- **El único blur real** es `DialogWindowBlur()` (`Components.kt` ~l.818, `FLAG_BLUR_BEHIND` + `blurBehindRadius`), usado en `GlassAlertDialog` y en `MediaPreviewDialog.kt:100`.

Cambios:

1. [P1] Quitar `implementation(libs.haze)` y la entrada `haze` de `gradle/libs.versions.toml`.
2. [P1] Borrar `LocalHazeState`, el `HazeState` de `AppBackground` y los imports `dev.chrisbanes.haze.*`.
3. [P1] Reemplazar `DialogWindowBlur()` por un atenuado simple: solo `window.setDimAmount(0.55f)` (sin `FLAG_BLUR_BEHIND`). Eliminar la llamada en `GlassAlertDialog` y `MediaPreviewDialog`.
4. [P2] Eliminar el parámetro `blur` de `GlassSurface` y de todas sus llamadas; eliminar `LocalBlurEnabled`, `blurEnabled`, `getBlurEnabled/setBlurEnabled`.
5. [P2] Renombrar `GlassSurface`/`GlassAlertDialog` a algo neutro (`Surface…`) si se quiere dejar de aludir a "glass"; es cosmético.
6. [P2] Actualizar `REDISENO_UI_CORRECCIONES_3.md` y `_4.md`: marcar el punto "blur / respaldo Android < 12" como **descartado** (ya no hay blur, no hace falta respaldo).
7. Regla de código: no usar `Modifier.blur`, `RenderEffect`, `FLAG_BLUR_BEHIND` ni librerías de blur.

---

## 1. Bugs críticos (pueden destruir datos)

### 1.1 [P0] Copiar/mover un archivo sobre sí mismo lo trunca (y lo borra al fallar)
`VfsManager.transferFile`, rama LOCAL→LOCAL (~l.775-800): se abre `srcFile.inputStream()` y luego `dstFile.outputStream()`. Si origen y destino son el mismo archivo (usuario elige "Reemplazar" en la misma carpeta, o pega en la carpeta de origen), `outputStream()` trunca el archivo a 0 bytes antes de leer. Además, en los `catch` se hace `if (dstFile.exists()) dstFile.delete()`, que en ese caso **borra el original**. Con "Mover" (`deleteSourceAfter`) también se borra el origen tras truncarlo.

Corrección:
- Antes de transferir, comparar `srcFile.canonicalFile == dstFile.canonicalFile` (y para SMB/FTP, mismo host + misma ruta normalizada). Si coinciden: omitir (mover) o forzar "Conservar ambos" (copiar).
- Copiar a un archivo temporal (`.name.part`) en la carpeta destino y hacer `renameTo` atómico al terminar; así un fallo nunca deja el destino roto ni borra un archivo previo.
- No borrar el destino en el `catch` si no fue creado por esta operación.

### 1.2 [P0] Copiar una carpeta dentro de sí misma: recursión infinita
`transferFile` (rama `source.isDirectory`) crea la carpeta destino **antes** de listar los hijos del origen. Si el destino está dentro del origen, el listado incluye la carpeta recién creada y se recursa sin fin.
- Guarda: rechazar si `dst.canonicalPath` empieza con `src.canonicalPath + File.separator` (local) o prefijo equivalente por conexión (SMB/FTP).
- Listar los hijos **antes** de crear la carpeta destino.
- Comprobar el resultado de `createFolder` (hoy `created` se calcula y se ignora): si falla, abortar ese subárbol y marcar el job con error.

### 1.3 [P0] `renameItem` y `createFolder` locales sin validación
- `File.renameTo` en Android/Linux **reemplaza en silencio** un archivo existente. Comprobar `dst.exists()` antes y devolver un error "ya existe".
- Aceptar nombres con `/`, `\`, `..`, vacíos, `.` o solo espacios. Validar en un único `validateFileName()` usado por UI y VFS (también para SMB/FTP).
- `createFolder` usa `mkdirs()` (crea ancestros y devuelve `false` si ya existe, sin distinguir causas). Usar `mkdir()` + validar nombre.

### 1.4 [P0] El borrado es permanente y el aviso se puede desactivar
`deleteItem` hace `deleteRecursively()`. En Ajustes existe "Confirmar antes de eliminar" que se puede apagar.
- Mínimo: para carpetas, mostrar siempre el conteo de elementos y confirmar aunque el ajuste esté apagado.
- Recomendado: papelera local de la app (carpeta `.trash` con índice y vaciado automático a los N días) o "Deshacer" de 5-10 s en un Snackbar antes de borrar de verdad. Para SMB/FTP, dejar claro que no hay papelera.

### 1.5 [P0] `checkDestinationExists` falla "abierto"
En `TransferManager.checkDestinationExists`, si el listado lanza excepción devuelve `false` ⇒ se **sobrescribe sin avisar**. Debe fallar "cerrado": si no se puede listar el destino, pausar el job y preguntar/abortar con error visible.
Además lista toda la carpeta destino **por cada archivo** (O(n²) por red). Listar una sola vez por carpeta destino y cachear el set de nombres durante el job.

### 1.6 [P0] `TransferManager` no es thread-safe
- `cancelledJobIds` es un `mutableSetOf` tocado desde IO (worker) y Main (UI/servicio) → usar `ConcurrentHashMap.newKeySet()`.
- `_jobs.value = _jobs.value.map {…}` hace read-modify-write sin atomicidad (callback de progreso vs. `cancel()` vs. `enqueue()` se pisan). Usar `_jobs.update { … }` en **todos** los puntos (`enqueue`, `cancel`, `clearFinished`, `updateJob`).
- `cancel()` marca CANCELLED y luego `processJob` puede sobrescribir el estado final (COMPLETED/ERROR). Respetar CANCELLED como estado terminal.
- `onProgress` actualiza el `StateFlow` por **cada bloque de 64 KB**: miles de emisiones por segundo en LAN rápida. Acumular y emitir como máximo cada ~250-500 ms.

### 1.7 [P0] El diálogo de conflicto puede colgar la cola
`processJob` espera `deferred.await()` sin tiempo límite ni cancelación. Si la UI se destruye (rotación del proceso, usuario sale) o el usuario cancela el job mientras el diálogo está abierto, `cancel()` no completa el `deferred` y la cola queda bloqueada para siempre.
- Completar el `deferred` con `SKIP`/cancelado desde `cancel()`.
- `activeConflict` no debe vivir solo en la UI: si no hay observador, aplicar la política por defecto (omitir) o pausar el job con estado `WAITING_USER` visible en la notificación.

---

## 2. Seguridad y privacidad

| # | Prio | Hallazgo | Acción |
|---|---|---|---|
| S1 | P1 | `android:usesCleartextTraffic="true"` en el manifest. FTP y SMB son sockets y esa flag no les afecta (solo `HttpURLConnection`/WebView/OkHttp). | Quitarla (`false`). |
| S2 | P1 | FTP va en claro: `VfsManager.withFtpClient` usa `FTPClient` (no `FTPSClient`). El comentario de `build.gradle.kts` dice "FTP/FTPS". Credenciales viajan sin cifrar. | Implementar FTPS explícito (`FTPSClient`, `execPBSZ(0)`, `execPROT("P")`) con validación de certificado y diálogo de "confiar en este certificado"; o mostrar advertencia fija "FTP sin cifrar" en la conexión y en el formulario. Corregir el comentario. |
| S3 | P1 | SMB: `jcifs.smb.client.ipcSigningEnforced=false` y fallback silencioso a **invitado/anónimo** (`buildJcifsContext`, `listSmbDirectory` ~l.211-233). | Activar signing por defecto (opción avanzada para NAS viejos). Si se cae a invitado, mostrarlo en la UI ("Conectado como invitado") y no hacerlo sin que el usuario lo haya pedido. |
| S4 | P1 | `ConnectionStorage.encryptPassword` devuelve `""` si falla el Keystore (incluida `KeyPermanentlyInvalidatedException`). Guardar una conexión **borra la contraseña sin avisar**. | Devolver un resultado tipado (`Result`) y avisar: "No se pudo guardar la contraseña de forma segura". Si la clave quedó invalidada, regenerarla y pedir la contraseña de nuevo con mensaje claro. |
| S5 | P1 | `res/xml/file_paths.xml` declara `<root-path path=".">` y `<external-path path=".">`: exponen todo el almacenamiento vía FileProvider. | Quitar `root-path`; limitar `external-path` y `cache-path` a subcarpetas concretas (p. ej. `cache/preview/`). Generar URIs solo para archivos que el usuario abre. |
| S6 | P1 | `MANAGE_EXTERNAL_STORAGE`: requiere formulario de declaración en Play Console (la app es un gestor de archivos, caso permitido) y la ficha debe describirlo. Ya existe aviso previo (disclosure) en `MainActivity`. | Completar la declaración; mantener el aviso; revisar el texto vigente en Policy Center antes de subir. |
| S7 | P1 | Licencias: `jcifs-ng` es **LGPL**, `smbj`/`commons-net` Apache-2.0, BouncyCastle MIT. No hay pantalla de licencias en esta app (MediaRecovery sí tiene un diálogo de "licencias de código abierto"). | Agregar pantalla "Licencias de código abierto" (reutilizar la de MediaRecovery); para LGPL, indicar dónde obtener el código y permitir reemplazar la librería (no ofuscarla con keep rules más allá de lo necesario). |
| S8 | P1 | Política de privacidad inexistente en el repo. | Redactarla (conexiones guardadas, contraseñas cifradas localmente, sin telemetría) y publicarla en una URL. |
| S9 | P2 | `android:requestLegacyExternalStorage="true"` no tiene efecto con target ≥ 30. | Quitar. |
| S10 | P2 | Falta `data_extraction_rules`/`fullBackupContent` (con `allowBackup="false"` basta, pero el lint de target 31+ lo advierte). | Añadir reglas vacías o `tools:ignore`. |
| S11 | P2 | Al activar R8 (ver §3), los keep de `org.bouncycastle.**`, `jcifs.**`, `com.hierynomus.**` son muy amplios. | Acotarlos a lo necesario (SMBJ y jcifs usan reflexión/ServiceLoader) y probar SMB/FTP en un build **release**. |

Lo que ya está bien: AES-256-GCM con Android Keystore para contraseñas (`ENC2:`), `allowBackup="false"`, disclosure previo para All Files Access, `exported="false"` en servicio y provider.

---

## 3. Build, firma y publicación

- [P0] **Target API**: `targetSdk = 35` y `compileSdk = 35`. Desde el 31/08/2026, nuevas apps y actualizaciones deben apuntar a **API 36** (confirmado en Play Help / developer.android.com; Android TV solo exige 34). Subir `compileSdk = 36` y `targetSdk = 36` y revisar cambios de comportamiento de Android 16 (edge-to-edge obligatorio, back predictivo, límites de foreground service).
- [P0] **Firma**: no hay `signingConfigs`. Añadir `keystore.properties` (fuera del repo) como en TvLauncher, usar Play App Signing y hacer que `bundleRelease` **falle** si falta el keystore.
- [P0] **R8**: `isMinifyEnabled = false` → los keep de `proguard-rules.pro` hoy no hacen nada. Activar `isMinifyEnabled = true` + `isShrinkResources = true`, eliminar logs en release (`-assumenosideeffects class android.util.Log`) y **probar SMB, FTP, cifrado y miniaturas en release**.
- [P0] **Repo**: `FileManagerStudio_v1.0.apk` (20 MB) y `filemanager-screenshot.mp4` (12 MB) están **versionados en git**. Sacarlos del historial (`git rm --cached` + `git filter-repo`/BFG si el repo es público) y agregar al `.gitignore`: `*.apk`, `*.aab`, `*.mp4`, `*.jks`, `*.keystore`, `keystore.properties`. Publicar el APK en GitHub Releases, no en el árbol.
- [P1] `versionCode = 1`: definir esquema (p. ej. `major*10000 + minor*100 + patch`) o leerlo de `gradle.properties`/CI.
- [P1] Ficha Play: capturas, icono 512, descripción, declaración de `MANAGE_EXTERNAL_STORAGE`, Data safety (la app no recopila datos; conexiones y credenciales quedan en el dispositivo).
- [P1] Si se distribuye también por GitHub: dos *flavors* no son necesarios aquí; basta con declarar el permiso (la app lo justifica como gestor de archivos).

---

## 4. Foreground service de transferencias (`TransferService`)

- [P1] `override fun onTimeout(startId: Int)` solo cubre API 34. En Android 15+ el timeout de `dataSync` llama a `onTimeout(startId, fgsType)`. Implementar **ambas** sobrecargas (verificar firmas en la documentación de `Service.onTimeout`), cancelar/pausar el job activo y avisar al usuario ("La transferencia se detuvo por límite del sistema; reanudar").
- [P1] Pedir `POST_NOTIFICATIONS` antes de la primera transferencia (ya se solicita en `MainActivity` ~l.255; confirmar que se pide *antes* de `TransferService.start`) y comportarse bien si se niega (la transferencia corre igual; avisar que no se verá progreso).
- [P2] Textos de la notificación hardcodeados en español ("Cancelar", "Completado", "Preparando…") → `strings.xml`.
- [P2] Mostrar notificación final (éxito/error) separada de la ongoing, con el resultado por archivo.
- [P2] `collectLatest` de jobs en `Dispatchers.Main` hace `nm.notify` en cada emisión; con el throttle de §1.6 queda resuelto.

---

## 5. UI, accesibilidad y ajustes

- [P1] `BackHandler(enabled = selectedTab == 0)`: en las otras pestañas "Atrás" **cierra la app**. Volver a la pestaña 0 antes de salir; cerrar primero overlays/selección/búsqueda.
- [P1] **Textos**: 0 `stringResource`; `strings.xml` tiene 120 bytes (solo `app_name`). ~165 literales en Compose + literales en servicios/ViewModel. Externalizar a `strings.xml` (+ `values-en`), incluyendo mensajes de error del VFS.
- [P1] **TalkBack**: 32 `contentDescription = null` (decide cuáles son decorativos y describe el resto: botones de icono, miniaturas con acción, estados de selección). Verificar tamaño táctil mínimo 48 dp.
- [P2] Pendientes de `REDISENO_UI_CORRECCIONES_3` y `_4`: revisar lo que quede; el punto de blur/Android < 12 se descarta (§0).
- [P2] Archivos enormes: `ExplorerScreen.kt` 80 KB, `SmartSearchScreen.kt` 76 KB, `VfsManager.kt` 61 KB, `MainViewModel.kt` 58 KB. Partir por responsabilidad (VFS local/SMB/FTP en clases separadas con interfaz común `VfsBackend`; ViewModel por pantalla).
- [P2] Estado vacío, error de red y "sin permiso" con acciones claras en Explorer y Red.
- [P2] Ajustes: añadir "Licencias", "Política de privacidad", versión/compilación, y opción de verificar certificados FTPS/SMB signing (§2).
- [P2] Los `!!` (7) y `catch (_: Exception) { false }` en `VfsManager` ocultan la causa real: devolver un `Result` con mensaje traducible (`translateSmbException` ya existe para SMB; hacer lo mismo en FTP/local).

---

## 6. Calidad y pruebas

- [P1] No hay `src/test` ni `src/androidTest`. Mínimo recomendado antes de publicar:
  - Unit tests del VFS local con carpeta temporal: copia sobre sí mismo, carpeta dentro de sí misma, renombrar sobre existente, nombres inválidos.
  - Unit tests de `TransferManager` (cola, cancelación durante conflicto, concurrencia de `update`).
  - Test de `ConnectionStorage` (cifrado/descifrado, clave invalidada).
  - Prueba manual contra un servidor SMB y uno FTP reales (y un FTP que falle a mitad) con el build **release minificado**.
- [P2] `lintRelease` con `abortOnError = true` como en TvLauncher.
- [P2] Probar en Android 11, 13, 14 y 16 (almacenamiento, notificaciones, foreground service).

---

## 7. Orden sugerido

1. §1.1 → §1.3 (copia sobre sí mismo, carpeta en sí misma, rename/mkdir): pueden destruir datos.
2. §1.5 → §1.7 (conflictos y `TransferManager`).
3. Quitar blur/haze (§0) — es un cambio pequeño y reduce dependencias antes de activar R8.
4. Seguridad: cleartext, FTPS/aviso, SMB signing/invitado, FileProvider, `encryptPassword` (§2).
5. Target 36 + firma + R8 + limpieza del repo (§3).
6. `onTimeout`, back, strings, TalkBack, tests (§4-§6).
