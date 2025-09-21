package org.dflib.jjava.magics;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.maven.MavenResolver;

import java.util.List;
import java.util.Map;

public class MavenRepoMagic implements LineMagic<Void> {

    private final MavenResolver mavenResolver;

    public MavenRepoMagic(MavenResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Void execute(List<String> args) {
        MagicsArgs schema = MagicsArgs.builder().required("id").required("url").build();
        Map<String, List<String>> vals = schema.parse(args);

        String id = vals.get("id").get(0);
        String url = vals.get("url").get(0);

        mavenResolver.addRemoteRepo(id, url);

        return null;
    }
}
