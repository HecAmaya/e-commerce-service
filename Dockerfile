FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/e-commerce-service-1.0.0.jar app.jar
EXPOSE 8080
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
