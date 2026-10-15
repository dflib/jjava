# JJava Developer Guide

## Building and Installing Locally-built Kernel

On MacOS and Linux, run the following commands:

```bash
mvn clean package
unzip -u jjava-distro/target/jjava-*-kernelspec.zip -d jjava-distro/target/unzip ; \
  jupyter kernelspec remove -y java ; \
  jupyter kernelspec install jjava-distro/target/unzip --name=java --user
```

## Releasing New Version

### Prerequisites

You will need a **JDK** >= 11 and proper credentials for the `sonatype-central` repository
(see [docs](https://central.sonatype.org/publish/generate-portal-token/))

### Perform the Release

Two submodules of this project have different release strategies:

- the library modules (`jjava-jupyter`, `jjava-kernel`, `jjava-maven`) are released
  on Maven Central via the Sonatype Central Portal
- the `jjava-distro` kernel assembly is released through GitHub Releases, and from there picked up by
  the [Homebrew tap](https://github.com/dflib/homebrew-tap) and PyPI

Still everything is done through a single set of Maven commands:

```bash
# mvn release:clean
mvn release:prepare -Prelease
mvn release:perform -Prelease
```
Go to https://central.sonatype.com/publishing and manually publish created bundle.

Go to [GitHub Releases](https://github.com/dflib/jjava/releases) to manually edit the created draft and publish it.
