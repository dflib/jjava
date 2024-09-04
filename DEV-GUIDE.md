# JJava Developer Guide

## Releasing New Version

### Prerequisites

You will need a **JDK** >= 11 and proper credentials for the `oss-sonatype-releases` repository

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
Go to [https://s01.oss.sonatype.org/]() and manually close and release the staging repository.

Go to [GitHub Releases](https://github.com/dflib/jjava/releases) to manually edit the created draft and publish it.
