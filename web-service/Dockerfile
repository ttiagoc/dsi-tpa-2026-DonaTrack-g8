# ==========================
# Etapa 1: Build de Maven
# ==========================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=web-service

# Copiar los archivos de configuración de Maven
COPY . .

# Compilar el proyecto (sin tests)
# -pl: Solo tu servicio
# -am: También sus dependencias (el pom padre)
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests


# ==========================
# Etapa 2: Imagen final (solo el JAR)
# ==========================
FROM eclipse-temurin:21-jre
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=web-service

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 7070
ENTRYPOINT ["java", "-jar", "app.jar"]
