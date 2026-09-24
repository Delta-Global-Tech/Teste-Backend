# ---- Etapa de build ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia só o pom.xml primeiro para aproveitar cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Agora copia o restante do código e builda
COPY src src
RUN mvn clean package -DskipTests -B

# ---- Etapa final (runtime) ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]