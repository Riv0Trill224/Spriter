# Verificación de Spriter 1.0.0

Fecha: 25/09/2026. Package `com.riv0trill.spriter`, versionCode 100.

- Compilación local de debug, release y AAB correcta (release local sin clave estable).
- 11 pruebas unitarias correctas en cada variante, sin fallos ni errores.
- Android Lint debug/release sin errores. Se añadió exclusión explícita de backups y transferencias para las credenciales locales.
- APK debug: firma de prueba verificada con apksigner.
- 305 PNG comprobados dentro del APK, byte a byte contra los assets de entrada.
- Los 154 PNG del ZIP personalizado se mantienen sin modificaciones en el repositorio.
- YAML de ambos workflows y sintaxis de los bloques bash validados.
- Workflow de publicación exige los cuatro secrets y comprueba el certificado estable RGDS antes de publicar.
- No se ha validado en hardware RG DS ni realizado una actualización instalada entre dos releases firmados.

El resultado definitivo de CI y de firma está en:
https://github.com/Riv0Trill224/Spriter/actions

Cada artifact/release incluye su propio `SHA256SUMS.txt`. El archivo histórico
`docs/SHA256SUMS.txt` pertenece exclusivamente al APK local 0.2.0 indicado en él.
