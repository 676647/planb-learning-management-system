# Backend image (Spring Boot). Built by `docker compose up --build` – see DEPLOYMENT.md.

# Stage 1: build the jar with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first, in their own layer: they are only fetched again when pom.xml changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# The tests run in the CI pipeline, not in every image build
RUN mvn -q -B clean package -DskipTests

# Stage 2: run it with a small Java runtime image (no Maven, no source code)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Don't run as root inside the container
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/learning-management-system.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
