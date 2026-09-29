# syntax=docker/dockerfile:1.7
# Standard multi-stage image for every CommerceHub service.
# Build from the repository root: docker build -f docker/service.Dockerfile --build-arg SERVICE=product-service .

ARG BUILD_IMAGE=eclipse-temurin:21.0.12.1_1-jdk-noble
ARG RUNTIME_IMAGE=registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

FROM ${BUILD_IMAGE} AS build
ARG SERVICE
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/ services/

RUN --mount=type=cache,id=commercehub-m2,target=/root/.m2,sharing=locked \
    test -n "${SERVICE}" && test -d "services/${SERVICE}" \
    && ./mvnw -B -ntp -pl "services/${SERVICE}" -am -DskipTests package

FROM ${RUNTIME_IMAGE}
ARG SERVICE
ARG VERSION=0.1.0

LABEL org.opencontainers.image.title="commercehub-${SERVICE}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.description="CommerceHub ${SERVICE}"

ENV LANGUAGE="en_US:en" \
    TZ="UTC" \
    JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Duser.timezone=UTC -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    JAVA_APP_JAR="/deployments/quarkus-run.jar"

COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /workspace/services/${SERVICE}/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]
