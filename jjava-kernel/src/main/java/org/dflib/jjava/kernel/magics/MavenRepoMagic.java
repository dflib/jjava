package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.kernel.JJavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.kernel.maven.MavenDependencyResolver;

import java.util.List;
import java.util.Map;

public class MavenRepoMagic implements LineMagic<Void, JJavaKernel> {

    private final MavenDependencyResolver mavenResolver;

    public MavenRepoMagic(MavenDependencyResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Void eval(JJavaKernel kernel, List<String> args) {

        MagicsArgs schema = MagicsArgs.builder().required("id").required("url").build();
        Map<String, List<String>> vals = schema.parse(args);

        String id = vals.get("id").get(0);
        String url = vals.get("url").get(0);

        mavenResolver.addRemoteRepo(id, url);

        return null;
    }
}
