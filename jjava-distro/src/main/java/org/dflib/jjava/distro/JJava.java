package org.dflib.jjava.distro;

import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.kernel.magics.ClasspathMagic;
import org.dflib.jjava.kernel.magics.JarsMagic;
import org.dflib.jjava.kernel.magics.LoadCodeMagic;
import org.dfllib.jjava.maven.MavenDependencyResolver;
import org.dfllib.jjava.maven.magics.AddMavenDependencyMagic;
import org.dfllib.jjava.maven.magics.LoadFromPomCellMagic;
import org.dfllib.jjava.maven.magics.LoadFromPomLineMagic;
import org.dfllib.jjava.maven.magics.MavenMagic;
import org.dfllib.jjava.maven.magics.MavenRepoMagic;

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

        JavaKernel kernel = JavaKernel.builder()
                .name("JJava")
                .version((String) pomProps.getOrDefault("version", ""))

                .extensionsEnabled(Env.extensionsEnabled())
                .startupSnippets(Env.startupSnippets())
                .compilerOpts(Env.compilerOpts())
                .timeout(Env.timeout())

                .lineMagic("load", new LoadCodeMagic("", ".jsh", ".jshell", ".java", ".jjava"))
                .lineMagic("classpath", new ClasspathMagic())
                .lineMagic("maven", new MavenMagic(mavenResolver))
                .lineMagic("mavenRepo", new MavenRepoMagic(mavenResolver))
                .lineMagic("loadFromPOM", new LoadFromPomLineMagic(mavenResolver))

                // temporarily support a few deprecated magics
                .lineMagic("jars", new JarsMagic())
                .lineMagic("addMavenDependency", new AddMavenDependencyMagic(mavenResolver))

                .cellMagic("loadFromPOM", new LoadFromPomCellMagic(mavenResolver))

                .build();

        // install built-in Extensions
        kernel.onStartup();

        // add custom locations to JShell classpath, look for Extensions there, load and install them
        kernel.addToClasspath(Env.extraClasspath());

        // connect to Jupyter
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
