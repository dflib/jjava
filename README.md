# JJava

## Project notes

This project is a Java Jupyter kernel based on the [IJava](https://github.com/SpencerPark/IJava) project.

For a full list of changes check [release notes](RELEASE-NOTES.md).

## Project info

![display-img](docs/img/display-img.png)

A [Jupyter](http://jupyter.org/) kernel for executing Java code. The kernel executes code via the new [JShell tool](https://docs.oracle.com/javase/9/jshell/introduction-jshell.htm). Some of the additional commands should be supported as needed via a syntax similar to the ipython magics.

The kernel is fully functional. Check out the [list of features](#features) further down in the README. Any requests for new ones or prioritizing current requests are welcomed in the [issues](https://github.com/SpencerPark/IJava/issues) along with bug requests, installation help, or other questions.

If you are interested in building your own kernel that runs on the JVM check out the related project that this kernel is build on, [jupyter-jvm-basekernel](https://github.com/SpencerPark/jupyter-jvm-basekernel).

### Contents

*   [Features](#features)
*   [Requirements](#requirements)
*   [Installing](#installing)
    *   [Install pre-build binary](#install-pre-built-binary)
    *   [Install from source](#install-from-source)
*   [Configuring](#configuring)
    *   [List of options](#list-of-options)
    *   [Changing VM/compiler options](#changing-vmcompiler-options)
    *   [Configuring startup scripts](#configuring-startup-scripts)
*   [Run](#run)

### Features

Currently, the kernel supports

*   Code execution.
    ![output](docs/img/output.png)
*   Autocompletion (`TAB` in Jupyter notebook).
    ![autocompletion](docs/img/autocompletion.png)
*   Code inspection (`Shift-TAB` up to 4 times in Jupyter notebook).
    ![code-inspection](docs/img/code-inspection.png)
*   Colored, friendly, error message displays.
    ![compilation-error](docs/img/compilation-error.png)
    ![incomplete-src-error](docs/img/incomplete-src-error.png)
    ![runtime-error](docs/img/runtime-error.png)
*   Add maven dependencies at runtime (See also [magics.md](docs/magics.md)).
    ![maven-pom-dep](docs/img/maven-pom-dep.png)
*   Display rich output (See also [display.md](docs/display.md) and [maven magic](docs/magics.md#addmavendependencies)). Chart library in the demo photo is [XChart](https://github.com/knowm/XChart) with the sample code taken from their README.
    ![display-img](docs/img/display-img.png)
*   `eval` function. (See also [kernel.md](docs/kernel.md)) **Note: the signature is `Object eval(String) throws Exception`.** This evaluates the expression (a cell) in the user scope and returns the actual evaluation result instead of a serialized one.
    ![eval](docs/img/eval.png)
*   Configurable evaluation timeout
    ![timeout](docs/img/timeout.png)

### Requirements

1.  [Java JDK >= 11](https://adoptium.net/temurin/releases/). **Not the JRE**.

    1.  Ensure that the `java` command is in the PATH and is using version 11. For example:
        ```bash
        > java -version
        openjdk version "11.0.21" 2023-10-17
        OpenJDK Runtime Environment Temurin-11.0.21+9 (build 11.0.21+9)
        OpenJDK 64-Bit Server VM Temurin-11.0.21+9 (build 11.0.21+9, mixed mode)
        ```

    2.  Next ensure that `java` is in a location where the jdk was installed and not just the jre. Use the `java --list-modules` command to do this. The list should contain `jdk.jshell`.

        *   On *nix `java --list-modules | grep "jdk.jshell"`
        *   On windows `java --list-modules | findstr "jdk.jshell"`

        Both should output `jdk.jshell@` followed by your java version.

    If the kernel cannot start with an error along the lines of
    ```text
    Exception in thread "main" java.lang.NoClassDefFoundError: jdk/jshell/JShellException
            ...
    Caused by: java.lang.ClassNotFoundException: jdk.jshell.JShellException
            ...
    ```
    then double check that `java` is referring to the command for the `jdk` and not the `jre`.
    
2.  Some jupyter-like environment to use the kernel in.

    A non-exhaustive list of options:

    *   [Jupyter](http://jupyter.org/install)
    *   [JupyterLab](http://jupyterlab.readthedocs.io/en/stable/getting_started/installation.html)
    *   [nteract](https://nteract.io/desktop)
        
### Installing

After meeting the [requirements](#requirements), the kernel can be installed locally. Any time you wish to remove a kernel you may use `jupyter kernelspec remove java`. If you have installed the kernel to multiple directories, this command may need to be run multiple times as it might only remove 1 installation at a time.

#### Install pre-built binary

Get the latest _release_ of the software with no compilation needed. See [Install from source](#install-from-source) for building the the latest commit.

**Note:** if you have an old installation or a debug one from running `gradlew installKernel` it is suggested that it is first removed via `jupyter kernelspec remove java`.

1.  Download the release from the [releases tab](https://github.com/dflib/jjava/releases). A prepackaged distribution will be in an artifact named `jjava-$version.zip`.

2.  Unzip it into a temporary location. It should have at least the `install.py` and `java` folder extracted in there.

3.  Run the installer with the same python command used to install jupyter. The installer is a python script and has the same options as `jupyter kernelspec install` but additionally supports configuring some of the kernel properties mentioned further below in the README.

    ```bash
    # Pass the -h option to see the help page
    > python3 install.py -h

    # Otherwise a common install command is
    > python3 install.py --sys-prefix
    ```

4.  Check that it installed with `jupyter kernelspec list` which should contain `java`.

#### Install from source

Get the latest version of the kernel but possibly run into some issues with installing. This is also the route to take if you wish to contribute to the kernel.

1.  Download the project.
    ```bash
    > git clone https://github.com/dflib/jjava.git
    > cd jjava/
    ```

2.  Build the kernel.
    
    On *nix `./gradlew zipKernel`
        
    On windows `gradlew zipKernel`

3.  Install `build/distributions/jjava-$version.zip` custom build as a pre-built binary 


### Configuring

Configuring the kernel can be done via environment variables. These can be set on the system or inside the `kernel.json`. The configuration can be done at install time, which may be repeated as often as desired. The parameters are listed with `python3 install.py -h` as well as below in the list of options. Configuration done via the installer (or `gradlew installKernel --param ...:...`) should use the names in the _Parameter name_ column.

#### List of options

| Environment variable | Parameter name | Default | Description |
|----------------------|----------------|---------|-------------|
| `IJAVA_COMPILER_OPTS` | `comp-opts` | `""` | A space delimited list of command line options that would be passed to the `javac` command when compiling a project. For example `-parameters` to enable retaining parameter names for reflection. |
| `IJAVA_TIMEOUT` | `timeout` | `"-1"` | A duration specifying a timeout (in milliseconds by default) for a _single top level statement_. If less than `1` then there is no timeout. If desired a time may be specified with a [`TimeUnit`](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/TimeUnit.html) may be given following the duration number (ex `"30 SECONDS"`). |
| `IJAVA_CLASSPATH` | `classpath` | `""` | A file path separator delimited list of classpath entries that should be available to the user code. **Important:** no matter what OS, this should use forward slash "/" as the file separator. Also each path may actually be a [simple glob](#simple-glob-syntax). |
| `IJAVA_STARTUP_SCRIPTS_PATH` | `startup-scripts-path` | `""` | A file path seperator delimited list of `.jshell` scripts to run on startup. This includes [ijava-jshell-init.jshell](src/main/resources/ijava-jshell-init.jshell) and [ijava-display-init.jshell](src/main/resources/ijava-display-init.jshell). **Important:** no matter what OS, this should use forward slash "/" as the file separator. Also each path may actually be a [simple glob](#simple-glob-syntax). |
| `IJAVA_STARTUP_SCRIPT` | `startup-script` | `""` | A block of java code to run when the kernel starts up. This may be something like `import my.utils;` to setup some default imports or even `void sleep(long time) { try {Thread.sleep(time); } catch (InterruptedException e) { throw new RuntimeException(e); }}` to declare a default utility method to use in the notebook. |

##### Simple glob syntax

Options that support this glob syntax may reference a set of files with a single path-like string. Basic glob queries are supported including:

*   `*` to match 0 or more characters up to the next path boundary `/`
*   `?` to match a single character
*   A path ending in `/` implicitly adds a `*` to match all files in the resolved directory

Any relative paths are resolved from the notebook server's working directory. For example the glob `*.jar` will match all jars is the directory that the `jupyter notebook` command was run.

**Note:** users on any OS should use `/` as a path separator.

#### Changing VM/compiler options

See the [List of options](#list-of-options) section for all of the configuration options.

To change compiler options use the `IJAVA_COMPILER_OPTS` environment variable (or `--comp-opts` parameter during installation) with a string of flags as if running the `javac` command.

The kernel VM parameters must currently be assigned in the `kernel.json` by adding/editing a JSON dictionary at the `env` key and changing the `argv` list. To find where the kernel is installed run

```bash
> jupyter kernelspec list
Available kernels:
  java           .../kernels/java
  python3        .../python35/share/jupyter/kernels/python3
```

and the `kernel.json` file will be in the given directory.

For example to enable assertions, set a limit on the heap size to `128m`.

```diff
{
- "argv": [ "java", "-jar", "{connection_file}"],
+ "argv": [ "java", "-ea", "-Xmx128m", "-jar", "{connection_file}"],
  "display_name": "Java",
  "language": "java",
  "interrupt_mode": "message",
  "env": {
  }
}
```

### Run

This is where the documentation diverges, each environment has it's own way of selecting a kernel. To test from command line with Jupyter's console application run:

```bash
jupyter console --kernel=java
```

Then at the prompt try:
```java
In [1]: String helloWorld = "Hello world!"

In [2]: helloWorld
Out[2]: "Hello world!"
```
