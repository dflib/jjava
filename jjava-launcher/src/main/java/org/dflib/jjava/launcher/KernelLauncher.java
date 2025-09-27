package org.dflib.jjava.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The launcher responsible for creating a new process to run the JJava kernel.
 * <p>
 * The command to run the kernel is constructed by combining the JVM executable,
 * the JJAVA_JVM_OPTS environment variable (if set), and the kernel arguments.
 */
public class KernelLauncher {

    private static final String JJAVA_JVM_OPTS = "JJAVA_JVM_OPTS";

    private final List<String> args;

    public KernelLauncher(List<String> args) {
        this.args = args;
    }


    /**
     * Launches a new process to run the JJava kernel with the given arguments.
     * <p>
     * The kernel inherits the standard input, output and error streams from the parent process.
     * The method blocks until the kernel terminates.
     * It also provides shutdown hooks to terminate the kernel when the JVM terminates.
     *
     * @return the exit code of the kernel
     */
    public int launchKernel() {
        List<String> command = buildCommand(args);
        System.out.println("Running JJava Kernel with args: " + command);
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process kernel = pb.start();
            addShutdownHook(kernel);
            return kernel.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void addShutdownHook(Process kernel) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Launcher is shutting down, terminating the kernel...");
            kernel.destroy();
            try {
                kernel.waitFor();
                System.out.println("Kernel terminated.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private List<String> buildCommand(List<String> args) {
        if (args.size() < 2) {
            throw new RuntimeException(buildErrorMessage(args));
        }
        String kernelPath = args.get(0);
        String connectionFile = args.get(1);

        List<String> command = new ArrayList<>();
        command.add("java");

        // Get JVM options from environment variable if provided
        String jvmOptions = System.getenv(JJAVA_JVM_OPTS);
        if (jvmOptions != null && !jvmOptions.isBlank()) {
            String[] options = jvmOptions.split("\\s+");
            command.addAll(Arrays.asList(options));
        }

        // Add JVM option for JShell permissions
        command.add("--add-opens");
        command.add("jdk.jshell/jdk.jshell=ALL-UNNAMED");
        command.add("-jar");
        command.add(kernelPath);
        command.add(connectionFile);
        return command;
    }

    private String buildErrorMessage(List<String> args) {
        if (args.isEmpty()) {
            return String.format("Missing arguments: <kernel_path> <connection_file>%n");
        }
        if (args.size() < 2) {
            return String.format("Missing arguments: %s <connection_file>%n", args.get(0));
        }
        return String.format("Arguments provided: %s%n", String.join(", ",args));
    }
}
