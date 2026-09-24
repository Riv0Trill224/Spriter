# Colección Pokémon integrada

- Repositorio: https://github.com/PokeAPI/sprites
- Commit: `a13b1f4ccd77f35fd1370d2db5f0051221e9683f`.
- Carpeta: `sprites/pokemon`.
- Selección inicial: `1.png` a `151.png`, sprites frontales por defecto.
- Los nombres numéricos corresponden a sus identificadores de Pokémon.
- Se descargan en el entorno de compilación y se empaquetan en el APK.
- Cada archivo se verifica contra el SHA de su objeto Git en el commit citado.
- Son PNG estáticos: Spriter anima su desplazamiento y rebote.

El archivo original `LICENCE.txt` del repositorio indica que las imágenes tienen
copyright de The Pokémon Company y declara CC0 1.0 para el repositorio. Se conserva
su texto completo en `POKEAPI-LICENCE.txt` y también dentro del APK. Spriter no se
presenta como producto oficial de Pokémon, Nintendo ni PokeAPI.

Para actualizar o ampliar la colección, revisar el commit de origen, generar
nuevos hashes en `scripts/pokemon.lock.json` y ajustar la cantidad esperada en el
script, la comprobación de Gradle y la etiqueta del botón. No se sigue master
automáticamente durante las compilaciones.
