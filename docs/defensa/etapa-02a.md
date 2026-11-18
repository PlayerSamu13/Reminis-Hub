# HTTPS local — cierre

Etapa autorizada y continuacion a registro confirmada por el estudiante. Verificacion automatica sobre el servidor local: catalogo ES/EN, Swagger y OpenAPI con HTTP 200 y validacion TLS; nombre incorrecto rechazado con curl 60. Evidencia target/https/e38c8117cee84b43927b377534ae1e94. El estudiante confirmo confianza en navegador.

Peticion: navegador verifica confianza, nombre y vigencia del certificado; establece TLS con Tomcat en 8443; Spring atiende la ruta y devuelve la respuesta cifrada. El perfil https obtiene ruta del PKCS12 y contrasena desde variables externas. El certificado publico admite localhost y 127.0.0.1, vence el 16/12/2026. La clave privada permanece fuera de Git.

Decision: certificado autofirmado para demostracion local y confianza explicita. Desactivar verificacion mantiene cifrado pero pierde autenticacion del servidor. El script usa --stderr de curl para evaluar correctamente errores esperados en Windows PowerShell 5.1.

Defensa corregida y recomprobada:
1. ¿Nombre incorrecto? Debe rechazarse aunque se pueda cifrar.
2. ¿HttpOnly frente a Secure? HttpOnly restringe lectura de cookie por JavaScript; Secure restringe envio a transporte seguro. Ninguno configura TLS del servidor.
3. ¿curl -k vuelve a HTTP? No; omite verificacion del certificado. El estudiante confirmo que puede seguir cifrado y que se pierde comprobacion de identidad.

Aporte NF-02, criterio 4. No acredita registro, JWT o roles. Estos requieren sus propias pruebas. Pendiente renovar el certificado para demostraciones posteriores a su caducidad.
