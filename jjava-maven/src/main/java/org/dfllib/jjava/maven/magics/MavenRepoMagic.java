package org.dfllib.jjava.maven.magics;

import org.dflib.jjava.kernel.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dfllib.jjava.maven.MavenDependencyResolver;

import java.util.List;
import java.util.Map;

public class MavenRepoMagic implements LineMagic<Void, JavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public MavenRepoMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Void eval(JavaKernel kernel, List<String> args) {

        MagicsArgs schema = MagicsArgs.builder().required("id").required("url").build();
        Map<String, List<String>> vals = schema.parse(args);

        String id = vals.get("id").get(0);
        String url = vals.get("url").get(0);

        mavenResolver.addRemoteRepo(id, url);

        return null;
    }
}
