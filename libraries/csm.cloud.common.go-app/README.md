# csm.cloud.common.go-app library

Provides common functionalities: 
- the app package provides functionality for error handling and runtime support
- the config package enables your app with cloud-ready configuration handling (slightly inspired by Spring Boot)
- the configuration package contains common reusable configuration models
- the datadog package provides DD tracing support
- the kafka package contains reusable components and services to interact with Kafka, specifically: 
  - schema registry support
  - generic Kafka producer
  - avro serializer

# GO get started

- Check the [Go Installation Guide](https://bosch-pt.atlassian.net/wiki/x/LICNlgI)
  in confluence to figure out how to install Go.

- Check the [Golang Cheat Sheet](https://bosch-pt.atlassian.net/wiki/x/-4CMmgI)
  in confluence to learn how to work with Go.

- This application is a private dependency (go module located in private Azure DevOps repository).
  Check [Golang Azure DevOps Dependencies](https://bosch-pt.atlassian.net/wiki/x/44CSmgI)
  in confluence to set up support for using private dependencies on your system.

A quick introduction to go can be found [here](https://go.dev/tour/list).

# Use the library

## 1. Ensure you have the setup in place for [private go dependencies](https://bosch-pt.atlassian.net/wiki/x/44CSmgI)

## 2. Add dependency to go.mod file of the project you would like to use this library in
```
go get dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git
```

# Release the library

GO handles dependencies based on GIT repositories and tags.
Example: 
```
git tag v0.2.1
git push origin v0.2.1
```
would release a version `v0.2.1` of `dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git`

which can be imported in a GO app in `go.mod` like so:

```
require (
	dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git v0.2.1
)
```

## test
run `go test ./...`

## build
run `go build`