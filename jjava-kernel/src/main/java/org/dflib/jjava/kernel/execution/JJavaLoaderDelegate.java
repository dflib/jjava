package org.dflib.jjava.kernel.execution;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

public class JJavaLoaderDelegate implements LoaderDelegate {

    private static final String CLASSPATH_PROPERTY = "java.class.path";

    private final Map<String, byte[]> declaredClasses;
    private final Map<String, Class<?>> loadedClasses;
    private final BytecodeClassLoader classLoader;

    public JJavaLoaderDelegate() {
        this.declaredClasses = new HashMap<>();
        this.loadedClasses = new HashMap<>();
        this.classLoader = new BytecodeClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Override
    public void load(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException {
        boolean[] installed = new boolean[cbcs.length];
        int i = 0;
        for(var cbc: cbcs) {
            declaredClasses.put(cbc.name(), cbc.bytecodes());
            try {
                Class<?> loaderClass = classLoader.findClass(cbc.name());
                loadedClasses.put(cbc.name(), loaderClass);
            } catch (ClassNotFoundException e) {
                throw new ExecutionControl.ClassInstallException("Unable to load class " + cbc.name()
                        + ": " + e.getMessage(), installed);
            }
            installed[i++] = true;
        }
    }

    @Override
    public void classesRedefined(ExecutionControl.ClassBytecodes[] cbcs) {
        for(var cbc: cbcs) {
            declaredClasses.put(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String path) throws ExecutionControl.InternalException {
        for (String next : PathsHandler.split(path)) {
            try {
                classLoader.addURL(Path.of(next).toUri().toURL());

                String classpath = System.getProperty(CLASSPATH_PROPERTY);
                classpath += System.lineSeparator() + path;
                System.setProperty(CLASSPATH_PROPERTY, classpath);
            } catch (MalformedURLException e) {
                throw new ExecutionControl.InternalException("Unable to resolve classpath " + next
                        + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> klass = loadedClasses.get(name);
        if (klass == null && declaredClasses.containsKey(name)) {
            // check if it was removed
            klass = loadClass(name);
        }
        if(klass == null) {
            throw new ClassNotFoundException(name + " not found");
        }
        return klass;
    }

    private Class<?> loadClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    public void unloadClass(String name) {
        loadedClasses.remove(name);
        declaredClasses.remove(name);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    class BytecodeClassLoader extends URLClassLoader {

        public BytecodeClassLoader() {
            super(new URL[0]);
        }

        public void addURL(URL url) {
            super.addURL(url);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] data = declaredClasses.get(name);
            if (data == null) {
                return super.findClass(name);
            }
            try {
                return super.defineClass(name, data, 0, data.length, (CodeSource) null);
            } catch (LinkageError er) {
                // rethrow as ClassNotFoundException to let the caller properly handle this case
                // this error could be thrown in some cases (like static method signature change)
                throw new ClassNotFoundException(name, er);
            }
        }
    }
}
