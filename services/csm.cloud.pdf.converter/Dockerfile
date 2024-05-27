FROM ptcsmacr.azurecr.io/alpine:3 AS build-env
RUN mkdir /datadog && \
    wget -O /datadog/dd-java-agent.jar https://dtdg.co/latest-java-tracer
RUN apk update && apk upgrade
# URL taken from here:
# https://github.com/microsoft/playwright-java/blob/main/scripts/download_driver_for_all_platforms.sh
ADD https://playwright.azureedge.net/builds/driver/playwright-1.40.0-linux.zip /tmp/playwright/playwright.zip
WORKDIR /tmp/playwright
RUN unzip playwright.zip && rm playwright.zip

FROM ptcsmacr.azurecr.io/eclipse-temurin:21-jre-jammy
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY --from=build-env /datadog /datadog
COPY --from=build-env /tmp/playwright /tmp/playwright

WORKDIR /tmp/playwright
RUN ./playwright.sh install-deps chromium \
     && ./playwright.sh install chromium \
     && rm -rf /var/lib/apt/lists/* \
     && mkdir -p /home/nobody/ms-playwright \
     && mv /root/.cache/ms-playwright/* /home/nobody/ms-playwright

ENV PLAYWRIGHT_BROWSERS_PATH /home/nobody/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD 1

# User nobody (ID 65534) is a non-root user provided by base image
USER 65534
EXPOSE 8080

WORKDIR "/"
CMD ["java", "-jar", "app.jar"]
