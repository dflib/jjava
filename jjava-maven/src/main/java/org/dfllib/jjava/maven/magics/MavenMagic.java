package org.dfllib.jjava.maven.magics;

import org.dflib.jjava.kernel.JJavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dfllib.jjava.maven.MavenDependencyResolver;

import java.util.List;
import java.util.Map;

public class MavenMagic implements LineMagic<Map<String, List<String>>, JJavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public MavenMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Map<String, List<String>> eval(JJavaKernel kernel, List<String> args) {
        MagicsArgs schema = MagicsArgs.builder()
                .varargs("deps")
                .keyword("from")
                .onlyKnownKeywords()
                .onlyKnownFlags()
                .build();

        Map<String, List<String>> parsed = schema.parse(args);
        Map<String, List<String>> deps = mavenResolver.loadDependencies(parsed.get("from"), parsed.get("deps"));

        deps.values().forEach(kernel::addToClasspath);
        return deps;
    }
}
