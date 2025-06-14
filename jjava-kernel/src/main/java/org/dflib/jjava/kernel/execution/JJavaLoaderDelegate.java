package org.dflib.jjava.kernel.execution;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import org.dflib.jjava.jupyter.kernel.util.PathsHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JJavaLoaderDelegate implements LoaderDelegate {

    private static final String CLASSPATH_PROPERTY = "java.class.path";
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private final Map<String, byte[]> declaredClasses;
    private final Map<String, Class<?>> loadedClasses;
    private final JJavaClassLoader classLoader;

    public JJavaLoaderDelegate() {
        this.declaredClasses = new ConcurrentHashMap<>();
        this.loadedClasses = new ConcurrentHashMap<>();
        this.classLoader = new JJavaClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Override
    public void load(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException {
        boolean[] installed = new boolean[cbcs.length];

        // Must record all defined classes before attempting to load them. Otherwise, classes depending on other,
        // not yet loaded classes, may fail (see https://github.com/dflib/jjava/issues/65)
        for (ExecutionControl.ClassBytecodes cbc : cbcs) {
            declaredClasses.put(cbc.name(), cbc.bytecodes());
        }

        int i = 0;
        for (ExecutionControl.ClassBytecodes cbc : cbcs) {
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
        for (ExecutionControl.ClassBytecodes cbc : cbcs) {
            declaredClasses.put(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String path) throws ExecutionControl.InternalException {
        for (String next : PathsHandler.split(path)) {
            try {
                classLoader.addURL(Path.of(next).toUri().toURL());

                System.setProperty(
                        CLASSPATH_PROPERTY,
                        System.getProperty(CLASSPATH_PROPERTY) + PATH_SEPARATOR + next);

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
        if (klass == null) {
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

    class JJavaClassLoader extends URLClassLoader {

        public JJavaClassLoader() {
            super(new URL[0]);
        }

        // redefine here for access from the parent class. Otherwise, the "protected" method would be inaccessible
        @Override
        protected void addURL(URL url) {
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
