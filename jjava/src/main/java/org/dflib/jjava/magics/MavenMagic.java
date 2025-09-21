package org.dflib.jjava.magics;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.maven.MavenResolver;

import java.util.List;
import java.util.Map;

public class MavenMagic implements LineMagic<Void> {

    private final MavenResolver mavenResolver;

    public MavenMagic(MavenResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Void execute(List<String> args) {
        MagicsArgs schema = MagicsArgs.builder()
                .varargs("deps")
                .keyword("from")
                .onlyKnownKeywords()
                .onlyKnownFlags()
                .build();

        Map<String, List<String>> parsed = schema.parse(args);
        mavenResolver.loadFromRemoteRepos(parsed.get("from"), parsed.get("deps"));
        return null;
    }
}
