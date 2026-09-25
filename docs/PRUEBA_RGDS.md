# Prueba física · 1.0.0

1. Instalar en RG DS y comprobar que solo se ve ⋮ como control permanente.
2. Mover Spriter a cada display; abrir un juego en el otro. La animación debe continuar al perder foco.
3. Observar durante dos minutos: cantidad variable, en ocasiones más de cinco, sin el mismo archivo simultáneo.
4. Probar cada colección y una carpeta con pocos PNG, archivos transparentes, dañados y huecos de numeración.
5. Ajustar velocidad durante la animación, comprobar rebote al redimensionar/rotar.
6. Probar brillo en la pantalla de Spriter y confirmar qué hace el firmware con la otra pantalla. Restaurar brillo del sistema.
7. Elegir un fondo, variar opacidad 0/100, cerrar/abrir y comprobar persistencia; quitar fondo.
8. Ocultar/mostrar reloj; comprobar hora, fecha y batería.
9. Abrir ambos enlaces de Acerca de y verificar el toast de inicio.
10. Apagar display o enviar al fondo: no debe seguir renderizando. Reabrir sin saltos grandes.
11. Probar updates sin red, sin acceso al repo privado y con token válido. No debe bloquear la animación.
12. Desde el primer release estable, probar un release posterior firmado con la misma clave: descarga, permiso de instalación, confirmación Android y ajustes preservados.
13. Verificar que rechaza APK con hash, versión o certificado distinto. La lógica de URLs/metadata también tiene pruebas unitarias.

Registrar versión del firmware, IDs de pantallas, brillo, consumo/temperatura y errores.
No se afirma identificar la ROM ni la aplicación activa del emulador automáticamente.
