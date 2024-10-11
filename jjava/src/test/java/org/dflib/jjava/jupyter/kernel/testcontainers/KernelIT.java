package org.dflib.jjava.jupyter.kernel.testcontainers;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.time.Duration;

public abstract class KernelIT {

    private static final String BASE_IMAGE = String.format("eclipse-temurin:%s", Runtime.version().version().get(0));
    private static final String FS_KERNELSPEC = "../kernelspec/java";
    private static final String CONTAINER_KERNELSPEC = "/usr/share/jupyter/kernels/java";
    private static final String WORKING_DIRECTORY = "/test";

    protected static final GenericContainer<?> container;

    static {
        container = new GenericContainer<>(BASE_IMAGE)
                .withWorkingDirectory(WORKING_DIRECTORY)
                .withCommand("bash", "-c", getStartupCommand())
                .withFileSystemBind(FS_KERNELSPEC, CONTAINER_KERNELSPEC, BindMode.READ_ONLY)
                .waitingFor(Wait.forSuccessfulCommand(getSuccessfulCommand()))
                .withStartupTimeout(Duration.ofMinutes(3));
        container.start();
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
                + "| grep ' java ' &&"
                + venvCommand("jupyter console --version");
    }

    private static String venvCommand(String command) {
        return WORKING_DIRECTORY + "/venv/bin/" + command;
    }

    protected Container.ExecResult executeInKernel(String snippet) throws IOException, InterruptedException {
        return container.execInContainer(
                "bash",
                "-c",
                "echo '" + snippet + "' | " + venvCommand("jupyter console --kernel=java --simple-prompt")
        );
    }
}
