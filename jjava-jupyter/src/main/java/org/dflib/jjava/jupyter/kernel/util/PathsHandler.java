package org.dflib.jjava.jupyter.kernel.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper methods to split and join paths and resolve glob patterns.
 */
public class PathsHandler {

    public static String joinStringPaths(List<String> paths) {
        return paths.stream().collect(Collectors.joining(File.pathSeparator));
    }

    public static String joinPaths(List<Path> paths) {
        return paths.stream()
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.joining(File.pathSeparator));
    }

    public static List<String> split(String paths) {
        Objects.requireNonNull(paths, "Null 'paths' String");
        return Arrays.stream(paths.split(File.pathSeparator))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

    public static List<Path> splitAndResolveGlobs(String paths) {

        List<Path> result = new ArrayList<>();

        for (String p : split(paths)) {

            if (p.isBlank()) {
                continue;
            }

            List<Path> subPaths = resolveGlobs(p);

            for (Path entry : subPaths) {
                result.add(entry);
            }
        }

        return result;
    }

    public static List<Path> resolveGlobs(String path) {
        try {
            return new GlobResolver(FileSystems.getDefault(), path).resolve();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error resolving glob '%s': %s", path, e.getMessage()), e);
        }
    }
}
