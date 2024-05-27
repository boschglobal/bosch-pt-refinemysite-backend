FROM ptcsmacr.azurecr.io/golang:1.21.6-alpine3.18 AS build-env

RUN apk add build-base
# Install librdkafka package from alpine edge repo
RUN sed -i -e 's/v3\.18/edge/g' /etc/apk/repositories \
    && apk upgrade --update-cache --available \
    && apk add --no-cache librdkafka-dev

COPY . /go/src/

# Using local .gomodcache folder to allow for reuse of all dependencies already downloaded with git authentication
ENV GOMODCACHE=/go/src/.gomodcache
WORKDIR /go/src

RUN CGO_ENABLED=1 go build -o /go/bin/app -tags dynamic

FROM ptcsmacr.azurecr.io/alpine:3.18

# Install librdkafka package from alpine edge repo
RUN sed -i -e 's/v3\.18/edge/g' /etc/apk/repositories \
    && apk upgrade --update-cache --available \
    && apk add --no-cache librdkafka

# Create nonroot user with same id as distroless images do it
RUN addgroup -g 65532 -S nonroot && adduser -u 65532 -S nonroot -G nonroot

COPY --chown=nonroot:nonroot --from=build-env /go/bin/app /
COPY --chown=nonroot:nonroot --from=build-env /go/src/resources /resources

USER 65532
EXPOSE 8080
CMD ["/app"]
