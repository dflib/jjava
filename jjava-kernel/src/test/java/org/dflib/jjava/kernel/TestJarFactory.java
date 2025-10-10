package org.dflib.jjava.kernel;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TestJarFactory {

    private static final Map<Set<String>, Path> cache = new HashMap<>();

    private TestJarFactory() {
    }

    static synchronized Path buildJar(String prefix, String... resourcePaths) throws Exception {
        Set<String> cacheKey = Set.of(resourcePaths);
        if (cache.containsKey(cacheKey)) {
            Path cached = cache.get(cacheKey);
            if (Files.exists(cached)) {
                return cached;
            }
        }

        Path tmpDir = Files.createTempDirectory("jjava-test-jar-");
        Path srcDir = tmpDir.resolve("src");
        Path classesDir = tmpDir.resolve("classes");
        Files.createDirectories(srcDir);
        Files.createDirectories(classesDir);

        moveResources(prefix, resourcePaths, srcDir, classesDir);
        compileJava(srcDir, classesDir);
        Path jar = packageJar(tmpDir.resolve("test.jar"), classesDir);

        cache.put(cacheKey, jar);
        return jar;
    }

    private static void moveResources(String prefix, String[] resourcePaths, Path srcDir, Path classesDir) throws IOException {
        for (String resourcePath : resourcePaths) {
            URL url = TestJarFactory.class.getResource("/" + resourcePath);
            if (url == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            String targetRelativePath = resourcePath;
            if (prefix != null && !prefix.isEmpty() && resourcePath.startsWith(prefix)) {
                targetRelativePath = resourcePath.substring(prefix.length());
            }

            Path targetDir = resourcePath.endsWith(".java") ? srcDir : classesDir;
            Path targetPath = targetDir.resolve(targetRelativePath);
            Files.createDirectories(targetPath.getParent());
            try (InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void compileJava(Path srcDir, Path classesDir) throws IOException {
        List<File> srcFiles;
        try (Stream<Path> stream = Files.walk(srcDir)) {
            srcFiles = stream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
        }
        if (srcFiles.isEmpty()) {
            return;
        }

        JavaCompiler compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler());
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesDir.toFile()));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(srcFiles);
            Boolean ok = compiler.getTask(null, fm, diagnostics, null, null, units).call();
            if (!Boolean.TRUE.equals(ok)) {
                throw new IllegalStateException("Compilation failed: " + diagnostics.getDiagnostics());
            }
        }
    }

    private static Path packageJar(Path jarPath, Path classesDir) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeDirToJar(jos, classesDir, classesDir);
        }
        return jarPath;
    }

    private static void writeDirToJar(JarOutputStream jos, Path root, Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                String entryName = root.relativize(path).toString().replace('\\', '/');
                JarEntry entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                Files.copy(path, jos);
                jos.closeEntry();
            }
        }
    }
}
