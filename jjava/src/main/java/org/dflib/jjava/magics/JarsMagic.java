package org.dflib.jjava.magics;

import org.dflib.jjava.JJavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.util.GlobFinder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// TODO: this is a subset of ClasspathMagic. The only difference is calling "computeMatchingFiles()" instead of
//  "computeMatchingPaths()". Deprecate in favor of "classpath"?
public class JarsMagic implements LineMagic<List<String>, JJavaKernel> {

    @Override
    public List<String> eval(JJavaKernel kernel, List<String> args) {
        List<String> resolved = args.stream()
                .flatMap(a -> StreamSupport.stream(resolveGlob(a).spliterator(), false))
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.toList());

        kernel.addToClasspath(resolved);
        return resolved;
    }

    private static Iterable<Path> resolveGlob(String jarArg) {
        try {
            return new GlobFinder(jarArg).computeMatchingFiles();
        } catch (IOException e) {
            throw new RuntimeException("Exception resolving jar glob", e);
        }
    }
}
