package org.dflib.jjava.distro;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class ContainerizedKernelCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerizedKernelCase.class);

    protected static GenericContainer<?> container;
    protected static final String WORKING_DIRECTORY = "/test";
    protected static final String CONTAINER_KERNELSPEC = "/usr/share/jupyter/kernels/java";
    protected static final String CONTAINER_RESOURCES = WORKING_DIRECTORY + "/resources";
    protected static final String TEST_CLASSPATH = CONTAINER_RESOURCES + "/classes";

    private static final String BASE_IMAGE = String.format("eclipse-temurin:%s", Runtime.version().feature());
    private static final String FS_KERNELSPEC = "../kernelspec/java";
    private static final String FS_RESOURCES = "src/test/resources";

    private static final String TESTS_ENABLED_PROPERTY = "docker.tests.enabled";

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        initializeContainer();
        Assumptions.assumeTrue(container != null, "Docker tests are disabled. Enable with -Pdocker");

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
        String snippetEscaped = snippet.replace("\\", "\\\\").replace("\"", "\\\"");
        String pexpectScript = String.join("\n",
                "import pexpect, sys, os, time",
                "env = os.environ.copy()",
                "env['PROMPT_TOOLKIT_NO_CPR'] = '1'",
                "env['TERM'] = 'dumb'",
                "p=pexpect.spawn('" + venvCommand("jupyter") + "', "
                        + "['console', '--kernel=java', '--no-confirm-exit'], "
                        + "env=env, timeout=60, encoding='utf-8')",
                "p.logfile = sys.stdout",
                "p.expect(r'In \\[\\d+\\]:', timeout=10)",
                "p.send(\"\"\"" + snippetEscaped + "\\n\"\"\")",
                "p.expect(r'In \\[\\d+\\]:')",
                "p.logfile.flush()",
                "time.sleep(0.1)",
                "p.sendeof()",
                "p.expect(pexpect.EOF, timeout=5)",
                "p.close()"
        );
        String[] containerCommand = new String[]{venvCommand("python"), "-c", pexpectScript};
        Container.ExecResult execResult = container.execInContainer(ExecConfig.builder()
                .envVars(env)
                .command(containerCommand)
                .build()
        );

        LOGGER.info("env = {}", env);
        LOGGER.info("snippet = {}", snippet);
        LOGGER.info("exitCode = {}", execResult.getExitCode());
        LOGGER.debug("stdout = {}", execResult.getStdout());
        LOGGER.debug("stderr = {}", execResult.getStderr());
        return execResult;
    }

    private static void initializeContainer() {
        if (container != null || !"true".equals(System.getProperty(TESTS_ENABLED_PROPERTY))) {
            return;
        }

        container = new GenericContainer<>(BASE_IMAGE)
                .withWorkingDirectory(WORKING_DIRECTORY)
                .withCopyToContainer(MountableFile.forHostPath(FS_KERNELSPEC), CONTAINER_KERNELSPEC)
                .withCopyToContainer(MountableFile.forHostPath(FS_RESOURCES), CONTAINER_RESOURCES)
                .withCommand("bash", "-c", getStartupCommand())
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forSuccessfulCommand(getSuccessfulCommand()))
                .withStartupTimeout(Duration.ofMinutes(1));
        container.start();
    }

    private static String getStartupCommand() {
        return String.join(" && ",
                "apt-get update",
                "apt-get install --no-install-recommends -y python3 python3-pip python3-venv curl",
                "python3 -m venv ./venv",
                venvCommand("pip install jupyter-console pexpect --progress-bar off"),
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
