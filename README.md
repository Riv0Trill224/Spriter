# Spriter · 1.0.0

Salvapantallas Android para la pantalla libre de la RG DS. Proyecto independiente,
package **com.riv0trill.spriter**. Se abre directamente en la animación; el único
control permanente es **⋮**, arriba a la derecha.

## Incluido

- **305 imágenes** sin conexión: 151 Pokémon de PokeAPI y 154 PNG del pack personalizado v2.
- Cantidad automática y aleatoria según espacio y tamaño de sprites, sin límite fijo de cinco.
  La población se reconsidera cada 25–50 segundos de animación; cambia un personaje cada 12–30 segundos.
  Se evitan repeticiones simultáneas del mismo archivo y se excluyen archivos ilegibles.
- Selección de todas las imágenes, solo Pokémon, solo el pack personalizado o una carpeta externa.
- Rebote estilo DVD, velocidad ajustable, escalado de pixel art y renderizado de hasta 30 FPS.
- Brillo de la ventana, fondo desde el explorador y opacidad del fondo; cambios en vivo.
- Reloj, fecha y batería opcionales. Toast **Spriter · GitHub** al abrir.
- Selector de pantalla por ID y opción para abrir un juego en otro display.
- Acerca de con enlaces al proyecto y a PokeAPI/sprites.
- Actualizador de GitHub con verificación de versión, tamaño, SHA-256, package y firma instalada.

## Uso

Instala el APK firmado desde [Releases](https://github.com/Riv0Trill224/Spriter/releases)
cuando el workflow de publicación haya terminado. Para probar antes, descarga el
artifact de **Actions → Compilar Spriter**; ese APK debug no usa la firma estable.

1. Abre Spriter. Si hace falta, usa **⋮ → Pantalla** para moverlo a la pantalla libre.
2. Usa **⋮ → Abrir juego** o el launcher de la consola para abrir el emulador en la otra pantalla.
3. Personaliza **Velocidad**, **Brillo**, **Fondo personalizado** y **Opacidad del fondo**.
4. Para usar una carpeta propia: **⋮ → Colección de sprites → Elegir carpeta externa**.
   PNG numerados o con otros nombres, sin subcarpetas. Cada PNG es una imagen completa.
5. Para un juego que ocupa ambas pantallas, usa **⋮ → Cerrar**.

Los IDs no presuponen pantalla superior/inferior. La ROM o el juego pueden decidir
reutilizar una ventana; en ese caso usa los controles de pantalla de la consola.
La animación se suspende cuando deja de estar visible, no al perder el foco.
La compatibilidad física y el impacto sobre autonomía se verifican en la RG DS.

No se identifica automáticamente la app activa ni la ROM ejecutada dentro de un emulador.
El brillo afecta a la ventana de Spriter; el firmware determina cómo aplica el brillo a cada display.
Los fondos se copian al almacenamiento privado de la app; los PNG externos usan permiso persistente de lectura.

## Actualizaciones y firma

Ver [docs/FIRMA.md](docs/FIRMA.md). El repositorio sigue privado. Para consultar sus releases
introduce un token de GitHub limitado a este repositorio y **Contents: Read-only** en
**⋮ → Acerca de → Acceso a updates**. El token se guarda cifrado con AndroidKeyStore y
solo se envía a la API de GitHub para este repositorio; no se incorpora al APK.
Si el repositorio se hace público más adelante, no será necesario usar token.

La app comprueba al abrir, como máximo una vez cada 24 horas, y avisa si hay una versión
nueva. **⋮ → Buscar actualizaciones** permite comprobar y descargar manualmente.
Android pide autorización para instalar desde Spriter y confirma la instalación.

Esta distribución es para GitHub. La futura versión de Play Store requerirá su propio
canal de actualización y una revisión de los requisitos vigentes y de los derechos de los assets.
El AAB producido por Actions es un artefacto de compilación, no una publicación en Play.

## Compilación

JDK 17, SDK 35, Build Tools 35.0.0, Gradle 8.9; Android 8.0 o posterior.

```sh
python3 scripts/fetch_pokemon.py
gradle testDebugUnitTest lintDebug assembleDebug
```

**Compilar Spriter** se ejecuta con cada push/PR y entrega un APK debug.
**Publicar release firmado** se ejecuta en main al cambiar la versión o su workflow,
o manualmente. Solo publica si están los cuatro secrets y el certificado coincide
con RGDS Dashboard. Cada release contiene `Spriter.apk`, `update.json` y `SHA256SUMS.txt`.

Pokémon se obtiene en build desde un commit fijado, comprobando los hashes Git de
cada imagen; la consola no necesita descargar sprites. Los PNG personalizados
están versionados en `app/src/main/assets/custom/`. Los fondos y sprites externos
se decodifican fuera del hilo de UI, con tamaño acotado.

Pruebas físicas: [docs/PRUEBA_RGDS.md](docs/PRUEBA_RGDS.md).
Créditos Pokémon y licencia de origen: [docs/POKEAPI.md](docs/POKEAPI.md).
Inventario del pack recibido: [docs/CUSTOM_SPRITES.txt](docs/CUSTOM_SPRITES.txt).
