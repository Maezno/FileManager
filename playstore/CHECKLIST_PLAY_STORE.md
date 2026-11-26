# QubitExplorer — Checklist para Google Play (2026-11-26)

## Capturas (`playstore/screenshots/phone/`)
Requisitos de Play para teléfono: PNG/JPEG sin alfa, 2–8 capturas, lados entre 320 y 3840 px, lado largo ≤ 2× el corto (para destacar: 1080×1920 o mayor, 9:16).
Tomadas en el emulador Medium_Phone_API_37 forzado a **1080×1920** (el 1080×2400 nativo es 20:9 y supera la proporción 2:1), barra de estado en modo demo (10:00, batería llena, sin notificaciones), app en inglés.

| Archivo | Pantalla |
|---|---|
| 01_inicio.png | Explorador, tema claro, lista |
| 02_archivos_grandes.png | Archivos grandes (tras escanear) |
| 03_red_lan.png | Red local SMB/FTP |
| 04_ajustes.png | Ajustes |
| 05_oscuro_cuadricula.png | Explorador, tema oscuro, cuadrícula |

Las 5 están verificadas: 1080×1920, RGB sin canal alfa.

Detalles de UI vistos al hacer las capturas: en Archivos grandes el resumen dice "3 cameras" aunque no hay fotos de cámara, y solo el .zip muestra chip de categoría (los vídeos no). La barra de estado tiene un icono de escudo fijo del emulador.

Para la versión en español de la ficha conviene repetirlas con el emulador en `es-ES`.

## En regla
- `applicationId` `com.maez.qubitexplorer`, versionCode 1 / versionName 1.0.0.
- targetSdk 36 / compileSdk 36 (cumple el requisito vigente de Play), minSdk 26.
- Release con R8 (`minify` + `shrinkResources`) y reglas ProGuard para SMB/FTP/compresión.
- `usesCleartextTraffic=false`, `allowBackup=false`, reglas de extracción de datos definidas.
- Servicio en primer plano con tipo `dataSync` y su permiso declarado; `POST_NOTIFICATIONS` declarado.
- Icono de ficha 512×512 PNG (`Resources/app_icon_play_store.png`).
- Lint release: **0 errores**, 83 avisos (tipografía, KTX, versiones de dependencias; ninguno bloqueante).
- Textos en `strings.xml` con traducción `values-en`.

## Bloqueantes antes de subir
1. **Firma de release**: no hay `signingConfigs` ni keystore. Crear la clave de subida (Android Studio → Generate Signed Bundle) guardándola fuera del repo, y activar Play App Signing.
2. **Generar AAB** (`bundleRelease`), no APK. Borrar `FileManagerStudio_v1.0.apk` del repo (nombre antiguo).
3. **Política de privacidad pública (URL)**: Play la exige porque se pide `MANAGE_EXTERNAL_STORAGE`. Hoy solo existe el diálogo dentro de Ajustes; publicar ese texto en una web y enlazarlo en la ficha (y opcionalmente desde Ajustes).
4. **Declaración de "Acceso a todos los archivos"** en Play Console (Contenido de la app → Permisos): categoría *Gestor de archivos*, con vídeo corto que muestre el flujo de concesión del permiso. Es el punto que más rechazos genera.
5. **Gráfico de funciones 1024×500** (obligatorio) — no existe aún.
6. **Formularios de Play Console**: Seguridad de los datos (no recopila ni comparte; credenciales cifradas localmente), clasificación de contenido, público objetivo, declaración de servicio en primer plano `dataSync` (vídeo/explicación de transferencias).
7. Revisar que los **P0 de `REVISION_PRODUCCION.md`** (pérdida de datos al copiar/mover/renombrar/borrar) estén resueltos; solo se confirmó la guarda de origen = destino en `VfsManager.kt:783`.

## Recomendado
- Probar el AAB release en un dispositivo (R8 puede romper SMB/FTP pese a las reglas).
- Atender `ObsoleteSdkInt` (`mipmap-anydpi-v26` → `mipmap-anydpi`) y actualizar dependencias marcadas por lint.
- Ficha: título ≤ 30 car., descripción breve ≤ 80, completa ≤ 4000, en es y en.
