# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy the pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy the source code (using standard Maven directory structure)
COPY src ./src

# Package the application using Maven (which handles dependencies correctly)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file from the builder stage
# The maven-shade-plugin creates an uber-jar with all dependencies included
COPY --from=builder /app/target/lp1-persistence-1.0-SNAPSHOT.jar ./app.jar

# Expose the port the application runs on (as defined in LP1 and its compose file)
EXPOSE 8081

# Command to run the JAR - the Main class is already specified in the manifest
CMD ["java", "-jar", "./app.jar"] 