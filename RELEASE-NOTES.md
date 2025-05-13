## 1.0-a5

- #23 Port dependency management from Ivy to Maven 
- #52 Homebrew: unable to install package 
- #55 Working on POM in SNAPSHOT mode fails to work properly due to .ivy2 cache 
- #57 Black error message in dark mode (e.g. VSCode) not easily readable
- #59 BaseKernel.eval(..) should not declare a checked exception
- #64 Notebook hangs when a static method signature is redefined

## 1.0-a4

- #35 Replace custom installer with "jupyter kernelspec install" 
- #39 Convert the default init script to an extension 
- #40 Add "java.time" to the list of default imports
- #45 Publish jjava as a pip package
- #46 Publish jjava as a Homebrew package

## 1.0-M3

- #27 Add direct access to JShell
- #28 Auto-bootstrap of kernel-aware libraries 
- #29 Incorrect classpath displayed in the environment 
- #30 DFLib / Parquet and Avro classpath issues 
- #31 An older jar is used instead of the one indicated in %%loadFromPOM 
- #33 Internalize dependency on `jupyter-jvm-basekernel` 
- #36 Extract and publish JJava API library

## 1.0-M2

- #5 Switch project to Maven 
- #7 Project renaming
- #8 Java package change 
- #11 Enable GitHub Actions 
- #12 Dynamically change the kernel startup parameters
- #16 Upgrade gson transitive dependency 
- #21 Rendering stale var
- #22 Clean implementation of the LoaderDelegate

## 1.0-M1

- #1 Upgrade to Gradle 8.5
- #2 Update Ivy to 2.5.2
- #3 Remove usage of the `io.github.spencerpark.jupyter-kernel-installer` plugin
- #4 Fix memory leak
