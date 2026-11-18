FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN --mount=type=cache,target=/root/.m2 \
    sed -i 's/\r$//' mvnw \
    && find backend/src -type f -name '*.java' -exec sed -i 's/\r$//' {} + \
    && chmod +x mvnw && ./mvnw -B -ntp -pl backend -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 govener && useradd --uid 10001 --gid govener --no-create-home govener
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/backend/target/govener-backend-0.1.0-SNAPSHOT.jar /app/govener.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=5 CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/govener.jar"]
