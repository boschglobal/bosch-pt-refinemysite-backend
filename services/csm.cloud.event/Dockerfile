FROM ptcsmacr.azurecr.io/alpine:3 AS build-env
RUN mkdir /datadog && \
    wget -O /datadog/dd-java-agent.jar https://dtdg.co/latest-java-tracer && \
    apk update && apk upgrade --no-cache

FROM gcr.io/distroless/java21-debian12
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY --from=build-env /datadog /datadog
#User nonroot (ID 65532) is a user created by distroless images
#See https://github.com/GoogleContainerTools/distroless/issues/235
USER 65532
EXPOSE 8080
CMD ["/app.jar"]