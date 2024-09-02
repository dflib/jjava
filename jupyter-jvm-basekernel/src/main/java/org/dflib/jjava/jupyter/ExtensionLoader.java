package org.dflib.jjava.jupyter;

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

public final class ExtensionLoader {

    private static ExtensionLoader instance;

    private final Set<String> usedExtensions;

    private ExtensionLoader() {
        usedExtensions = new HashSet<>();
    }

    public Iterable<Extension> getExtensions(Iterable<String> jarPaths) {
        List<URL> jarUrls = new ArrayList<>();
        for (String jarPath : jarPaths) {
            try {
                jarUrls.add(Path.of(jarPath).toUri().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        try (URLClassLoader classLoader = URLClassLoader.newInstance(jarUrls.toArray(new URL[]{}))) {
            return ServiceLoader.load(Extension.class, classLoader).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(extension -> !usedExtensions.contains(extension.getClass().getName()))
                    .peek(extension -> usedExtensions.add(extension.getClass().getName()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        usedExtensions.clear();
    }

    public static ExtensionLoader getInstance() {
        if (instance == null) {
            instance = new ExtensionLoader();
        }
        return instance;
    }
}
