package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.kernel.JavaKernel;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ClasspathMagic implements LineMagic<String, JavaKernel> {

    @Override
    public String eval(JavaKernel kernel, List<String> args) {
        String classpath = args.stream()
                .flatMap(a -> PathsHandler.resolveGlobs(a).stream())
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.joining(File.pathSeparator));

        kernel.addToClasspath(classpath);
        return classpath;
    }
}
