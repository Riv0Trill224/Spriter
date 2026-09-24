# Primera prueba en RG DS

Estado: el APK debe probarse físicamente; una compilación correcta no certifica el
comportamiento de los dos displays internos de TrebleDroid.

1. Abre la demo y comprueba visualmente qué pantalla corresponde a cada ID.
2. Deja Spriter en la pantalla libre y abre un juego de una pantalla en la otra.
3. Juega durante un minuto: los sprites deben seguir moviéndose al cambiar el foco.
4. Toca Spriter: verifica que el juego siga visible. Prueba Pausar, Cambiar y Cerrar.
5. Concede acceso a una carpeta con 8 PNG numerados; elige 5 sprites y cambio cada
   20 segundos. No debe haber dos copias del mismo archivo visibles a la vez.
6. Prueba una carpeta con solo 2 PNG: deben verse 2 sprites, sin duplicados.
7. Prueba una carpeta vacía y un archivo que no sea PNG renombrado como `.png`:
   debe aparecer un aviso o contabilizarse como omitido; la app no debe cerrarse.
8. Cierra y reabre Spriter: debe recordar la carpeta, la cantidad y los ajustes.
9. Apaga/enciende la pantalla o cierra/abre la consola. Comprueba recuperación.
10. Retira la SD cuando sea seguro para el dispositivo y vuelve a abrir el modo:
    debe avisar que falta la carpeta. Vuelve a insertarla y selecciónala otra vez.
11. Compara el mismo juego con Spriter cerrado, con 1 sprite a 15 FPS y con 5 a
    30 FPS. Registra si hay diferencia perceptible antes de aumentar efectos.

Si falla, envía una foto del mensaje y estos datos: ID elegido, pantalla real,
versión Android/ROM, juego usado y paso exacto. No se necesita ADB para esta prueba.

Pendientes posteriores: detección de app por display, integraciones específicas
para títulos de ROM, wallpaper opcional y perfiles por emulador. La implementación
actual muestra siempre la etiqueta veraz «Elegida».
