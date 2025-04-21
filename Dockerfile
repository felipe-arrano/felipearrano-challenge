# Dockerfile

# --- Stage 1: Build ---
# Usa una imagen base con JDK 21 y Gradle para construir el proyecto
# Puedes elegir una imagen base diferente si prefieres (e.g., Gradle oficial)
FROM eclipse-temurin:21-jdk-alpine AS builder

# Define el directorio de trabajo dentro de la imagen
WORKDIR /app

# Copia los archivos necesarios de Gradle (wrapper y build files)
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Descarga las dependencias (esto se cachea si los build files no cambian)
# Usamos --no-daemon para asegurar que no interfiera con el build en CI/CD
RUN ./gradlew dependencies --no-daemon

# Copia el resto del código fuente
COPY src ./src

# Construye el JAR ejecutable
RUN ./gradlew bootJar --no-daemon

# --- Stage 2: Runtime ---
# Usa una imagen base ligera con JRE 21 (Alpine es pequeña)
FROM eclipse-temurin:21-jre-alpine

# Directorio de trabajo
WORKDIR /app

# Puerto que expone la aplicación (de application.yml)
EXPOSE 8080

# Argumento para el JAR (podría usarse para pasar nombre de JAR si cambia)
ARG JAR_FILE=build/libs/*.jar

# Copia SOLAMENTE el JAR construido desde la etapa 'builder'
COPY --from=builder /app/${JAR_FILE} app.jar

# Define el usuario y grupo para correr la aplicación (más seguro que root)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]