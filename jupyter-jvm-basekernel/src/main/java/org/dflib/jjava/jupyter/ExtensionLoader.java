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

    private final Set<String> usedExtensions;

    public ExtensionLoader() {
        this.usedExtensions = new HashSet<>();
    }

    /**
     * Loads available {@code Extension} implementations and initializes them.
     * <p>The user of this method should manually call {@link Extension#install(BaseKernel)} method
     * for extension installation.
     * <p>This method performs discovery of extension classes based on a {@link ServiceLoader} API.
     * <p>This method will use classloader that manages JJava runtime to discover extensions
     *
     * @return list of the available extensions
     * @since 1.0-M4
     */
    public List<Extension> loadExtensions() {
        return getExtensions(getClass().getClassLoader());
    }

    /**
     * Loads available {@code Extension} implementations and initializes them.
     * <p>The user of this method should manually call {@link Extension#install(BaseKernel)} method
     * for extension installation.
     * <p>This method performs discovery of extension classes based on a {@link ServiceLoader} API.
     * <p>This method will only scan jars provided by the caller
     *
     * @param jarPaths paths to jar files to scan for extensions
     * @return list of the available extensions
     */
    public List<Extension> loadExtensions(Iterable<String> jarPaths) {
        URL[] urls = StreamSupport.stream(jarPaths.spliterator(), false)
                .map(ExtensionLoader::toURL)
                .toArray(URL[]::new);

        // TODO: using URLClassLoader for scanning META-INF/services may be a security issue. Do we really need it, and
        //  and should we switch to a local ClassLoader
        try (URLClassLoader classLoader = URLClassLoader.newInstance(urls)) {
            return getExtensions(classLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Extension> getExtensions(ClassLoader classLoader) {
        return ServiceLoader.load(Extension.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .filter(extension -> !usedExtensions.contains(extension.getClass().getName()))
                .peek(extension -> usedExtensions.add(extension.getClass().getName()))
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
