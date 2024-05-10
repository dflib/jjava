package io.github.spencerpark.ijava.execution;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This code is a copy of jdk.jshell.execution.DefaultLoaderDelegate, with an option to unload stored classes
 */
public class IJavaLoaderDelegate implements LoaderDelegate {

    private final RemoteClassLoader loader;
    private final Map<String, Class<?>> klasses = new HashMap<>();

    private static class RemoteClassLoader extends URLClassLoader {

        private final Map<String, RemoteClassLoader.ClassFile> classFiles = new HashMap<>();

        RemoteClassLoader() {
            super(new URL[0]);
        }

        private class ResourceURLStreamHandler extends URLStreamHandler {

            private final String name;

            ResourceURLStreamHandler(String name) {
                this.name = name;
            }

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    private InputStream in;
                    private Map<String, List<String>> fields;
                    private List<String> fieldNames;

                    @Override
                    public void connect() {
                        if (connected) {
                            return;
                        }
                        connected = true;
                        RemoteClassLoader.ClassFile file = classFiles.get(name);
                        in = new ByteArrayInputStream(file.data);
                        fields = new LinkedHashMap<>();
                        fields.put("content-length", List.of(Integer.toString(file.data.length)));
                        Instant instant = new Date(file.timestamp).toInstant();
                        ZonedDateTime time = ZonedDateTime.ofInstant(instant, ZoneId.of("GMT"));
                        String timeStamp = DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
                        fields.put("date", List.of(timeStamp));
                        fields.put("last-modified", List.of(timeStamp));
                        fieldNames = new ArrayList<>(fields.keySet());
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        connect();
                        return in;
                    }

                    @Override
                    public String getHeaderField(String name) {
                        connect();
                        return fields.getOrDefault(name, List.of())
                                .stream()
                                .findFirst()
                                .orElse(null);
                    }

                    @Override
                    public Map<String, List<String>> getHeaderFields() {
                        connect();
                        return fields;
                    }

                    @Override
                    public String getHeaderFieldKey(int n) {
                        return n < fieldNames.size() ? fieldNames.get(n) : null;
                    }

                    @Override
                    public String getHeaderField(int n) {
                        String name = getHeaderFieldKey(n);

                        return name != null ? getHeaderField(name) : null;
                    }

                };
            }
        }

        void declare(String name, byte[] bytes) {
            classFiles.put(toResourceString(name), new RemoteClassLoader.ClassFile(bytes, System.currentTimeMillis()));
        }

        boolean isDeclared(String name) {
            return classFiles.containsKey(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            RemoteClassLoader.ClassFile file = classFiles.get(toResourceString(name));
            if (file == null) {
                return super.findClass(name);
            }
            return super.defineClass(name, file.data, 0, file.data.length, (CodeSource) null);
        }

        @Override
        public URL findResource(String name) {
            URL u = doFindResource(name);
            return u != null ? u : super.findResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            URL u = doFindResource(name);
            Enumeration<URL> sup = super.findResources(name);

            if (u == null) {
                return sup;
            }

            List<URL> result = new ArrayList<>();

            while (sup.hasMoreElements()) {
                result.add(sup.nextElement());
            }

            result.add(u);

            return Collections.enumeration(result);
        }

        private URL doFindResource(String name) {
            if (classFiles.containsKey(name)) {
                try {
                    return URL.of(new URI("jshell", null, "/" + name, null),
                            new RemoteClassLoader.ResourceURLStreamHandler(name));
                } catch (MalformedURLException | URISyntaxException ex) {
                    throw new InternalError(ex);
                }
            }

            return null;
        }

        private String toResourceString(String className) {
            return className.replace('.', '/') + ".class";
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

        private static class ClassFile {
            public final byte[] data;
            public final long timestamp;

            ClassFile(byte[] data, long timestamp) {
                this.data = data;
                this.timestamp = timestamp;
            }

        }
    }

    public IJavaLoaderDelegate() {
        this.loader = new RemoteClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
    }

    @Override
    public void load(ExecutionControl.ClassBytecodes[] cbcs)
            throws ExecutionControl.ClassInstallException {
        boolean[] loaded = new boolean[cbcs.length];
        try {
            for (ExecutionControl.ClassBytecodes cbc : cbcs) {
                loader.declare(cbc.name(), cbc.bytecodes());
            }
            for (int i = 0; i < cbcs.length; ++i) {
                ExecutionControl.ClassBytecodes cbc = cbcs[i];
                Class<?> klass = loadClass(cbc.name());
                loaded[i] = true;
                // Get class loaded to the point of, at least, preparation
                klass.getDeclaredMethods();
            }
        } catch (Throwable ex) {
            throw new ExecutionControl.ClassInstallException("load: " + ex.getMessage(), loaded);
        }
    }

    private Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> klass = loader.loadClass(name);
        klasses.put(name, klass);
        return klass;
    }

    void unloadClass(String name) throws ClassNotFoundException {
        klasses.remove(name);
    }

    @Override
    public void classesRedefined(ExecutionControl.ClassBytecodes[] cbcs) {
        for (ExecutionControl.ClassBytecodes cbc : cbcs) {
            loader.declare(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String cp)
            throws ExecutionControl.InternalException {
        try {
            for (String path : cp.split(File.pathSeparator)) {
                loader.addURL(new File(path).toURI().toURL());
            }
        } catch (Exception ex) {
            throw new ExecutionControl.InternalException(ex.toString());
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> klass = klasses.get(name);
        if (klass == null && loader.isDeclared(name)) {
            // check if it was removed
            klass = loadClass(name);
        }
        if(klass == null) {
            throw new ClassNotFoundException(name + " not found");
        }
        return klass;
    }
}
