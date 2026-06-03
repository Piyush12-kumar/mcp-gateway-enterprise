FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY backend-spring/pom.xml .
RUN mvn dependency:go-offline
COPY backend-spring .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

