package org.dflib.jjava.runtime;

import org.dflib.jjava.JJava;
import org.dflib.jjava.JavaKernel;
import org.dflib.jjava.jupyter.kernel.magic.registry.UndefinedMagicException;

import java.util.List;

public class Magics {
    public static <T> T lineMagic(String name, List<String> args) {
        JavaKernel kernel = JJava.getKernelInstance();

        if (kernel != null) {
            try {
                return kernel.getMagics().applyLineMagic(name, args);
            } catch (UndefinedMagicException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception occurred while running line magic '%s': %s", name, e.getMessage()), e);
            }
        } else {
            throw new RuntimeException("No JJava kernel running");
        }
    }

    public static <T> T cellMagic(String name, List<String> args, String body) {
        JavaKernel kernel = JJava.getKernelInstance();

        if (kernel != null) {
            try {
                return kernel.getMagics().applyCellMagic(name, args, body);
            } catch (UndefinedMagicException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(String.format("Exception occurred while running cell magic '%s': %s", name, e.getMessage()), e);
            }
        } else {
            throw new RuntimeException("No JJava kernel running");
        }
    }
}
