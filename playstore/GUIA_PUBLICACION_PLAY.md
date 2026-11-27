# Guía de publicación en Google Play (proceso usado con QubitExplorer)

Esta guía resume todo lo que se hizo para publicar QubitExplorer y sirve de contexto para la próxima app. Fecha: 27/11/2026.

---

## 0. Datos de QubitExplorer, como referencia

| Dato | Valor |
|---|---|
| Paquete | `com.maez.qubitexplorer` (no se puede cambiar después de subir el primer AAB) |
| Versión actual | 1.1.1, versionCode 3 |
| Repositorio | https://github.com/Maezno/FileManager (público) |
| Política de privacidad | https://maezno.github.io/FileManager/privacy/ |
| Términos del servicio | https://maezno.github.io/FileManager/terms/ |
| Clave de subida | `E:\Android\Key\qubitexplorer-upload.jks`, alias `upload` |
| Idioma de la ficha | es-419 (predeterminado) + en-US |
| Contacto | maezno@gmail.com |

La carpeta de trabajo buena es `E:\Documents\Proyectos Software\FileManager`. La copia en `C:\Users\Maez\Documents\...` es secundaria: antes de usarla, hacer `git pull`.

---

## 1. Preparar el proyecto

### 1.1 Firma de la versión
- Hay que crear **una clave de subida nueva por app** y guardarla en `E:\Android\Key\`. Con Play App Signing, Google guarda la clave real de la app; la nuestra solo sirve para subir.
- Comando, poniendo los datos del certificado para que solo pida las contraseñas:
  ```
  keytool -genkeypair -v -keystore E:\Android\Key\<app>-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Matias Nougues, OU=Maezno, O=Maezno"
  ```
- Las contraseñas las escribe el usuario, nunca el asistente.
- **`keystore.properties`** en la raíz del proyecto, fuera de git:
  ```
  storeFile=E:/Android/Key/<app>-upload.jks
  keyAlias=upload
  storePassword=...
  keyPassword=...
  ```
- Hay que añadir `keystore.properties.example` al repo y, en `.gitignore`, `keystore.properties`, `*.jks` y `*.keystore`.
- Configuración de `app/build.gradle.kts`, con `import java.util.Properties` arriba del todo:
  ```kotlin
  val keystorePropsFile = rootProject.file("keystore.properties")
  val keystoreProps = Properties().apply { if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) } }
  signingConfigs { if (keystorePropsFile.exists()) { create("release") {
      storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
      storePassword = keystoreProps.getProperty("storePassword")
      keyAlias = keystoreProps.getProperty("keyAlias")
      keyPassword = keystoreProps.getProperty("keyPassword")
  } } }
  buildTypes { release { signingConfigs.findByName("release")?.let { signingConfig = it }; isMinifyEnabled = true; isShrinkResources = true } }
  ```
- **Copia de seguridad** del `.jks` y de `keystore.properties`. Si se pierden, hay que pedir a Google que restablezca la clave de subida.

### 1.2 Compilar
- Como JAVA_HOME se usa el JBR de Android Studio: `C:/Program Files/Android/Android Studio/jbr`.
- Comando: `./gradlew bundleRelease`. El AAB sale en `app/build/outputs/bundle/release/app-release.aab`.
- Para comprobar la firma: `jarsigner -verify app-release.aab` debe responder `jar verified`.
- `lintVitalRelease` bloquea errores que en debug no aparecen. Por ejemplo, Billing trae una versión vieja de Fragment, y se arregla añadiendo `androidx.fragment:fragment-ktx` 1.8.x a mano.

### 1.3 Requisitos técnicos a revisar
- `targetSdk` actual: lo que pida Play en ese momento; QubitExplorer usa 36.
- Llamar a `enableEdgeToEdge()` en la Activity, y no definir `statusBarColor` ni `navigationBarColor` en los temas, porque en Android 15 están obsoletos.
- Activar `android.r8.optimizedResourceShrinking=true` en `gradle.properties`.
- Tener cuidado con reglas `-keep class lib.** { *; }`: dejan el porcentaje de R8 bajo, pero quitarlas puede romper librerías que usan reflexión.
- Desactivar la copia de seguridad automática si la app guarda credenciales.
- Las contraseñas se guardan cifradas con el Android Keystore.
- **Símbolos nativos:** si el AAB lleva archivos `.so`, aunque sean de Compose, Play avisa. Se arma un zip con una carpeta por arquitectura, con **rutas separadas por `/`**. `Compress-Archive` de PowerShell 5 usa `\` y Play rechaza el zip, así que hay que crearlo con Python:
  ```
  python -I -c "import zipfile,os;src='app/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib';z=zipfile.ZipFile('native-debug-symbols.zip','w',zipfile.ZIP_DEFLATED);[z.write(os.path.join(src,a,f),a+'/'+f) for a in os.listdir(src) for f in os.listdir(os.path.join(src,a))];z.close()"
  ```
  Es opcional: sin él solo aparece un aviso.

---

## 2. Recursos gráficos

| Recurso | Requisito | Dónde estaba en QubitExplorer |
|---|---|---|
| Icono | PNG 512×512, de 32 bits, ≤1 MB y cuadrado a sangre: sin esquinas redondeadas ni sombra | `Resources/app_icon_play_store.png` |
| Gráfico de funciones | 1024×500, sin canal alfa | `playstore/graphics/feature_graphic_1024x500.png` |
| Capturas de teléfono | De 2 a 8, proporción 9:16 (1080×1920) y sin canal alfa | `playstore/screenshots/phone/` |

### Capturas en el emulador
- El emulador va a 1080×2400, que es 20:9 y supera el límite de 2:1 de Play. Se fuerza el tamaño con `adb shell wm size 1080x1920` y al terminar se deshace con `wm size reset`.
- Para una barra de estado limpia se usa el modo demostración de SystemUI: `settings put global sysui_demo_allowed 1` y después los `am broadcast -a com.android.systemui.demo ...` de hora, batería y red.
- Para poner la app en español sin cambiar el idioma del sistema: `adb shell cmd locale set-app-locales <paquete> --locales es-419`.
- Captura: `adb shell screencap -p /sdcard/x.png` y luego `adb pull`. En PowerShell 5 no hay que usar `exec-out >`, porque corrompe el PNG.
- La primera captura después de `wm size` a veces sale negra: hay que despertar la pantalla (`input keyevent KEYCODE_WAKEUP`) y repetirla.
- En Git Bash, los comandos de adb con rutas necesitan `MSYS_NO_PATHCONV=1` o rutas del tipo `//sdcard/...`.
- Antes de capturar hay que crear datos de ejemplo para que las pantallas no salgan vacías.

