package org.dflib.jjava.maven.magics;

import org.dflib.jjava.jupyter.kernel.util.PathsHandler;
import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.maven.MavenDependencyResolver;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoadFromPomLineMagic implements LineMagic<List<String>, JavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public LoadFromPomLineMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public List<String> eval(JavaKernel kernel, List<String> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Loading from POM requires at least the path to the POM file");
        }

        MagicsArgs argsSchema = MagicsArgs.builder()
                .required("pomPath")
                .onlyKnownKeywords()
                .onlyKnownFlags()
                .build();

        Map<String, List<String>> argsParsed = argsSchema.parse(args);
        File pomFile = new File(argsParsed.get("pomPath").get(0));

        List<String> deps = mavenResolver.loadPomDependencies(pomFile)
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        kernel.addToClasspath(PathsHandler.joinStringPaths(deps));
        return deps;
    }
}
