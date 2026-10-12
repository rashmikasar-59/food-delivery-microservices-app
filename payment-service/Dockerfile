FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
# Copy sibling module POMs because the root project is a Maven reactor.
COPY . .
RUN mvn -pl payment-service -am -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/payment-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
