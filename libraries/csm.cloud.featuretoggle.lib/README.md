[![Build Status](https://dev.azure.com/pt-iot/smartsite/_apis/build/status/csm.cloud.featuretoggle.lib?branchName=master)](https://dev.azure.com/pt-iot/smartsite/_build/latest?definitionId=56?branchName=master)

# Introduction

This repo contains functionality to replicate feature toggles in downstream services.

# Usage

Add this library as dependency to a service. Configure a MongoDB. Configure following properties:

- `custom.feature.enabled` (set to `true` to enable the replication of feature toggles)
- `custom.kafka.bindings.feature.kafkaTopic` (the featuretoggle topic)
- `custom.kafka.listener.query.feature-projector.groupId` (group id for the kafka listener)
- `custom.kafka.listener.query.feature-projector.clientIdPrefix` (client id prefix for the kafka listener)

The `FeatureQueryService` can be used to query if a feature is enabled or query all enabled features for a subject.

A bean of type `ParticipantAuthorization` can be implemented and provided to enable the rest controller where enabled
features by project can be queried. Additionally, the property `custom.feature.endpoint.prefix` must be set. The path
for the endpoint will then be `${custom.feature.endpoint.prefix}/projects/<projectid>/features`.

The kafka listener for feature events can be disabled by activating the
profile `kafka-feature-projector-listener-disabled`.

# Versioning

This library uses [semantic versioning](https://semver.org).

Accordingly, the version number follows the pattern **MAJOR.MINOR.PATCH**.

Increment the:

- *MAJOR* version when you make incompatible API changes,
- *MINOR* version when you add functionality in a backwards-compatible manner, and
- *PATCH* version when you make backwards-compatible bug fixes.

# Build

The project is build using [Gradle](https://gradle.org/):

```Bash
./gradlew clean build
```

# Release

To create a new release, first make sure that all changes are committed and pushed to the remote repo.

Then, you can create the release:

```Bash
./gradlew release
```

This will work in interactive mode by asking to confirm the release version and the next *SNAPSHOT* version.

You can also use this command in automatic mode (i.e. without any interaction):

```Bash
./gradlew release -Prelease.useAutomaticVersion=true
```

**Please note:** The plugin creates and pushes two commits, one for tagging the release and another one for switching
back to next *SNAPSHOT* version.
