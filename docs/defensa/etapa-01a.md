# E1A — Esquema y entidades

El estudiante autorizó continuar después de revisar las preguntas de comprensión. Correcciones registradas posteriormente, con permiso explícito, en el commit `8b02066`.

## Recorrido verificado

Al iniciar el contexto de pruebas, Flyway valida el historial y aplica V3; Hibernate comprueba la compatibilidad de tablas y columnas con las entidades. Aún no existía un flujo HTTP de negocio en E1A.

## Decisiones y alternativas

- Se recuperó la V2 original de Descargas: dos comentarios faltantes causaban el checksum distinto. No se usó repair, que habría ajustado el historial sin ejecutar cambios de esquema.
- V3 exige usuario_id cuando el medio es CUENTA_PROPIA. Validar solo en Java no protege escrituras desde otro cliente.
- El enum usa DATO_FICTICIO igual que SQL. EnumType.STRING persiste su nombre, por lo que un error de escritura rompe el contrato.
- es_ficticio queda fuera del INSERT/UPDATE de JPA. PostgreSQL asigna false; las semillas controladas identifican los ejemplos.
- El estudiante corrigió setRol y setActivo sin cambiar sus cuerpos.

## Evidencia

Compilación Java 25; dos pruebas de esquema con PostgreSQL; contexto Spring con Flyway y Hibernate; git diff --check: PASS. La prueba de autorización usa tabla temporal con CHECK e identidad, sin FK; no acredita el CRUD completo ni los triggers futuros.

## Defensa

1. ¿Por qué V3 y no editar V2? Para conservar el historial aplicado y reproducir el cambio.
2. ¿Por qué Java y SQL? Java explica errores; SQL garantiza integridad incluso fuera de la aplicación.
3. ¿Hibernate validate demuestra la regla de autorización? No. Verifica estructura; una prueba debe intentar el caso inválido y comprobar su rechazo. Tampoco existe aún una validación de esta regla en un servicio de escritura de Spring.

Rúbrica: contribuye a backend y datos (2, 3); la restricción protege integridad, pero no acredita por sí sola seguridad completa (4).
