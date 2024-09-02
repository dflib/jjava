package org.dflib.jjava.jupyter;

import org.dflib.jjava.jupyter.kernel.BaseKernel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for discovering and initializing
 * a set of {@link Extension} instances.
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
        usedExtensions = new HashSet<>();
    }

    /**
     * Loads available {@code Extension} implementations and initializes them.
     * <p>The user of this method should manually call {@link Extension#install(BaseKernel)} method
     * for extension installation.
     * <p>This method performs discovery of extension classes based on a {@link ServiceLoader} API.
     *
     *
     * @param jarPaths paths to jar files to scan for extensions
     * @return list of the available extensions
     */
    public List<Extension> loadExtensions(Iterable<String> jarPaths) {
        List<URL> jarUrls = new ArrayList<>();
        for (String jarPath : jarPaths) {
            try {
                jarUrls.add(Path.of(jarPath).toUri().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        try (URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls.toArray(new URL[0]))) {
            return ServiceLoader.load(Extension.class, classLoader).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(extension -> !usedExtensions.contains(extension.getClass().getName()))
                    .peek(extension -> usedExtensions.add(extension.getClass().getName()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
