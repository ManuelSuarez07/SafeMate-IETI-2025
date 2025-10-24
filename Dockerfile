# Etapa 1: Construcción del proyecto con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final para ejecutar el JAR
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar savemate.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "savemate.jar"]
