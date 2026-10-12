# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 2G 内存机器上限制 Maven 堆，避免构建期 OOM
ARG MAVEN_BUILD_MEMORY=768m
ENV MAVEN_OPTS="-Xmx${MAVEN_BUILD_MEMORY}"
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar /app/blog-server.jar
EXPOSE 8080
# 运行时堆由 JAVA_TOOL_OPTIONS 控制（见 docker-compose.prod.yml）
ENTRYPOINT ["java", "-jar", "/app/blog-server.jar"]
