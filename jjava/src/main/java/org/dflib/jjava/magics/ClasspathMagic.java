package org.dflib.jjava.magics;

import org.dflib.jjava.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.util.GlobFinder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ClasspathMagic implements LineMagic<List<String>> {

    private final JavaKernel kernel;

    public ClasspathMagic(JavaKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public List<String> execute(List<String> args) {
        List<String> resolved = args.stream()
                .flatMap(a -> StreamSupport.stream(resolveGlob(a).spliterator(), false))
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.toList());
        kernel.addToClasspath(resolved);
        return resolved;
    }

    private static Iterable<Path> resolveGlob(String jarArg) {
        try {
            return new GlobFinder(jarArg).computeMatchingPaths();
        } catch (IOException e) {
            throw new RuntimeException("Exception resolving jar glob", e);
        }
    }
}
