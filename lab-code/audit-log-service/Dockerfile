FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY common-security/pom.xml common-security/pom.xml
COPY common-security/src common-security/src
RUN mvn -q -f common-security/pom.xml -DskipTests install

COPY audit-log-service/pom.xml audit-log-service/pom.xml
RUN mvn -q -f audit-log-service/pom.xml -DskipTests dependency:go-offline
COPY audit-log-service/src audit-log-service/src
RUN mvn -q -f audit-log-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/audit-log-service/target/audit-log-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
