-- Datos ficticios autorizados para la demostración local. No acreditan proyectos,
-- consentimientos ni evaluaciones reales. La cuenta inactiva no permite iniciar sesión.
INSERT INTO usuario (email, nombre, password_hash, activo)
VALUES ('demo@reminishub.invalid', 'Cuenta ficticia de demostración', NULL, false);

INSERT INTO proyecto (nombre, resumen, descripcion, url_repositorio, anio, semestre,
                      asignatura, categoria_id, estado, cargado_por,
                      terminos_aceptados_en, terminos_version, es_ficticio)
SELECT d.nombre, d.resumen,
       d.descripcion || E'\n\nEste registro es completamente ficticio. Ilustra una posible idea de proyecto; no representa una implementación ni una evaluación real.',
       'https://github.com/reminishub-demo/' || d.slug,
       d.anio, d.semestre, d.asignatura, c.id, 'PUBLICADO', u.id,
       TIMESTAMPTZ '2026-11-12 12:00:00+00', 'DEMO-SIN-CONSENTIMIENTO', true
FROM (VALUES
    ('Campus Verde: una huerta conectada',
     'Una propuesta para observar la humedad del suelo y explorar cómo los sensores pueden apoyar el cuidado de una huerta universitaria.',
     'La idea combina sensores y un panel de consulta para comparar mediciones de humedad. El diseño propuesto separa la lectura de dispositivos de la visualización, con el fin de estudiar consumo de agua y mantenimiento preventivo.',
     'campus-verde', 2026, 2, 'Internet de las cosas', 'IOT'),
    ('Aula Abierta: recursos para aprender juntos',
     'Un catálogo colaborativo de recursos educativos, organizado por temas y tecnologías para facilitar el aprendizaje entre estudiantes.',
     'El proyecto ilustrativo plantea un catálogo de materiales con revisión previa a su publicación. Permite estudiar búsqueda, clasificación y diseño de interfaces accesibles, sin recopilar calificaciones ni información privada de estudiantes.',
     'aula-abierta', 2026, 2, 'Programación Web', 'WEB'),
    ('Rutas del campus en tu bolsillo',
     'Una idea de aplicación móvil para consultar recorridos peatonales y localizar espacios de estudio dentro de un campus universitario.',
     'Esta propuesta explora una navegación móvil sencilla, con puntos de interés y recorridos descritos en texto. El alcance demostrativo enfatiza orientación accesible y almacenamiento local de información.',
     'rutas-campus', 2026, 1, 'Desarrollo móvil', 'MOVIL'),
    ('Datos que cuentan: exploración de bibliotecas',
     'Visualización de un conjunto sintético de préstamos para descubrir patrones de uso y practicar análisis exploratorio de datos.',
     'Se propone preparar y explorar datos sintéticos de circulación bibliográfica. El objetivo pedagógico es distinguir limpieza de datos, agregaciones y visualizaciones, evitando conclusiones sobre una biblioteca real.',
     'datos-biblioteca', 2026, 1, 'Análisis de datos', 'DATOS'),
    ('Bitácora de laboratorio',
     'Una herramienta de escritorio para organizar prácticas, notas y resultados de experimentos en un entorno de aprendizaje.',
     'La propuesta organiza registros de prácticas en una base local. Sirve para estudiar operaciones CRUD, validación y separación entre interfaz y persistencia. Los ejemplos no contienen resultados de experimentos reales.',
     'bitacora-laboratorio', 2025, 2, 'Programación orientada a objetos', 'ESCRITORIO'),
    ('Código Aventura: aprender jugando',
     'Un videojuego educativo que convierte conceptos básicos de programación en pequeños retos de lógica y exploración.',
     'La idea presenta niveles cortos sobre secuencias, decisiones y ciclos. El diseño busca practicar retroalimentación clara y progresión de dificultad; no se ha realizado una evaluación real de aprendizaje.',
     'codigo-aventura', 2025, 2, 'Desarrollo de videojuegos', 'JUEGOS'),
    ('Encuentro: agenda de grupos de estudio',
     'Una plataforma web para explorar encuentros académicos y organizar sesiones de estudio por temática y periodo.',
     'La propuesta se centra en la consulta de actividades y su clasificación por temática. Ofrece un caso de estudio para formularios, autorización y consultas paginadas, con datos de demostración.',
     'encuentro', 2025, 1, 'Ingeniería de Software', 'WEB'),
    ('Observatorio del aire',
     'Exploración de mediciones ambientales sintéticas mediante gráficas y resúmenes para comprender series de tiempo.',
     'Este ejemplo propone analizar series ambientales sintéticas con agregaciones por periodo. Su propósito es practicar visualización y explicar límites de los datos, sin emitir recomendaciones ambientales reales.',
     'observatorio-aire', 2024, 2, 'Estadística aplicada', 'DATOS')
) AS d(nombre, resumen, descripcion, slug, anio, semestre, asignatura, categoria)
JOIN categoria c ON c.codigo = d.categoria
CROSS JOIN usuario u
WHERE u.email = 'demo@reminishub.invalid';

INSERT INTO proyecto_autor (proyecto_id, nombre_publico, orden, medio_autorizacion)
SELECT id, 'Equipo ficticio ' || nombre, 1, 'DATO_FICTICIO'
FROM proyecto WHERE cargado_por = (SELECT id FROM usuario WHERE email = 'demo@reminishub.invalid')
    AND es_ficticio;

INSERT INTO proyecto_tecnologia (proyecto_id, tecnologia_id)
SELECT p.id, t.id FROM proyecto p
JOIN (VALUES
    ('campus-verde','ESP32'), ('campus-verde','Python'), ('campus-verde','PostgreSQL'),
    ('aula-abierta','Java'), ('aula-abierta','Spring Boot'), ('aula-abierta','Bootstrap'),
    ('rutas-campus','Kotlin'), ('rutas-campus','Android'), ('rutas-campus','SQLite'),
    ('datos-biblioteca','Python'), ('datos-biblioteca','Pandas'),
    ('bitacora-laboratorio','Java'), ('bitacora-laboratorio','JavaFX'), ('bitacora-laboratorio','SQLite'),
    ('codigo-aventura','Godot'), ('codigo-aventura','C#'),
    ('encuentro','Java'), ('encuentro','Thymeleaf'), ('encuentro','PostgreSQL'),
    ('observatorio-aire','R'), ('observatorio-aire','PostgreSQL')
) AS d(slug, tecnologia) ON p.url_repositorio = 'https://github.com/reminishub-demo/' || d.slug
JOIN tecnologia t ON t.nombre = d.tecnologia
WHERE p.es_ficticio;
