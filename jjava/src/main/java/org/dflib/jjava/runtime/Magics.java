package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.UndefinedMagicException;

import java.util.List;
import java.util.Objects;

/**
 * Magic-related static methods exposed in each notebook via static imports.
 */
public class Magics {

    public static <T> T lineMagic(String name, List<String> args) {
        JavaKernel kernel = Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running");

        try {
            return kernel.getMagics().evalLineMagic(name, args);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running line magic '%s': %s", name, e.getMessage()), e);
        }
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        JavaKernel kernel = Objects.requireNonNull(JJava.getKernelInstance(), "No JJava kernel running");

        try {
            return kernel.getMagics().evalCellMagic(name, args, body);
        } catch (UndefinedMagicException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Exception occurred while running cell magic '%s': %s", name, e.getMessage()), e);
        }
    }
}
