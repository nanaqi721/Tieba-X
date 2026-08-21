FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG MODULE
WORKDIR /workspace
COPY ../.. .
RUN mvn -B -pl "${MODULE}" -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy

ARG MODULE
WORKDIR /app
RUN groupadd --system --gid 10001 teibax \
    && useradd --system --uid 10001 --gid teibax --home-dir /app teibax \
    && mkdir -p /app/logs \
    && chown -R teibax:teibax /app
COPY --from=builder --chown=teibax:teibax /workspace/${MODULE}/target/${MODULE}-*.jar /app/app.jar

USER teibax
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
