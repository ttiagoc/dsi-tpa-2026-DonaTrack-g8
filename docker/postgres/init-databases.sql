-- Una base por servicio: cada microservicio tiene su propio esquema de datos y no
-- comparte entidades ni FKs con los otros (Entrega 3, requerimiento 1.1).
-- La base "donaciones" ya la crea el entrypoint de la imagen a partir de POSTGRES_DB.
CREATE DATABASE logistica;
CREATE DATABASE notificaciones;
