# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, Test, Run

- `mvn clean package` — full build; produces the shaded kernel jar and the kernelspec zip under `jjava-distro/target/`.
- `mvn clean verify` — runs Failsafe integration tests (`*IT.java` in `jjava-distro`) via Testcontainers if Testcontainers can reach Docker (Windows GitHub-hosted runners cannot run WSL-backed containers). If Docker is unavailable, use `mvn clean test`.
- `mvn test -pl jjava-kernel -am` — single-module unit tests (with required upstream modules built).
- `mvn -pl jjava-kernel test -Dtest=JavaKernelExtensionsLifecycleTest` — run a single test class.
- Surefire in `jjava-kernel` and `jjava-distro` is configured with `--add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED` — required for JShell reflection. Anything that exercises `JavaKernel`/`CodeEvaluator` directly needs the same flag.
- Local install of the freshly built kernel into Jupyter: see `DEV-GUIDE.md` (`unzip` the kernelspec zip, then `jupyter kernelspec install ... --name=java --user`).

Minimum toolchain: JDK 21 (`maven.compiler.release=21`). CI matrix builds against 21 and 25.

## Module Layout and Dependency Graph

This is a multi-module Maven project (`groupId: org.dflib.jjava`). Each module has a distinct role — do not collapse them.

```
jjava-jupyter      ← generic, language-agnostic Jupyter kernel framework
   ▲
jjava-kernel       ← Java-specific kernel (uses JShell), built on jjava-jupyter
   ▲
jjava-maven        ← Maven dependency resolution magics, built on jjava-kernel
   ▲
jjava-distro       ← Fat/shaded jar combining kernel + maven + magics. THE deliverable.

jjava-launcher     ← Tiny no-dependency jar; spawns the child java process for jjava-distro.
```

- **`jjava-jupyter`** — Reusable JVM Jupyter library: the ZMQ wire protocol (jeromq), message types and Gson adapters, channel loops (`shell`, `iopub`, `stdin`, `heartbeat`, `control`), display/rendering, magics infrastructure (`MagicsRegistry`, `MagicsResolver`, `MagicTranspiler`), comm, history, and `BaseKernel` — the abstract superclass that any language-specific kernel extends. Published to Maven Central as a library for building other JVM kernels.
- **`jjava-kernel`** — `JavaKernel extends BaseKernel`. Owns the `JShell` instance and `CodeEvaluator` (with custom `JJavaExecutionControl` and `JJavaLoaderDelegate`). Ships built-in magics under `org.dflib.jjava.kernel.magics`: `%load`, `%classpath`, `%time`, `%jars` (deprecated).
- **`jjava-maven`** — Maven dependency resolution, isolated so the core kernel stays slim. Uses Maveniverse Mima for resolution. Ships `%maven`, `%mavenRepo`, `%loadFromPOM` (line + cell), `%addMavenDependency` (deprecated).
- **`jjava-distro`** — The actual runnable kernel jar. Main class: `org.dflib.jjava.distro.JJava`. Wires all magics into `JavaKernel.builder()`, reads `JJAVA_*` env vars (see `Env.java`), parses the Jupyter connection file, and runs the message loop. Uses `maven-shade-plugin` to relocate every 3rd-party package (gson, jeromq, slf4j, maven, mima, etc.) under `org.dflib.jjava.shaded.*` — keep this in mind when reading stack traces from a built kernel. SLF4J is bound to `slf4j-jdk14` (JUL) inside the shaded jar.
- **`jjava-launcher`** — Standalone jar with zero dependencies. Spawns a child `java` process running the distro jar, applying `--add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED` and any `JJAVA_JVM_OPTS`. Its `package` phase also copies the launcher jar into `kernelspec/java/jjava-launcher.jar`. Uses `java.util.logging` directly rather than SLF4J because it must remain dependency-free.

`kernelspec/java/kernel.json` is the Jupyter kernel descriptor — its `argv` invokes `jjava-launcher.jar`, which in turn invokes `jjava.jar`. The shade output of `jjava-distro` and the `jjava-launcher` jar are both copied into the assembled `jjava-<version>-kernelspec.zip` (see `jjava-distro/assembly/zipped-kernel.xml`).

`pip/jjava/` and `pyproject.toml` package the kernelspec for PyPI distribution; version is injected from the git tag by the release workflow.

## Runtime Behavior to Keep in Mind

- **JShell-based execution.** All user code runs in a `JShell` instance owned by `JavaKernel`. `CodeEvaluator` orchestrates snippet evaluation, and `JJavaLoaderDelegate` participates in JShell's class loading. Adding to the classpath at runtime means calling `kernel.addToClasspath(...)`, which both extends JShell's classpath *and* triggers extension discovery from the new entries.
- **Magic resolution happens before evaluation.** Cells are passed through `MagicTranspiler` / `MagicsResolver`. Line magics start with `%`, cell magics with `%%`. New magics are registered in `JJava.main()` via `.lineMagic(...)` / `.cellMagic(...)` on the `JavaKernel.Builder`.
- **Extensions** are discovered via `ServiceLoader` for `org.dflib.jjava.jupyter.Extension`, scanning both the application classpath and any classpath added at runtime through magics. `JJAVA_LOAD_EXTENSIONS=false` disables this entirely. Extensions get an `install(BaseKernel)` callback on load and `uninstall` on shutdown.
- **Environment variables** consumed by the kernel are centralized in `org.dflib.jjava.distro.Env`: `JJAVA_COMPILER_OPTS`, `JJAVA_TIMEOUT`, `JJAVA_CLASSPATH`, `JJAVA_STARTUP_SCRIPT`, `JJAVA_STARTUP_SCRIPTS_PATH`, `JJAVA_LOAD_EXTENSIONS`. `JJAVA_JVM_OPTS` is read by the launcher, not the kernel, and is *not* forwarded to the child process beyond being unpacked into the JVM command line.
- **Logging.** Code in `jjava-jupyter`, `jjava-kernel`, `jjava-maven` uses SLF4J. The launcher and `JJava` main class use JUL directly (the launcher because it has no dependencies; `JJava.main` because it sets the JUL `SimpleFormatter.format` system property to mimic Jupyter's log style before the SLF4J→JUL bridge takes over). In the shaded distro jar, SLF4J is itself relocated to `org.dflib.jjava.shaded.org.slf4j` and routed to JUL via `slf4j-jdk14`.
- **Integration tests** (`jjava-distro/src/test/java/.../*IT.java`) extend `ContainerizedKernelCase`, which mounts the locally-built `kernelspec/java` and `src/test/resources` into an `eclipse-temurin:<jdkVersion>` container and runs `jupyter` against the real kernel. These tests need a working Docker daemon and the kernel jars must already be packaged (Failsafe runs in the `verify` phase, after `package`).
