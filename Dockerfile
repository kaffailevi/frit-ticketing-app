# One Dockerfile for every service. From the repository root:
#   docker build --build-arg SERVICE=ticketing-service    --build-arg PORT=8085 -t ticketing-service .
#   docker build --build-arg SERVICE=notification-service --build-arg PORT=8086 -t notification-service .
#   docker build --build-arg SERVICE=api-gateway          --build-arg PORT=8080 -t api-gateway .
# The context is the root because the services share the Gradle wrapper, the settings and the version catalog.

# ---- build: a JDK and Gradle, thrown away at the end -------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
RUN test -n "$SERVICE" || { echo "Pass --build-arg SERVICE=<module folder>"; exit 1; }
# A checkout on Windows may give gradlew CRLF line endings; Linux would then not find "sh\r".
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# Gradle's downloads stay in a build cache between builds, not in a layer of the image.
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew ":${SERVICE}:bootJar" --no-daemon --quiet
# Split the jar into layers: libraries change rarely, our classes on every commit.
RUN java -Djarmode=tools -jar "${SERVICE}/build/libs/${SERVICE}.jar" extract --layers --launcher --destination /extracted

# ---- run: only a JRE, the layers, and curl for the health check ---------------------------------------------
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 spring
WORKDIR /app
# Least often changed first: a new commit only replaces the last, small layer.
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
# Never root: whoever breaks in gets a user that owns nothing.
USER spring

ARG PORT
RUN test -n "$PORT" || { echo "Pass --build-arg PORT=<the service's server.port>"; exit 1; }
ENV PORT=${PORT}
EXPOSE ${PORT}
# Running is not ready: Docker asks Spring Boot's readiness probe (Compose waits for it with service_healthy).
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT}/actuator/health/readiness" || exit 1
# The heap follows the container's memory limit: 75 % of it, the rest for threads, metaspace and buffers.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
