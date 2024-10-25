package org.dflib.jjava.jupyter.kernel;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class ContainerizedKernelCase {

    protected static final GenericContainer<?> container;
    protected static final String WORKING_DIRECTORY = "/test";
    protected static final String CONTAINER_KERNELSPEC = "/usr/share/jupyter/kernels/java";
    protected static final String CONTAINER_RESOURCES = WORKING_DIRECTORY + "/resources";
    protected static final String TEST_CLASSPATH = CONTAINER_RESOURCES + "/classes";

    private static final String BASE_IMAGE = String.format("eclipse-temurin:%s", Runtime.version().version().get(0));
    private static final String FS_KERNELSPEC = "../kernelspec/java";
    private static final String FS_RESOURCES = "src/test/resources";

    static {
        container = new GenericContainer<>(BASE_IMAGE)
                .withWorkingDirectory(WORKING_DIRECTORY)
                .withCopyToContainer(MountableFile.forHostPath(FS_KERNELSPEC), CONTAINER_KERNELSPEC)
                .withCopyToContainer(MountableFile.forHostPath(FS_RESOURCES), CONTAINER_RESOURCES)
                .withCommand("bash", "-c", getStartupCommand())
                .waitingFor(Wait.forSuccessfulCommand(getSuccessfulCommand()))
                .withStartupTimeout(Duration.ofMinutes(5));
        container.start();
    }

    @BeforeAll
    static void compileSources() throws IOException, InterruptedException {
        String source = "$(find " + CONTAINER_RESOURCES + "/src -name '*.java')";
        Container.ExecResult compileResult = executeInContainer("javac -d " + TEST_CLASSPATH + " " + source);

        assertEquals("", compileResult.getStdout());
        assertEquals("", compileResult.getStderr());
    }

    protected static Container.ExecResult executeInContainer(String... commands) throws IOException, InterruptedException {
        List<String> wrappedCommands = new ArrayList<>();
        wrappedCommands.add("bash");
        wrappedCommands.add("-c");
        wrappedCommands.addAll(List.of(commands));
        return container.execInContainer(wrappedCommands.toArray(new String[]{}));
    }

    protected static Container.ExecResult executeInKernel(String snippet) throws IOException, InterruptedException {
        return executeInKernel(snippet, Collections.emptyMap());
    }

    protected static Container.ExecResult executeInKernel(String snippet, Map<String, String> env) throws IOException, InterruptedException {
        String snippet64 = Base64.getEncoder().encodeToString(snippet.getBytes());
        String jupyterCommand = venvCommand("jupyter console --kernel=java --simple-prompt");
        String[] containerCommand = new String[]{"bash", "-c", "base64 -d <<< " + snippet64 + " | " + jupyterCommand};
        return container.execInContainer(ExecConfig.builder()
                .envVars(env)
                .command(containerCommand)
                .build()
        );
    }

    private static String getStartupCommand() {
        return String.join(" && ",
                "apt-get update",
                "apt-get install --no-install-recommends -y python3 python3-pip python3-venv",
                "python3 -m venv ./venv",
                venvCommand("pip install jupyter-console --progress-bar off"),
                "tail -f /dev/null"
        );
    }

    private static String getSuccessfulCommand() {
        return venvCommand("jupyter kernelspec list")
                + " | grep ' java ' && "
                + venvCommand("jupyter console --version");
    }

    private static String venvCommand(String command) {
        return WORKING_DIRECTORY + "/venv/bin/" + command;
    }
}
