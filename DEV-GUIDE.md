# JJava Developer Guide

## Installing the latest snapshot version of the Kernel

On macOS (and possibly on Linux) you could just run `install-snapshot.sh` script 
to build and install the current snapshot version of the DFLib JJava kernel.

Alternatively, you could just build it and install as with any release version:

```bash
mvn clean package
unzip -u "${BUILD_DIR}".zip -d "${TARGET_PATH}"
jupyter kernelspec install "${TARGET_PATH}" --name=java --user
```

## Releasing New Version

### Prerequisites

You will need a **JDK** >= 11 and proper credentials for the `sonatype-central` repository
(see [docs](https://central.sonatype.org/publish/generate-portal-token/))

### Perform the Release

Two submodules of this project have different release strategies:

- `JJava Kernel` assembly is released using GitHub releases
- `jupyter-jvm-basekenel` module is released on Maven Central using the oss.sonatype.org repo

Still everything is done through a single set of Maven commands:

```bash
# mvn release:clean
mvn release:prepare -Prelease
mvn release:perform -Prelease
```
Go to https://central.sonatype.com/publishing and manually publish created bundle.

Go to [GitHub Releases](https://github.com/dflib/jjava/releases) to manually edit the created draft and publish it.
