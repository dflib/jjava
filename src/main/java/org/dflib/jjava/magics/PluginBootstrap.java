package org.dflib.jjava.magics;

import java.util.Collections;
import java.util.List;

public interface PluginBootstrap {

    default List<String> getImports() {
        return Collections.emptyList();
    }

    default List<String> getStaticImports() {
        return Collections.emptyList();
    }

    default String getBootstrapScript() {
        StringBuilder builder = new StringBuilder();
        getImports().forEach(imp -> builder.append("import ").append(imp).append(";\n"));
        getStaticImports().forEach(imp -> builder.append("import static ").append(imp).append(";\n"));
        return builder.toString();
    }
}
