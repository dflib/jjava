package org.dfllib.jjava.maven.magics;

import org.dflib.jjava.kernel.JavaKernel;
import org.dfllib.jjava.maven.MavenDependencyResolver;

import java.util.List;

/**
 * @deprecated in favor of {@link MavenMagic}
 */
@Deprecated(since = "1.0", forRemoval = true)
public class AddMavenDependencyMagic extends MavenMagic {

    public AddMavenDependencyMagic(MavenDependencyResolver mavenResolver) {
        super(mavenResolver);
    }

    @Override
    public List<String> eval(JavaKernel kernel, List<String> args) {
        System.err.println("'%addMavenDependency' magic is deprecated and will be removed in the future versions of JJava. " +
                "'%maven' magic should be used in its place.");
        return super.eval(kernel, args);
    }
}
