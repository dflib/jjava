package org.dflib.jjava.launcher;

import java.util.Arrays;

public class LauncherMain {

    public static void main(String[] args) {
        KernelLauncher launcher = new KernelLauncher(Arrays.asList(args));
        int exitCode = launcher.launchKernel();
        System.exit(exitCode);
    }
}
