# Spriter · 0.2.0

Aplicación Android independiente para mostrar hasta cinco PNG rebotando en la
pantalla libre de una RG DS. Package: **`com.riv0trill.spriter`**.

## Primer uso en la consola

1. Instala `Spriter-0.2.0-debug.apk`. Es una compilación de pruebas.
2. Abre Spriter, selecciona una pantalla y pulsa **Iniciar Pokémon incluidos · 151**.
   La colección viene dentro del APK y funciona sin Internet. También hay una demo de colores.
3. Si aparece en la pantalla equivocada, pulsa **Cerrar**, elige otro ID e inicia de nuevo.
4. Crea una carpeta como `SpriterSprites` en la SD o almacenamiento interno.
5. Copia dentro tus archivos `1.png`, `2.png`, `3.png`… Se aceptan nombres distintos
   y huecos en la numeración. Cada PNG es una imagen completa; no se interpretan
   hojas con múltiples fotogramas.
6. Pulsa **Elegir carpeta de PNG**, abre esa carpeta y concede acceso.
7. Selecciona cantidad, velocidad, tamaño y frecuencia de cambio.
8. Pulsa **Iniciar con mi carpeta**.
9. En la otra pantalla, elige el juego desde Spriter y pulsa **Abrir app en otra
   pantalla**, o ábrelo desde el launcher de la consola.

La ROM y el juego pueden decidir reutilizar una ventana existente. Si el juego no
respeta la pantalla elegida, colócalo con los controles de pantalla de la RG DS.
No se supone que display 0 sea arriba ni que display 1 sea abajo.

Durante la animación, toca para mostrar **Pausar / Cambiar / Ocultar / Cerrar**.
Para aplicar nuevos ajustes o releer una carpeta modificada, cierra la animación
y vuelve a iniciarla. Para jugar a un título que necesita ambas pantallas, cierra
Spriter desde sus controles o desde **Detener sprites**.

## Funciones de esta versión

