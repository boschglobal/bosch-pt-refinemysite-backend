[![Build Status](https://dev.azure.com/pt-iot/smartsite/_apis/build/status/csm.cloud.storage.event.avro?branchName=master)](https://dev.azure.com/pt-iot/smartsite/_build/latest?definitionId=52?branchName=master)

# Introduction

This repo contains generated code for storage event avro schema, the event listener interface and convenience classes
usable in other modules to simplify the testing (event stream generator, event listener extensions, etc.).

# Build and Versioning

The project is build using the [gradle build tool](https://gradle.org/). You can trigger a build using this command:

```Bash
./gradlew clean build
```

This library uses [semantic versioning](https://semver.org).

According to this the version number follows the pattern **MAJOR.MINOR.PATCH**. Increment the:

- *MAJOR* version when you make incompatible API changes,
- *MINOR* version when you add functionality in a backwards-compatible manner, and
- *PATCH* version when you make backwards-compatible bug fixes.

The target version of this library is calculated using special messages of all git commits between the last git tag up
to the git head reference.

| Semantic version | Sample  | Commit message prefix  |
| -----------------|:-------:| ----------------------:|
| Patch            | 1.0.1   | SMAR-1234: xxx         |
| Patch            | 1.0.1   | SMAR-1234 [patch]: xxx |
| Minor            | 1.1.0   | SMAR-1234 [minor]: xxx |
| Major            | 2.0.0   | SMAR-1234 [major]: xxx |

The target version is calculated by
the [semantic version gradle plugin](https://github.com/vivin/gradle-semantic-build-versioning)  and can be checked by
this command:

```Bash
./gradlew printVersion
```

This should print out something like this:

```Bash
> Task :printVersion
1.0.0-SNAPSHOT
```

**Please note**: If you omit the commit message prefix (*patch*, *minor*, *major*) then the gradle plugin always
calculates a *patch* version number!

# Release

As long as you implement new features or fixes to this library you are working on *SNAPSHOT* versions (like
1.0.0-SNAPSHOT). If you want to release the library this should be done in two steps:

1. Create a tagged version without the *SNAPSHOT* suffix (e.g. **1.0.0**)
2. Add a new commit to the repo with switching to next *SNAPSHOT* version (e.g. 1.0.1-SNAPSHOT) to continue next
   implementation.

These steps are done using the [gradle release plugin](https://github.com/researchgate/gradle-release).

To create a new release first all changes have to be committed and pushed to the remote repo. Then you can create the
release by using the following command:

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
