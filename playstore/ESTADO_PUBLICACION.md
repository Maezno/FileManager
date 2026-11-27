# Estado de la publicación en Google Play — QubitExplorer

Última actualización: 26/11/2026. Textos y respuestas completos: `PLAY_CONSOLE.md`.

## Datos clave
- Paquete: `com.maez.qubitexplorer` · versionCode 1 · versionName 1.0.0
- AAB firmado: `app/build/outputs/bundle/release/app-release.aab`. Se generó en la carpeta de E:; si E: no vuelve, regenerarlo con `bundleRelease`.
- Clave de subida: `E:\Android\Key\qubitexplorer-upload.jks`, alias `upload`. Las contraseñas están en `keystore.properties`, que está fuera de git; ver `keystore.properties.example`. ¡Haz copia de seguridad de ambos!
- Política de privacidad: https://maezno.github.io/FileManager/privacy/
- Idioma predeterminado de la ficha: **es-419** (Latinoamérica). Traducción en **en-US**.
- Símbolos nativos: solo contienen la librería de Compose (`libandroidx.graphics.path.so`).
  - Son opcionales; sirven para quitar el aviso.
  - Se hacen comprimiendo `app/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/*` en un zip.

## Hecho en Play Console
- [x] App creada, idioma es-419
- [x] Versión de prueba en preparación:
  - nombre `1 (1.0.0)`;
  - notas de la versión en es-419 y en-US.
- [x] URL de la política de privacidad indicada
- [~] Seguridad de los datos: en el paso 2 ("Recogida de datos y seguridad")

## Pendiente (en orden)
1. **Público objetivo y contenido.**
   - Dejar solo **18+**.
   - Ahora tiene marcados menores y por eso pide la política de Familias.
2. **Seguridad de los datos.**
   - ¿Recoge/comparte datos? **No**.
   - Si sigue apareciendo la pregunta de Familias, responde **No**.
   - Revisar la vista previa y enviar.
3. **Resto de "Contenido de la app"** (respuestas en PLAY_CONSOLE.md §6):
   - anuncios: No;
   - acceso a la app: sin restricciones;
   - clasificación de contenido: utilidad, No a todo;
   - noticias, gobierno, finanzas y salud: No;
   - ID de publicidad: no se usa.
4. **Permiso "Acceso a todos los archivos"** (§3):
   - justificación;
   - vídeo `playstore/videos/permiso_todos_los_archivos.mp4`, subido a YouTube como "No listado". El vídeo estaba en E:.
5. **Servicio en primer plano dataSync** (§4):
   - vídeo `playstore/videos/transferencia_servicio_primer_plano.mp4`, subido a YouTube como "No listado". El vídeo estaba en E:.
6. **Ficha principal** (§2):
   - nombre;
   - descripción breve y completa (ES/EN);
   - icono `Resources/app_icon_play_store.png`;
   - gráfico `playstore/graphics/feature_graphic_1024x500.png`;
   - capturas `playstore/screenshots/phone/01…05.png`;
   - categoría Herramientas;
   - etiquetas: Gestor de archivos y similares, máximo 5.
7. **Prueba cerrada.**
   - Subir el AAB y aceptar la firma de apps de Google Play.
   - Añadir **≥12 testers** y mantenerlos **14 días**.
8. Tras los 14 días, **solicitar acceso a producción**.

No hacen falta términos de servicio.

## Pendiente en el código (no bloquea)
- Errores graves de pérdida de datos en `REVISION_PRODUCCION.md`: sin verificar del todo.
- Errores menores:
  - velocidad de subida FTP mostrada más baja de lo real;
  - "3 cámaras" en el resumen de archivos grandes;
  - el chip de categoría solo aparece en el .zip.
