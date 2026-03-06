# Multi-stage build for Spring Boot application (RECOMMENDED)
FROM eclipse-temurin:17-jdk-focal AS build

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]

# ============================================
# Alternative: Single-stage build (simpler but larger image)
# ============================================

# FROM eclipse-temurin:17-jdk-focal
# 
# WORKDIR /app
# 
# # Install Maven
# RUN apt-get update && apt-get install -y maven
# 
# # Copy pom.xml and download dependencies
# COPY pom.xml .
# RUN mvn dependency:go-offline -B
# 
# # Copy source code and build
# COPY src ./src
# RUN mvn clean package -DskipTests
# 
# # Expose port
# EXPOSE 8080
# 
# # Run the application
# CMD ["java", "-jar", "target/*.jar"]