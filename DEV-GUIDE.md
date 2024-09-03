# JJava Developer Guide

## Releasing New Version

Two submodules of this project have different release strategies.

- `JJava Kernel` assembly is released using GitHub releases
- `jupyter-jvm-basekenel` module is released on Maven Central using the oss.sonatype.org repo

Before performing a release you should check `RELEASE-NOTES.md` to see that everything intended is there.

### Prerequisites

You need a **JDK** >= 11 and a proper credentials for the `oss-sonatype-releases` repository

### Preparing release

To start a release process you could use `maven-release-plugin` as usual:

```bash
mvn release:clean
mvn release:prapare
```

These commands will create a release tag and [GitHub Actions](https://github.com/dflib/jjava/actions/workflows/release.yml) automation will create a draft release.
(**NOTE**: this is not fully set up for now, and release creation will fail, see next section).

### Manual creation of the JJava release on GitHub

You could create a release manually in case GitHub Action is failing.

Just clone the tag created in the previous step

```bash
git clone https://github.com/dflib/jjava.git --branch "XXX" --depth 1
```
and run
```bash
mvn clean package
```

Then go to [GitHub Releases](https://github.com/dflib/jjava/releases) create a new release and upload `jjava/target/jjava-XXX.zip` artifact.

### Finalizing `JJava` release

To finalize the JJava release you should go to the [GitHub Releases](https://github.com/dflib/jjava/releases) to manually edit the created draft and publish it.

### Finalizing `jupyter-jvm-basekenel` release

To finalize the `jupyter-jvm-basekenel` release run:

```bash
mvn release:perform -Prelease
```

And go to [https://s01.oss.sonatype.org/]() and manually close and release the staging repository.
