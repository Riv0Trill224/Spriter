# Firma estable y releases

Spriter usa la misma identidad de firma que RGDS Dashboard. Package independiente:
`com.riv0trill.spriter`. Certificado SHA-256 esperado:

`1c09515ee923d5610dc41e85d75a49900c10158b18c4703463896e4fd1b10264`

Configura en **Spriter → Settings → Secrets and variables → Actions** los mismos
valores originales del otro proyecto:

- `RGDS_KEYSTORE_BASE64`
- `RGDS_KEYSTORE_PASSWORD`
- `RGDS_KEY_ALIAS`
- `RGDS_KEY_PASSWORD`

GitHub no permite recuperar el contenido de un secret ya guardado. Deben usarse
el keystore y las credenciales originales; no crear otra clave ni guardarlos en git.
El workflow restaura la clave en el directorio temporal del runner, compila,
comprueba el certificado con apksigner y la elimina al terminar. Si falta un secret
o la identidad no coincide, no publica ningún release.

Después, ejecuta **Actions → Publicar release firmado → Run workflow** sobre main.
Para versiones posteriores incrementa **versionCode** y **versionName** en
`app/build.gradle`; cada versión debe ser nueva. No se sobrescriben releases.
El primer release estable es **1.0.0**, versionCode **100**.

El actualizador solo acepta versiones superiores, el package exacto y la misma
firma que la instalación actual. Antes del instalador verifica el SHA-256 y tamaño
del APK frente a `update.json`, y revisa su certificado y versionCode.

Las compilaciones debug anteriores llevan otra firma. Para pasar al primer release
estable puede ser necesario desinstalar el debug (se pierden sus ajustes y fondo
copiado) e instalar manualmente el APK firmado. No borres tus carpetas de sprites.
A partir de la instalación estable, conserva siempre esta clave para actualizar.

El repositorio privado requiere un token de lectura configurado por el usuario en
la app. Nunca distribuyas un APK con un token incluido ni publiques el keystore.
La app no instala en silencio: usa el instalador normal de Android.
