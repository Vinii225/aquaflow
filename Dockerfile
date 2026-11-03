# Estágio 1: compila o projeto (os testes rodam no CI, não aqui)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Estágio 2: roda só o .jar, com usuário sem privilégios
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S aquaflow && adduser -S aquaflow -G aquaflow
COPY --from=build /app/target/*.jar app.jar
USER aquaflow

# Respeita o limite de memória do container (o plano free do Render tem 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
