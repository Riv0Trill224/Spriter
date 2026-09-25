# Nueva firma exclusiva de Spriter

Spriter (`com.riv0trill.spriter`) tendrá una identidad propia. La clave de RGDS
Dashboard deja de usarse. Conserva **spriter-release.jks** y sus contraseñas en
respaldo privado: todas las versiones futuras de GitHub deberán usar exactamente
esta misma clave. No guardes el archivo ni sus contraseñas en el repositorio.

## Crear la clave en tu PC (PowerShell)

En una carpeta privada donde quieras guardar la clave, ejecuta:

```powershell
keytool -genkeypair -alias spriter-release -keyalg RSA -keysize 3072 -validity 10000 -storetype JKS -keystore .\spriter-release.jks -dname "CN=Spriter, OU=Development, O=Riv0Trill224, C=MX"
```

Escribe una contraseña nueva y sólida para el almacén. Cuando keytool pida la
contraseña de `spriter-release`, pulsa Enter para usar la misma. Anótala fuera
del repositorio. No uses el keystore ni las contraseñas anteriores de RGDS.

Comprueba que se creó una sola entrada `PrivateKeyEntry`, alias `spriter-release`,
y anota el **SHA256** del certificado (solo este dato es público):

```powershell
keytool -list -v -keystore .\spriter-release.jks
```

Crea el texto Base64 a partir del archivo nuevo:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\spriter-release.jks).Path)) | Set-Content -Encoding ascii -NoNewline .\spriter-release-base64.txt
```

## GitHub Actions

En **Spriter → Settings → Secrets and variables → Actions → Secrets** crea estos
tres repository secrets:

| Nombre | Contenido |
|---|---|
| `SPRITER_KEYSTORE_BASE64` | Todo el contenido de `spriter-release-base64.txt` |
| `SPRITER_KEYSTORE_PASSWORD` | La contraseña nueva del almacén |
| `SPRITER_KEY_PASSWORD` | La misma contraseña, si pulsaste Enter al crear la clave |

En la pestaña **Variables** crea `SPRITER_CERT_SHA256` con el SHA256 obtenido
por keytool (con o sin dos puntos). El alias `spriter-release` ya está fijado en
el proyecto. Los viejos secrets `RGDS_*` no se usan: puedes eliminarlos de
Spriter una vez publicado el primer release.

Ejecuta **Actions → Publicar release firmado → Run workflow**, rama `main`.
El workflow restaura el keystore solo en el directorio temporal del runner,
compila, verifica la firma real del APK y compara el SHA256 del certificado con
la variable, y publica `v1.0.0` solo cuando todos los pasos pasan. Lo elimina
al terminar. No genera claves en Actions.

## Instalación y futuras versiones

El APK debug anterior tiene otra firma. Para instalar el primer release estable,
puede ser necesario desinstalar el debug: se pierden sus ajustes y fondo guardado,
pero no las carpetas externas de sprites. Después instala `Spriter.apk` desde
Releases. Mantén el mismo `spriter-release.jks` para todas las versiones siguientes.

Cada versión posterior incrementará `versionCode` y `versionName` en
`app/build.gradle` y publicará un tag nuevo. El actualizador solo acepta un
APK de versión superior con package y firma iguales a los de la instalación;
verifica además tamaño y SHA256 de la descarga. Android pide confirmación para
instalar. Si el repositorio sigue privado, el usuario configura en la app un
token de lectura limitado a Spriter; nunca se incluye un token en el APK.
