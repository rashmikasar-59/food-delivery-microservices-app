FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY discovery-server/pom.xml ./discovery-server/pom.xml
# Copy sibling module POMs because the root project is a Maven reactor.
COPY . .
RUN mvn -pl discovery-server -am -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/discovery-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