- Colección Pokémon 1–151 de [PokeAPI/sprites](https://github.com/PokeAPI/sprites), incluida sin conexión.

- Selector de carpeta con permiso persistente de lectura.
- PNG transparentes y normales; recorte automático del margen totalmente transparente.
- De 1 a 5 imágenes distintas a la vez; si hay menos, usa las disponibles.
- Bolsa aleatoria: recorre las imágenes elegibles antes de barajar otra vez.
  Las imágenes visibles y los archivos que no se pueden leer se excluyen.
- Cambio escalonado de un sprite cada 20, 40 o 60 segundos, o solo manual.
  Si todas las imágenes disponibles ya están visibles, permanecen en pantalla.
- Rebote con tiempo real transcurrido; 15 o 30 FPS de **animación de Spriter**.
- Hora, fecha, batería y nombre de **app elegida**.
- Demo integrada de figuras originales de colores; no requiere carpeta.
- Pantalla elegida por ID y animación en actividad independiente.
- Render suspendido cuando la actividad deja de estar visible o el display se apaga.
- Lectura y decodificación de archivos fuera del hilo de interfaz.
- Android 8.0 o posterior; objetivo principal de prueba: RG DS con Android 14.

## Alcance y límites

El texto **Elegida** significa la app seleccionada por el usuario. Esta versión no
afirma detectar automáticamente la app activa ni el título de una ROM dentro de
un emulador. Tampoco mide FPS del juego. Los 15/30 FPS son el límite de la animación.

Cada PNG se reduce a un máximo de 256 píxeles por lado para acotar memoria. Se
conserva su proporción; el escalado sin suavizado favorece pixel art. Se omiten PNG
ilegibles o totalmente transparentes. No se cargan subcarpetas. Límite: 10 000 PNG
por carpeta. Si desaparece la SD, las imágenes ya cargadas pueden permanecer;
para recuperar archivos omitidos hay que volver a iniciar el modo.

Spriter mantiene encendida su pantalla mientras está visible. La autonomía y el
impacto sobre el juego requieren medición física. Las funciones básicas no
requieren root, Google Play Services, conexión a Internet ni permisos globales de
almacenamiento. El APK no incluye permiso de Internet.

## Compilar en GitHub, sin Android Studio

1. Crea un repositorio nuevo llamado `Spriter` en tu cuenta; puede ser privado.
2. Descomprime el proyecto y sube **su contenido** a la raíz del repositorio.
   Deben quedar `app/`, `.github/`, `settings.gradle` y `build.gradle` en la raíz.
   Incluye `.github/workflows/android.yml` aunque tu explorador o selector oculte
   carpetas cuyo nombre empieza por punto.
3. Haz commit en `main`.
4. Abre **Actions → Compilar Spriter**. Se inicia al subir a `main`; también
   puedes ejecutarlo con **Run workflow**.
5. Cuando termine correctamente, descarga el artifact **Spriter-0.2.0-debug**.
6. Extrae el ZIP e instala el APK en la RG DS.

El workflow ejecuta pruebas de lógica, Android Lint y la compilación. Tiene permiso
de lectura del repositorio y entrega un artifact; no publica Releases.
Lee **[docs/FIRMA.md](docs/FIRMA.md)** antes de distribuir actualizaciones.

## Compilación local opcional

Requisitos: JDK 17, SDK 35, Build Tools 35.0.0 y Gradle 8.9.

```sh
python3 scripts/fetch_pokemon.py
gradle testDebugUnitTest lintDebug assembleDebug
```

Si se incluye Gradle Wrapper, también puedes usar `./gradlew` o `gradlew.bat`.
El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Archivos principales

| Archivo | Función |
|---|---|
| `MainActivity.java` | Configuración, carpeta, pantallas y lanzamiento de juegos |
| `StageActivity.java` | Actividad visible en el display elegido y controles |
| `SpriteView.java` | Animación, demo, carga asíncrona y franja de información |
| `SpriteFiles.java` | Enumeración y lectura de PNG mediante el selector Android |
| `core/ShuffleBag.java` | Aleatoriedad con exclusión de sprites visibles |
| `core/Motion.java` | Movimiento y rebote, independientes de Android |
| `CoreTest.java` | Pruebas de aleatoriedad, límites y velocidades |

## Contexto de RGDS Dashboard aplicado

Proyecto original, separado de `com.rgds.dashboard`. Se tomaron como referencia los
hallazgos documentados el 24/09/2026: Android 14/TrebleDroid, dos displays observados
de 640×480, elección explícita de display y compilación por GitHub Actions.

El foco en una pantalla no debe detener la actividad visible en la otra: se usa
`onStop` para suspender renderizado, no `onPause`. No se ejecutan sondeos de root ni
SurfaceFlinger durante la animación. La compatibilidad final con las dos pantallas
y el rendimiento se validan con **[docs/PRUEBA_RGDS.md](docs/PRUEBA_RGDS.md)**.

## Colección de PokeAPI

El flujo de Actions descarga durante la compilación los PNG frontales por defecto
`sprites/pokemon/1.png` a `151.png`, del commit fijado en
`scripts/pokemon.lock.json`. Verifica cada imagen contra su hash Git original.
Si falta una imagen o no coincide, la preparación falla y no publica un APK incompleto.

La consola no descarga imágenes: se incluyen en el APK. Puedes seguir eligiendo
una carpeta externa con Pokémon, Yu-Gi-Oh!, CTR u otros sprites propios.

Los archivos descargados y los avisos se generan bajo `app/src/main/assets/`
y se excluyen del repositorio. Ejecuta el script antes de compilar localmente.
El proyecto no depende del contenido futuro de la rama master de PokeAPI.

Origen y avisos: **[docs/POKEAPI.md](docs/POKEAPI.md)**.
