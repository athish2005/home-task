# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the specific JAR file
COPY --from=build /app/target/task-0.0.1-SNAPSHOT.jar task-0.0.1-SNAPSHOT.jar

# Expose port (Render uses 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","task-0.0.1-SNAPSHOT.jar"]
