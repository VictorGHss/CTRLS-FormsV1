# Stage 1 - Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# Stage 2 - Runtime
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /app/target/api-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=80.0", "-jar", "app.jar"]

