-- Catálogos iniciales para selección relacional; las etiquetas de categoría se traducen en la UI.
INSERT INTO categoria (codigo, orden) VALUES
    ('WEB', 1), ('MOVIL', 2), ('ESCRITORIO', 3), ('DATOS', 4),
    ('JUEGOS', 5), ('IOT', 6), ('OTRO', 7)
ON CONFLICT DO NOTHING;

INSERT INTO tecnologia (nombre) VALUES
    ('Java'), ('Spring Boot'), ('PostgreSQL'), ('Thymeleaf'), ('Bootstrap'), ('jQuery'),
    ('HTML'), ('CSS'), ('JavaScript'), ('TypeScript'), ('Python'), ('Flask'),
    ('Django'), ('FastAPI'), ('MySQL'), ('SQLite'), ('MongoDB'), ('Redis'),
    ('Kotlin'), ('Android'), ('Flutter'), ('Dart'), ('C#'), ('.NET'),
    ('JavaFX'), ('Electron'), ('React'), ('Vue'), ('Angular'), ('Node.js'),
    ('Unity'), ('Godot'), ('Arduino'), ('ESP32'), ('Pandas'), ('R')
ON CONFLICT DO NOTHING;
