package org.dflib.jjava;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.magics.ClasspathMagic;
import org.dflib.jjava.magics.JarsMagic;
import org.dflib.jjava.magics.LoadCodeMagic;
import org.dflib.jjava.magics.LoadFromPomCellMagic;
import org.dflib.jjava.magics.LoadFromPomLineMagic;
import org.dflib.jjava.magics.MavenMagic;
import org.dflib.jjava.magics.MavenRepoMagic;
import org.dflib.jjava.maven.MavenDependencyResolver;

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

    private static JJavaKernel kernel;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing connection file argument");
        }

        Path connectionFile = Paths.get(args[0]);
        if (!Files.isRegularFile(connectionFile)) {
            throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");
        }

        String contents = Files.readString(connectionFile);

        JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);

        KernelConnectionProperties connProps = KernelConnectionProperties.parse(contents);
        JupyterConnection connection = new JupyterConnection(connProps);

        MavenDependencyResolver mavenResolver = new MavenDependencyResolver();

        kernel = JJavaKernel.builder()
                .version(loadKernelVersion())

                .lineMagic("load", new LoadCodeMagic("", ".jsh", ".jshell", ".java", ".jjava"))
                .lineMagic("classpath", new ClasspathMagic())
                .lineMagic("maven", new MavenMagic(mavenResolver))
                .lineMagic("mavenRepo", new MavenRepoMagic(mavenResolver))
                .lineMagic("loadFromPOM", new LoadFromPomLineMagic(mavenResolver))
                .lineMagic("jars", new JarsMagic()) // TODO: deprecate redundant "jars" alias; "classpath" is a superset of this
                .lineMagic("addMavenDependency", new MavenMagic(mavenResolver)) // TODO: deprecate redundant "addMavenDependency" alias
                .cellMagic("loadFromPOM", new LoadFromPomCellMagic(mavenResolver))

                .build();

        kernel.becomeHandlerForConnection(connection);

        connection.connect();
        connection.waitUntilClose();
    }

    /**
     * Returns the kernel instance created in {@link #main(String[])}.
     */
    public static JJavaKernel getKernelInstance() {
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
