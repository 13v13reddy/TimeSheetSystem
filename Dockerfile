# Stage 1: Build the application using Maven
# We use a specific Maven image that includes JDK 17 to match our project's Java version.
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project file first. This layer is cached and only re-run if pom.xml changes.
COPY pom.xml .

# Download all the dependencies specified in pom.xml
RUN mvn dependency:go-offline

# Copy the rest of the application's source code
COPY src ./src

# Compile the application and package it into a single JAR file.
# We skip tests during the Docker build for speed; they should be run in a separate CI step.
RUN mvn clean package -DskipTests


# Stage 2: Create the final, lightweight production image
# We use a slim image with just the Java Runtime Environment, which is much smaller than the JDK/Maven image.
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the executable JAR file that was created in the 'build' stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port that the Spring Boot application runs on
EXPOSE 8080

# The command to run when the container starts
ENTRYPOINT ["java","-jar","app.jar"]
