package org.dflib.jjava.jupyter;

import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;

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
     * Locates and loads {@code Extension} implementations. Extension classes discovery is performed with
     * {@link ServiceLoader}, using the kernel's default ClassLoader. Callers should manually invoke
     * {@link Extension#install(BaseKernel)} to initialize each extension.
     *
     * @return a list of the discovered extensions
     */
    public List<Extension> loadFromDefaultClasspath() {
        return load(getClass().getClassLoader());
    }

    /**
     * Locates and loads {@code Extension} implementations. Extension classes discovery is performed with
     * {@link ServiceLoader}, only scanning the locations present in the classpath argument, not the entire classpath.
     * Callers should invoke {@link Extension#install(BaseKernel)} to initialize each extension.
     *
     * @param classpath one or more filesystem paths separated by {@link java.io.File#pathSeparator}.
     * @return a list of the discovered extensions
     */
    public List<Extension> loadFromClasspath(String classpath) {

        URL[] urls = PathsHandler.split(classpath)
                .stream()
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
