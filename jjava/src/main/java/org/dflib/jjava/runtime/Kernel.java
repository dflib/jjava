package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.JJavaKernel;

import java.util.Objects;

/**
 * Kernel-related static methods exposed in each notebook via static imports.
 */
public class Kernel {

    public static JJavaKernel getKernelInstance() {
        return JJava.getKernelInstance();
    }

    public static Object eval(String expr) {
        return Objects
                .requireNonNull(JJava.getKernelInstance(), "No JJava kernel running")
                .evalRaw(expr);
    }
}
