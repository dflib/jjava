package org.dflib.jjava.distro;

import org.dflib.jjava.jupyter.channels.JupyterConnection;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.kernel.magics.ClasspathMagic;
import org.dflib.jjava.kernel.magics.JarsMagic;
import org.dflib.jjava.kernel.magics.LoadCodeMagic;
import org.dflib.jjava.maven.MavenDependencyResolver;
import org.dflib.jjava.maven.magics.AddMavenDependencyMagic;
import org.dflib.jjava.maven.magics.LoadFromPomCellMagic;
import org.dflib.jjava.maven.magics.LoadFromPomLineMagic;
import org.dflib.jjava.maven.magics.MavenMagic;
import org.dflib.jjava.maven.magics.MavenRepoMagic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The main class launching Jupyter Java kernel.
 */
public class JJava {

    // single-line JUL logging that mimics the default Jupyter format for the label:
    // [I 2025-10-19 16:15:41.181 ServerApp]
    private static final String JUL_JUPYTER_LOG_FORMAT = "[%4$.1s %1$tF %1$tT.%1$tL %3$s] %5$s%n";
    private static final String POM_PROPERTIES = "META-INF/maven/org.dflib.jjava/jjava-distro/pom.properties";

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            throw new IllegalArgumentException("Missing connection file argument");
        }

        Path connectionFile = Paths.get(args[0]);
        if (!Files.isRegularFile(connectionFile)) {
            throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");
        }

        System.setProperty("java.util.logging.SimpleFormatter.format", JUL_JUPYTER_LOG_FORMAT);

        KernelConnectionProperties connProps = KernelConnectionProperties.parse(Files.readString(connectionFile));
        JupyterConnection connection = new JupyterConnection(connProps);

        MavenDependencyResolver mavenResolver = new MavenDependencyResolver();

        Properties pomProps = loadPomProps();

        JavaKernel kernel = JavaKernel.builder()
                .name("JJava")
                .version((String) pomProps.getOrDefault("version", ""))

                .extensionsEnabled(Env.extensionsEnabled())
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

        // default startup: init "BaseKernel.notebookKernel" and install the default extensions (if enabled)
        kernel.onStartup();

        // process custom locations: expand JShell classpath, install extensions from those places (if enabled)
        kernel.addToClasspath(Env.extraClasspath());

        // run user defined startup snippets explicitly after the default startup
        Env.startupSnippets().forEach(kernel::evalRaw);

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
