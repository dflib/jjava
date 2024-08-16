package org.dflib.jjava.magics;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginBootstrapMaster {

    private static PluginBootstrapMaster instance;

    private final Set<String> usedBootstraps;

    private PluginBootstrapMaster() {
        usedBootstraps = new HashSet<>();
    }

    public Iterable<PluginBootstrap> getBootstraps(String jarPath) {
        try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{Path.of(jarPath).toUri().toURL()})) {
            return ServiceLoader.load(PluginBootstrap.class, classLoader).stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(bootstrap -> !usedBootstraps.contains(bootstrap.getClass().getName()))
                    .peek(bootstrap -> usedBootstraps.add(bootstrap.getClass().getName()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        usedBootstraps.clear();
    }

    public static PluginBootstrapMaster getInstance() {
        if (instance == null) {
            instance = new PluginBootstrapMaster();
        }
        return instance;
    }
}
