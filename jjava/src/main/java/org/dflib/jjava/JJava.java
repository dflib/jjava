package org.dflib.jjava;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

/**
 * The main class launching Jupyter Java kernel.
 */
public class JJava {

    private static final String KERNEL_METADATA_FILE = "jjava-kernel-metadata.json";

    /**
     * @deprecated in favor of {@link Env#JJAVA_COMPILER_OPTS}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static final String COMPILER_OPTS_KEY = "IJAVA_COMPILER_OPTS";

    /**
     * @deprecated in favor of {@link Env#JJAVA_TIMEOUT}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static final String TIMEOUT_DURATION_KEY = "IJAVA_TIMEOUT";

    /**
     * @deprecated in favor of  {@link Env#JJAVA_CLASSPATH}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static final String CLASSPATH_KEY = "IJAVA_CLASSPATH";

    /**
     * @deprecated in favor of  {@link Env#JJAVA_STARTUP_SCRIPTS_PATH}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static final String STARTUP_SCRIPTS_KEY = "IJAVA_STARTUP_SCRIPTS_PATH";

    /**
     * @deprecated in favor of  {@link Env#JJAVA_STARTUP_SCRIPT}
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public static final String STARTUP_SCRIPT_KEY = "IJAVA_STARTUP_SCRIPT";

    private static JavaKernel kernel;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing connection file argument");
        }

        String version = loadKernelVersion();

        Path connectionFile = Paths.get(args[0]);
        if (!Files.isRegularFile(connectionFile)) {
            throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");
        }

        String contents = new String(Files.readAllBytes(connectionFile));

        JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);

        KernelConnectionProperties connProps = KernelConnectionProperties.parse(contents);
        JupyterConnection connection = new JupyterConnection(connProps);

        kernel = new JavaKernel(version);
        kernel.becomeHandlerForConnection(connection);

        connection.connect();
        connection.waitUntilClose();
    }

    /**
     * Obtain a reference to the kernel created by running {@link #main(String[])}. This
     * kernel may be null if one is not present but as the main use for this method is
     * for the kernel user code to access kernel services.
     *
     * @return the kernel created by running {@link #main(String[])} or {@code null} if
     * one has not yet (or already created and finished) been created.
     */
    public static JavaKernel getKernelInstance() {
        return JJava.kernel;
    }


    private static String loadKernelVersion() {
        JsonObject meta = loadKernelMetadata();
        return meta != null && meta.get("version") != null ? meta.get("version").getAsString() : "0";
    }

    private static JsonObject loadKernelMetadata() {

        try (Reader metaReader = new InputStreamReader(JJava.class.getClassLoader().getResourceAsStream(KERNEL_METADATA_FILE))) {
            return JsonParser.parseReader(metaReader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
