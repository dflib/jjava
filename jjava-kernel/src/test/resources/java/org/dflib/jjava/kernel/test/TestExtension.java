package org.dflib.jjava.kernel.test;

import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.kernel.JavaNotebookStatics;

public class TestExtension implements Extension {

    @Override
    public void install(BaseKernel kernel) {
        String key = "ext.installs:" + getClass().getName();
        String value = System.getProperty(key, "0");
        try {
            int count = Integer.parseInt(value);
            System.setProperty(key, String.valueOf(count + 1));
        } catch (NumberFormatException e) {
            System.setProperty(key, "1");
        }
    }

    @Override
    public void uninstall(BaseKernel kernel) {
        String key = "ext.installs:" + getClass().getName();
        System.clearProperty(key);
    }
}
