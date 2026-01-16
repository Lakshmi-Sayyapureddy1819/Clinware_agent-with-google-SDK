# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build

# Copy project files
COPY pom.xml .
COPY src ./src
COPY tavily-server.js . 
# Build the JAR
RUN mvn clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# 1. INSTALL NODE.JS (Crucial Step!)
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean

# 2. Copy the JAR from the build stage
COPY --from=build /build/target/clinware-agent-1.0-SNAPSHOT.jar app.jar

# 3. Copy the Node.js script (Crucial Step!)
COPY tavily-server.js /app/tavily-server.js

# 4. Set permissions and run
EXPOSE 7000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]