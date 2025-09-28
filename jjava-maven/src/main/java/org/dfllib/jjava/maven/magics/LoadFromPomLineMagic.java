package org.dfllib.jjava.maven.magics;

import org.dflib.jjava.kernel.JJavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dfllib.jjava.maven.MavenDependencyResolver;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LoadFromPomLineMagic implements LineMagic<Map<String, List<String>>, JJavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public LoadFromPomLineMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Map<String, List<String>> eval(JJavaKernel kernel, List<String> args) {
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

        Map<String, List<String>> deps = mavenResolver.loadPomDependencies(pomFile);
        deps.values().forEach(kernel::addToClasspath);
        return deps;
    }
}
