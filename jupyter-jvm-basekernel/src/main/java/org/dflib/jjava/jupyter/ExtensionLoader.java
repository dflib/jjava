package org.dflib.jjava.jupyter;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class is responsible for discovering and initializing a set of {@link Extension} instances.
 *
 * <p>Example usage:
 * <pre>
 *     ExtensionLoader loader = new ExtensionLoader();
 *     List&lt;Extension&gt; extensions = loader.loadExtensions(jars);
 *     for (Extension ext : extensions) {
 *         ext.install(kernel);
 *     }
 * </pre>
 */
public final class ExtensionLoader {

    private final Set<String> seenExtensions;

    public ExtensionLoader() {
        this.seenExtensions = new HashSet<>();
    }

    /**
     * Loads available {@code Extension} implementations and initializes them. Calls should manually invoke
     * {@link Extension#install(BaseKernel)} to initialize extension. This method performs discovery of extension
     * classes based on a {@link ServiceLoader} API, using the kernel default ClassLoader
     */
    public List<Extension> loadFromClasspath() {
        return load(getClass().getClassLoader());
    }

    /**
     * Loads available {@code Extension} implementations and initializes them. Calls should manually invoke
     * {@link Extension#install(BaseKernel)} to initialize extension. This method performs discovery of extension
     * classes based on a {@link ServiceLoader} API, but only scanning the specified jars, not the entire classpath.
     *
     * @param jarPaths paths to jar files to scan for extensions
     * @return list of the available extensions
     */
    public List<Extension> loadFromJars(Iterable<String> jarPaths) {
        URL[] urls = StreamSupport.stream(jarPaths.spliterator(), false)
                .map(ExtensionLoader::toURL)
                .toArray(URL[]::new);

        // TODO: using URLClassLoader for scanning META-INF/services may be a security issue. Do we really need it, and
        //  and should we switch to a local ClassLoader
        try (URLClassLoader classLoader = URLClassLoader.newInstance(urls)) {
            return load(classLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Extension> load(ClassLoader classLoader) {
        return ServiceLoader.load(Extension.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)

                // TODO: should we worry about concurrency of "seenExtensions" Set?
                .filter(e -> seenExtensions.add(e.getClass().getName()))
                .collect(Collectors.toList());
    }

    private static URL toURL(String path) {
        try {
            return Path.of(path).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
