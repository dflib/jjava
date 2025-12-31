package org.dflib.jjava.kernel.magics;

import org.dflib.jjava.kernel.JavaKernel;

import java.util.List;

/**
 * @deprecated in favor of {@link ClasspathMagic}
 */
@Deprecated(since = "1.0", forRemoval = true)
public class JarsMagic extends ClasspathMagic {

    @Override
    public Void eval(JavaKernel kernel, List<String> args) {
        System.err.println("'%jars' magic is deprecated and will be removed in the future versions of JJava. " +
                "A more generic '%classpath' should be used in its place.");
        return super.eval(kernel, args);
    }
}
