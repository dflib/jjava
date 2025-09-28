# JJava Developer Guide

## Building and Installing Locally-built Kernel

On MacOS and Linux, run the following commands:

```bash
mvn clean package
unzip -u jjava-distro/target/jjava-*-kernelspec.zip -d jjava-distro/target/unzip
jupyter kernelspec remove -y java
jupyter kernelspec install jjava-distro/target/unzip --name=java --user
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
