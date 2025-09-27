package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.JJavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;

import java.util.List;
import java.util.Objects;

/**
 * Magic-related static methods exposed in each notebook via static imports.
 */
public class Magics {

    public static <T> T lineMagic(String name, List<String> args) {
        JJavaKernel kernel = Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running");

        try {
            return kernel.getMagicsRegistry().evalLineMagic(kernel, name, args);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running line magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        JJavaKernel kernel = Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running");

        try {
            return kernel.getMagicsRegistry().evalCellMagic(kernel, name, args, body);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running cell magic '%s': %s", name, e.getMessage()), e);
        }
    }
}
