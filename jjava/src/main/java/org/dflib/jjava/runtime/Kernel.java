package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.JavaKernel;

public class Kernel {
    public static JavaKernel getKernelInstance() {
        return JJava.getKernelInstance();
    }

    public static Object eval(String expr) throws Exception {
        JavaKernel kernel = getKernelInstance();

        if (kernel != null) {
            return kernel.evalRaw(expr);
        } else {
            throw new RuntimeException("No JJava kernel running");
        }
    }
}