### Vídeos para las declaraciones
- Se montan a partir de capturas con transiciones `xfade` de ffmpeg; cada vídeo dura 20–30 s.
- Grabar la pantalla con `screenrecord` dio problemas en el emulador.
- Se suben a YouTube como **"No listado"**, nunca "Privado", porque el revisor tiene que poder verlos.
- La carpeta `playstore/videos/` va en `.gitignore`.
- Para enseñar transferencias por red sin un servidor real, se levanta pyftpdlib en el PC (anónimo, `127.0.0.1:2121`, con la velocidad limitada para que se vea la barra de progreso). Desde el emulador, el PC es `10.0.2.2`.

---

## 3. Política de privacidad y términos (GitHub Pages)
- Cada página es un archivo: `docs/privacy/index.html` y `docs/terms/index.html`, en español e inglés y con modo oscuro.
- En GitHub hay que ir a Settings → Pages → *Deploy from a branch*, rama `main`, carpeta `/docs`.
- URL resultante: `https://<usuario>.github.io/<repo>/privacy/`.
- La política tiene que coincidir con lo que la app hace de verdad. Por ejemplo, "extraer" y no "comprimir" si la app no comprime.
- Los términos son opcionales en Play. Conviene tenerlos si hay compras: deben decir que la app se ofrece "tal cual", que las propinas son voluntarias y que los reembolsos siguen las reglas de Google Play.
- Si el repositorio de la app es privado, hay que crear un repositorio público aparte solo para estas páginas.

---

## 4. Play Console, paso a paso

### 4.1 Ficha de Play Store
- **Idioma predeterminado:** es-419 (Latinoamérica), con traducción en-US. La app usa voseo ("Elegí", "podés").
- **Nombre:** como máximo 30 caracteres.
- **Descripción breve:** como máximo 80 caracteres; hay que **contarlos**, porque la primera versión se pasaba.
- **Descripción completa:** secciones en mayúsculas con viñetas; en es-419 se escribe "video", sin tilde.
- **Categoría y etiquetas:** hasta 5 etiquetas, que se eligen de una lista; no se escriben a mano.
- **Vídeo de la ficha:** opcional. Conviene dejarlo vacío si solo están los vídeos de las declaraciones, porque tapan la primera captura.

### 4.2 Contenido de la app (Políticas)

| Formulario | Respuesta en QubitExplorer |
|---|---|
| Política de privacidad | URL de GitHub Pages |
| Anuncios | No |
| Acceso a la app (datos de inicio de sesión) | Todo disponible sin restricciones |
| Clasificación de contenido | Categoría utilidad; No a todo salvo "compras digitales: Sí" desde que hay propinas; cajas de botín, criptomonedas y navegador: No |
| Público objetivo | **Solo 18+**. Si se marca menores, se activa la política de Familias y aparecen más preguntas |
| Seguridad de los datos | "¿Recoge o comparte datos?" → **No**. Lo que se procesa solo en el teléfono no cuenta, y los pagos los procesa Google |
| ID de publicidad | No, salvo que haya un SDK de anuncios |
| Salud, finanzas, gobierno, noticias | No |
| Estado de comerciante (UE) | **Sí** si la app genera ingresos; Play muestra tus datos de contacto en la UE |
| Términos del servicio | URL de los términos (opcional) |

