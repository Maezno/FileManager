# QubitExplorer — Textos y respuestas para Play Console

Todo lo de aquí se copia y pega a mano en Play Console. Revisa cada respuesta antes de enviarla: quien declara eres tú.

## 1. Política de privacidad (GitHub Pages)
Archivo: `docs/privacy/index.html` (español + inglés).
1. Correo de contacto: ya incluido.
2. Hacer commit y push a `main`.
3. GitHub → repo `Maezno/FileManager` → Settings → Pages → Source: *Deploy from a branch*, rama `main`, carpeta `/docs`.
4. URL resultante: `https://maezno.github.io/FileManager/privacy/`
5. Comprobar que abre sin iniciar sesión y pegarla en Play Console → Contenido de la app → Política de privacidad.

Nota: el repo es público, así que todo el código queda visible. Si no quieres eso, crea un repo aparte solo para la política (p. ej. `Maezno/qubitexplorer-privacy`).

## 2. Ficha de Play Store

**Nombre (≤30):** `Qubit Explorer`

**Descripción breve (≤80)**
- ES: `Gestor de archivos con acceso a carpetas de red SMB y FTP, búsqueda y duplicados.`
- EN: `File manager with SMB & FTP network shares, smart search and duplicate finder.`

**Descripción completa — ES**
```
Qubit Explorer es un gestor de archivos rápido y sin anuncios para tu almacenamiento interno, tu tarjeta SD y las carpetas compartidas de tu red local.

EXPLORA TUS ARCHIVOS
• Vista de lista o cuadrícula, ordenación y accesos rápidos a Descargas, Cámara, Vídeos y más
• Copiar, mover, renombrar, crear carpetas y eliminar
• Vista previa de imágenes, vídeo y texto, y editor de texto integrado
• Abre y extrae archivos ZIP, 7z, RAR, TAR, GZ y XZ

RED LOCAL: SMB Y FTP
• Accede a carpetas compartidas de Windows y NAS (SMB v2/v3)
• Servidores FTP y FTPS con cifrado TLS
• Detecta dispositivos de tu Wi-Fi automáticamente
• Transferencias en segundo plano con progreso en las notificaciones

LIBERA ESPACIO
• Encuentra los archivos más grandes de tu teléfono
• Detecta archivos duplicados y compara carpetas

PRIVACIDAD
• Sin anuncios, sin cuentas y sin rastreadores
• Tus archivos nunca salen del dispositivo ni de tu red local
• Las contraseñas de tus servidores se guardan cifradas con el Android Keystore

Tema claro, oscuro o del sistema. Disponible en español e inglés.
```

**Descripción completa — EN**
```
Qubit Explorer is a fast, ad-free file manager for your internal storage, SD card and the shared folders on your local network.

BROWSE YOUR FILES
• List or grid view, sorting and shortcuts to Downloads, Camera, Videos and more
• Copy, move, rename, create folders and delete
• Preview images, video and text, plus a built-in text editor
• Open and extract ZIP, 7z, RAR, TAR, GZ and XZ archives

LOCAL NETWORK: SMB AND FTP
• Access Windows and NAS shared folders (SMB v2/v3)
• FTP and FTPS servers with TLS encryption
• Automatically discover devices on your Wi-Fi
• Background transfers with progress in notifications

FREE UP SPACE
• Find the largest files on your phone
• Detect duplicate files and compare folders

PRIVACY
• No ads, no accounts, no trackers
• Your files never leave your device or your local network
• Server passwords are stored encrypted with the Android Keystore

Light, dark or system theme. Available in English and Spanish.
```

**Recursos gráficos**
- Icono: `Resources/app_icon_play_store.png` (512×512)
- Gráfico de funciones: `playstore/graphics/feature_graphic_1024x500.png`
- Capturas de teléfono: `playstore/screenshots/phone/01…05.png`

**Categoría:** Herramientas · **Etiquetas:** Gestor de archivos.

## 3. Declaración de permiso "Acceso a todos los archivos"
Contenido de la app → Permisos confidenciales → Acceso a todos los archivos.
- **Uso principal:** Gestión de archivos (File management).
- **Justificación (EN, para pegar):**
```
Qubit Explorer is a file manager. Its core functionality is to let the user browse, copy, move, rename, extract, search and delete any file in shared storage and on SD cards, including files created by other apps and non-media files (documents, archives, APKs). It also transfers files between local storage and the user's own SMB/FTP servers. These operations require broad access to all files and cannot be done with the Storage Access Framework or MediaStore without breaking the core file-manager experience.
```
- **Capturas del flujo** (`playstore/screenshots/permiso/`): 01 app sin acceso → 02 diálogo explicativo de la app → 03 ajuste del sistema desactivado → 04 activado → 05 app con acceso completo.
- **Vídeo:** `playstore/videos/permiso_todos_los_archivos.mp4` (21 s, montado con esas 5 capturas; la carpeta no se sube a git). Subirlo a YouTube como "No listado" y pegar el enlace.

## 4. Declaración de servicio en primer plano
Contenido de la app → Servicios en primer plano → tipo **Sincronización de datos (dataSync)**.
- **Descripción:** `Transfers (copy/move) of files between the device and the user's SMB/FTP servers, started by the user, shown with a progress notification and stopped when the transfer ends.`
- **Impacto si se interrumpe:** `The file transfer would fail midway, leaving incomplete files.`
- **Vídeo:** `playstore/videos/transferencia_servicio_primer_plano.mp4` (23 s, capturas de `playstore/screenshots/transferencia/`: servidor FTP guardado → conectado → pegar en Descargas → progreso en la app → notificación → archivo copiado). Subirlo a YouTube como "No listado".

## 5. Seguridad de los datos (Data safety)
- ¿Recopila o comparte algún tipo de datos de usuario? **No.**
  (Las credenciales SMB/FTP y los archivos se procesan solo en el dispositivo y no se envían al desarrollador ni a terceros; Google no considera "recopilación" el procesamiento solo en el dispositivo.)
- Cifrado en tránsito: no aplica al no recopilar datos.
- Eliminación de datos: no aplica (no hay cuenta); mencionar que se borran al desinstalar.

## 6. Resto de "Contenido de la app"
| Formulario | Respuesta |
|---|---|
| Anuncios | No contiene anuncios |
| Acceso a la app | Toda la funcionalidad está disponible sin restricciones (no hay inicio de sesión) |
| Clasificación de contenido | Categoría *Utilidad, productividad, comunicación u otras*; responder **No** a todo (violencia, sexo, lenguaje, drogas, apuestas, interacción entre usuarios, compras). Resultado esperado: PEGI 3 / Para todos |
| Público objetivo | 18+ (o 13+). No marcar edades menores de 13 para evitar requisitos de Familias |
| Aplicación de noticias | No |
| Apps gubernamentales / financieras / salud | No |
| ID de publicidad | No usa el ID de publicidad |

## 7. Publicación
1. Cuenta personal nueva de Play Console: es obligatoria una **prueba cerrada con ≥12 testers durante 14 días** antes de poder pasar a producción.
2. Crear la app → Pruebas → Prueba cerrada → subir `app/build/outputs/bundle/release/app-release.aab` → aceptar Play App Signing.
3. Tras los 14 días, solicitar acceso a producción.
