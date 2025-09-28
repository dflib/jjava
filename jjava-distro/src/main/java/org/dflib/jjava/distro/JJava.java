package org.dflib.jjava.distro;

import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.kernel.JJavaKernel;
import org.dflib.jjava.kernel.magics.ClasspathMagic;
import org.dflib.jjava.kernel.magics.JarsMagic;
import org.dflib.jjava.kernel.magics.LoadCodeMagic;
import org.dflib.jjava.kernel.magics.LoadFromPomCellMagic;
import org.dflib.jjava.kernel.magics.LoadFromPomLineMagic;
import org.dflib.jjava.kernel.magics.MavenMagic;
import org.dflib.jjava.kernel.magics.MavenRepoMagic;
import org.dflib.jjava.kernel.maven.MavenDependencyResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;

/**
 * The main class launching Jupyter Java kernel.
 */
public class JJava {

    private static final String POM_PROPERTIES = "META-INF/maven/org.dflib.jjava/jjava-distro/pom.properties";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing connection file argument");
        }

        Path connectionFile = Paths.get(args[0]);
        if (!Files.isRegularFile(connectionFile)) {
            throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");
        }

        JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);

        KernelConnectionProperties connProps = KernelConnectionProperties.parse(Files.readString(connectionFile));
        JupyterConnection connection = new JupyterConnection(connProps);

        MavenDependencyResolver mavenResolver = new MavenDependencyResolver();

        Properties pomProps = loadPomProps();

        JJavaKernel kernel = JJavaKernel.builder()
                .name("JJava")
                .version((String) pomProps.getOrDefault("version", ""))

                .extensionsEnabled(Env.extensionsEnabled())
                .startupSnippets(Env.startupSnippets())
                .compilerOpts(Env.compilerOpts())
                .extraClasspath(Env.extraClasspath())
                .timeout(Env.timeout())

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

    private static Properties loadPomProps() {

        Properties props = new Properties();

        InputStream in = JJava.class.getClassLoader().getResourceAsStream(POM_PROPERTIES);
        if (in == null) {
            return props;
        }

        try {
            try {
                props.load(in);
                return props;
            } finally {
                in.close();
            }
        } catch (IOException e) {
            // generally, this should be ignorable, but it should also never happen, so still rethrow
            throw new RuntimeException("Error reading project properties");
        }
    }
}