### 4.3 Permisos sensibles: se declaran después de subir el AAB
**Acceso a todos los archivos (`MANAGE_EXTERNAL_STORAGE`)**
- Uso: **Funciones principales**.
- Descripción de la función, en inglés y como máximo 500 caracteres: qué hace el gestor de archivos y con qué tipos de archivos trabaja.
- Motivo técnico, como máximo 500 caracteres: MediaStore solo da acceso a archivos multimedia, y el Storage Access Framework no deja acceder a la raíz ni a la carpeta Download en Android 11 y posteriores.
- Vídeo: el flujo de pedir el permiso, de menos de 30 s.

**Servicio en primer plano (`FOREGROUND_SERVICE_DATA_SYNC`)**
- Opción: **Procesamiento de red → Otro**; las copias de seguridad son otra categoría.
- Solo pide el enlace del vídeo: una transferencia con progreso en la notificación.

### 4.4 Versiones y pruebas
- **Nombre de la versión:** `versionCode (versionName)`, por ejemplo `3 (1.1.1)`. Solo lo ve el desarrollador.
- **Notas de la versión,** con una etiqueta por idioma:
  ```
  <es-419>
  ...
  </es-419>
  <en-US>
  ...
  </en-US>
  ```
- **Cada AAB subido necesita un versionCode nuevo,** aunque se haya subido solo para que Play lo analice.
- **Cuenta personal nueva:** hace falta una **prueba cerrada con 12 testers o más durante 14 días** antes de poder pedir el acceso a producción.
- **Avisos que salen al subir:** borde a borde, APIs obsoletas y porcentaje de R8 son recomendaciones, no bloquean. El aviso de APIs obsoletas suele venir de las clases de AndroidX, ya ofuscadas.

---

## 5. Monetización sin cobrar la app: propinas
- **Lo que Play permite:** pagos dentro de la app **con Google Play Billing**. Un botón que lleve a PayPal, Ko-fi o Cafecito para pagar al desarrollador incumple la política de pagos de Play; la excepción son las organizaciones benéficas. Esos enlaces sí se pueden poner en el README de GitHub o en la web.
- **Implementación en QubitExplorer:**
  - `services/TipJar.kt`: Billing 8.0.0 y productos consumibles `tip_small`, `tip_medium` y `tip_large`, que se consumen después de comprarlos.
  - `ui/screens/SupportSection.kt`: la primera sección de Ajustes, con 3 botones (☕ US$ 1, 🍕 US$ 3, 🍽️ US$ 5) que muestran el precio local cuando Play responde.
  - Dependencias `com.android.billingclient:billing-ktx:8.0.0` y `androidx.fragment:fragment-ktx:1.8.6`.
  - Las propinas no desbloquean nada.
- **En Play Console:**
  1. Configurar el **perfil de pagos** (cuenta de comerciante).
  2. Subir un AAB que tenga el permiso `com.android.vending.BILLING`. Sin eso, Play no deja crear productos.
  3. **Monetizar con Play → Productos → Productos únicos → Crear:**
     - ID igual al del código, que no se puede cambiar;
     - nombre y descripción de como máximo 200 caracteres, con traducción en-US;
     - categoría fiscal "Ventas de apps digitales";
     - clasificación 18+;
     - sin restricción de países;
     - opción de compra con ID `default`, tipo **Comprar** y compatible con versiones anteriores;
     - estado **Activa**.
  4. **Pruebas de licencias** (en Gestionar cuenta): añadir el Gmail del desarrollador para probar sin que se cobre.
  5. Probar instalando la app **desde Play** (prueba cerrada o interna). Si se instala desde Android Studio, la facturación no funciona.
- **Tarifa:** Google se queda el 15 % del primer millón de USD al año.

---

## 6. Checklist rápido para la próxima app
- [ ] Elegir el paquete definitivo.
- [ ] Crear la clave de subida en `E:\Android\Key\`, junto con `keystore.properties` y las entradas de `.gitignore`.
- [ ] Configurar en Gradle la firma, `minify`, `shrinkResources` y `optimizedResourceShrinking`.
- [ ] Llamar a `enableEdgeToEdge()` y quitar los colores de barra de los temas.
- [ ] Revisar los permisos sensibles del manifiesto: cada uno necesita una declaración y un vídeo.
- [ ] Generar el AAB firmado y verificarlo con `jarsigner`.
- [ ] Preparar el icono de 512, el gráfico de 1024×500 y las capturas 1080×1920 sin alfa.
- [ ] Publicar la política de privacidad (y, si hace falta, los términos) en GitHub Pages.
- [ ] Escribir los textos de la ficha en es-419 y en-US, contando los caracteres.
- [ ] Rellenar los formularios de Contenido de la app (sección 4.2).
- [ ] Crear la prueba cerrada, subir el AAB y aceptar Play App Signing.
- [ ] Declarar los permisos sensibles con sus vídeos de YouTube "No listado".
- [ ] Conseguir 12 testers durante 14 días y solicitar el acceso a producción.
- [ ] Opcional: propinas con Billing (sección 5).
