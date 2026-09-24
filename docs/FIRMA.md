# Firma del primer APK y actualizaciones

El APK inicial es **debug**, para probar el funcionamiento. Android exige que una
actualización del mismo package tenga el mismo certificado que la instalación
anterior. Cada entorno nuevo puede generar un debug keystore diferente.

Si instalas el APK entregado y después compilas desde GitHub con otra clave,
Android puede rechazar la actualización. Para cambiar de clave hay que
desinstalar la versión anterior (se borran ajustes y el permiso guardado de la
carpeta; tus PNG originales permanecen).

## Mantener una clave de pruebas entre ejecuciones de GitHub

El workflow admite el secret **`SPRITER_DEBUG_KEYSTORE_BASE64`**. Debe contener un
debug keystore válido codificado en Base64, con las credenciales estándar de
debug: alias `androiddebugkey`, contraseña de almacén y clave `android`.

Puedes crear uno en un equipo con JDK:

```sh
keytool -genkeypair -keystore spriter-debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Spriter Development"
```

En PowerShell, para copiar el Base64 al portapapeles:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\spriter-debug.keystore))) | Set-Clipboard
```

En el repositorio: **Settings → Secrets and variables → Actions → New repository
secret**. Usa el nombre exacto y pega el valor. Conserva una copia privada de la
clave. No subas el archivo ni el Base64 al repositorio.

Esto estabiliza la firma de pruebas de los builds posteriores a la configuración
del secret. No convierte en compatible un APK ya instalado con una clave distinta.
Para una distribución pública debe prepararse una firma de release propia y
aumentar `versionCode` en cada actualización.
