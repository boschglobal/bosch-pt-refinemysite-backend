[![Build Status](https://dev.azure.com/pt-iot/smartsite/_apis/build/status/csm.cloud.common.event-consumer?branchName=master)](https://dev.azure.com/pt-iot/smartsite/_build/latest?definitionId=56?branchName=master)

# Introduction

This repo contains all commonly used classes and interfaces that work with mysql

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

**Please note:** The plugin creates and pushes two commits, one for tagging the release and another one for switching back to next *SNAPSHOT* version.
