package org.dflib.jjava.magics;

import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.maven.MavenResolver;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LoadFromPomLineMagic implements LineMagic<Void> {

    private final MavenResolver mavenResolver;

    public LoadFromPomLineMagic(MavenResolver mavenResolver) {
        this.mavenResolver = mavenResolver;
    }

    @Override
    public Void execute(List<String> args) {
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

        mavenResolver.loadFromPOM(pomFile);
        return null;
    }
}
