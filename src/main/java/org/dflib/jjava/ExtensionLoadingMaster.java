package org.dflib.jjava;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public final class ExtensionLoadingMaster {

    private static ExtensionLoadingMaster instance;

    private final Set<String> usedExtensions;

    private ExtensionLoadingMaster() {
        usedExtensions = new HashSet<>();
    }

    public Iterable<Extension> getExtensions(String jarPath) {
        try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{Path.of(jarPath).toUri().toURL()})) {
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

    public static ExtensionLoadingMaster getInstance() {
        if (instance == null) {
            instance = new ExtensionLoadingMaster();
        }
        return instance;
    }
}
