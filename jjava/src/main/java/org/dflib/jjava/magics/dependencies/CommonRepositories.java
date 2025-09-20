package org.dflib.jjava.magics.dependencies;

import org.eclipse.aether.repository.RemoteRepository;

public class CommonRepositories {

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";

    public static RemoteRepository maven(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    public static RemoteRepository mavenCentral() {
        return maven("central", MAVEN_CENTRAL_URL);
    }
}
