FROM maven:3.9.12-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY . .

ARG MODULE

RUN mvn -B -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

ARG MODULE

COPY --from=build /workspace/${MODULE}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
