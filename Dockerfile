# Stage 1: build the JAR (needs JDK, Maven and your source code)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
# npm.install.command=ci: a clean, byte-exact install from the lockfile. The
# default (`install`) exists for local builds, where keeping node_modules in
# place matters more.
RUN mvn package -DskipTests -Dnpm.install.command=ci

# Stage 2: run the JAR (JRE is enough)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
