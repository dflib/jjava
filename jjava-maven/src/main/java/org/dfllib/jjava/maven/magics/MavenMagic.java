package org.dfllib.jjava.maven.magics;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dfllib.jjava.maven.MavenDependencyResolver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MavenMagic implements LineMagic<List<String>, JavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public MavenMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public List<String> eval(JavaKernel kernel, List<String> args) {
        MagicsArgs schema = MagicsArgs.builder()
                .varargs("deps")
                .keyword("from")
                .onlyKnownKeywords()
                .onlyKnownFlags()
                .build();

        Map<String, List<String>> parsed = schema.parse(args);
        List<String> deps = mavenResolver.loadDependencies(parsed.get("from"), parsed.get("deps"))
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        kernel.addToClasspath(PathsHandler.joinStringPaths(deps));

        return deps;
    }
}
