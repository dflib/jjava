/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Spencer Park
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dflib.jjava.magics;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.dflib.jjava.jupyter.Extension;
import org.dflib.jjava.jupyter.ExtensionLoader;
import org.dflib.jjava.jupyter.kernel.magic.registry.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.registry.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.registry.MagicsArgs;
import org.dflib.jjava.magics.dependencies.CommonRepositories;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MavenResolver {

    private static final String DEFAULT_RESOLVER_NAME = "default";

    /**
     * Ivy artifact coordinates in the form organization#name[#branch];revision.
     */
    private static final Pattern IVY_MRID_PATTERN = Pattern.compile(
            "^(?<organization>[-\\w/._+=]*)#(?<name>[-\\w/._+=]+)(?:#(?<branch>[-\\w/._+=]+))?;(?<revision>[-\\w/._+=,\\[\\]{}():@]+)$"
    );

    private final Consumer<String> classPathHandler;
    private final Consumer<Extension> extensionHandler;
    private final ExtensionLoader extensionLoader;
    private final List<RemoteRepository> repositories;
    private final Runtime runtime;

    public MavenResolver(Consumer<String> classPathHandler, Consumer<Extension> extensionHandler) {
        this.classPathHandler = classPathHandler;
        this.extensionHandler = extensionHandler;
        extensionLoader = new ExtensionLoader();
        repositories = new ArrayList<>();
        runtime = Runtimes.INSTANCE.getRuntime();

        repositories.add(CommonRepositories.mavenCentral());
    }

    /**
     * Load extensions provided by the JJava kernel itself (and it's dependencies)
     *
     * @since 1.0-M4
     */
    public void initImplicitExtensions() {
        extensionLoader.loadExtensions().forEach(extensionHandler);
    }

    public void addRemoteRepo(String name, String url) {
        if (DEFAULT_RESOLVER_NAME.equals(name)) {
            throw new IllegalArgumentException("Illegal repository name, cannot use '" + DEFAULT_RESOLVER_NAME + "'.");
        }
        repositories.add(CommonRepositories.maven(name, url));
    }

    // TODO support multiple at once. This is necessary for conflict resolution with multiple overlapping dependencies.
    // TODO support classpath resolution
    public List<File> resolveMavenDependency(String coordinates, List<RemoteRepository> repositories) throws Exception {
        ContextOverrides overrides = ContextOverrides.create()
                .withUserSettings(true)
                .repositories(repositories.isEmpty() ? this.repositories : repositories)
                .build();

        try (Context context = runtime.create(overrides)) {
            Artifact artifact = parseArtifact(coordinates);
            Dependency dependency = new Dependency(artifact, "runtime");

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.setRepositories(context.remoteRepositories());

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setCollectRequest(collectRequest);

            RepositorySystemSession session = context.repositorySystemSession();
            DependencyNode rootNode = context.repositorySystem()
                    .resolveDependencies(session, dependencyRequest)
                    .getRoot();

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            rootNode.accept(nlg);

            String classpath = nlg.getClassPath();
            return Arrays.stream(classpath.split(File.pathSeparator))
                    .map(File::new)
                    .collect(Collectors.toList());
        }
    }

    public void addJarsToClasspath(Iterable<String> resolvedJars) {
        resolvedJars.forEach(classPathHandler);
        loadExtensions(resolvedJars);
    }

    public void loadExtensions(Iterable<String> resolvedJars) {
        extensionLoader
                .loadExtensions(resolvedJars)
                .forEach(extensionHandler);
    }

    private static Artifact parseArtifact(String coordinates) {
        Matcher ivyMatcher = IVY_MRID_PATTERN.matcher(coordinates);
        if (ivyMatcher.matches()) {
            String organization = ivyMatcher.group("organization");
            String name = ivyMatcher.group("name");
            String revision = ivyMatcher.group("revision");
            return new DefaultArtifact(organization, name, "jar", revision);
        }
        return new DefaultArtifact(coordinates);
    }

    private String solidifyPartialPOM(String rawIn) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Wrap in a dummy tag to allow fragments
        InputStream inStream = new SequenceInputStream(Collections.enumeration(Arrays.asList(
                new ByteArrayInputStream("<jjava>".getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(rawIn.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream("</jjava>".getBytes(StandardCharsets.UTF_8))
        )));

        Document doc = builder.parse(inStream);
        NodeList rootChildren = doc.getDocumentElement().getChildNodes();

        // If input was a single "project" tag then we don't touch it. It is assumed
        // to be complete.
        if (rootChildren.getLength() == 1 && "project".equalsIgnoreCase(rootChildren.item(0).getNodeName()))
            return writeDOM(new DOMSource(rootChildren.item(0)));

        // Put the pieces together and fill in the blanks.
        Document fixed = builder.newDocument();

        Node project = fixed.appendChild(fixed.createElement("project"));

        Node dependencies = project.appendChild(fixed.createElement("dependencies"));
        Node repositories = project.appendChild(fixed.createElement("repositories"));

        boolean setModelVersion = false;
        boolean setGroupId = false;
        boolean setArtifactId = false;
        boolean setVersion = false;

        for (int i = 0; i < rootChildren.getLength(); i++) {
            Node child = rootChildren.item(i);

            switch (child.getNodeName()) {
                case "modelVersion":
                    setModelVersion = true;
                    appendChildInNewDoc(child, fixed, project);
                    break;
                case "groupId":
                    setGroupId = true;
                    appendChildInNewDoc(child, fixed, project);
                    break;
                case "artifactId":
                    setArtifactId = true;
                    appendChildInNewDoc(child, fixed, project);
                    break;
                case "version":
                    setVersion = true;
                    appendChildInNewDoc(child, fixed, project);
                    break;
                case "dependency":
                    appendChildInNewDoc(child, fixed, dependencies);
                    break;
                case "repository":
                    appendChildInNewDoc(child, fixed, repositories);
                    break;
                case "dependencies":
                    // Add all dependencies to the collecting tag
                    NodeList dependencyChildren = child.getChildNodes();
                    for (int j = 0; j < dependencyChildren.getLength(); j++)
                        appendChildInNewDoc(dependencyChildren.item(j), fixed, dependencies);
                    break;
                case "repositories":
                    // Add all repositories to the collecting tag
                    NodeList repositoryChildren = child.getChildNodes();
                    for (int j = 0; j < repositoryChildren.getLength(); j++)
                        appendChildInNewDoc(repositoryChildren.item(j), fixed, repositories);
                    break;
                default:
                    appendChildInNewDoc(child, fixed, project);
                    break;
            }
        }

        if (!setModelVersion) {
            Node modelVersion = project.appendChild(fixed.createElement("modelVersion"));
            modelVersion.setTextContent("4.0.0");
        }

        if (!setGroupId) {
            Node groupId = project.appendChild(fixed.createElement("groupId"));
            groupId.setTextContent("jjava.notebook");
        }

        if (!setArtifactId) {
            Node artifactId = project.appendChild(fixed.createElement("artifactId"));
            artifactId.setTextContent("cell");
        }

        if (!setVersion) {
            Node version = project.appendChild(fixed.createElement("version"));
            version.setTextContent("1");
        }

        return writeDOM(new DOMSource(fixed));
    }

    private void appendChildInNewDoc(Node oldNode, Document doc, Node newParent) {
        Node newNode = oldNode.cloneNode(true);
        doc.adoptNode(newNode);
        newParent.appendChild(newNode);
    }

    private String writeDOM(Source src) throws TransformerException {
        Transformer idTransformer = TransformerFactory.newInstance().newTransformer();
        idTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result dest = new StreamResult(out);

        idTransformer.transform(src, dest);

        return out.toString(StandardCharsets.UTF_8);
    }

    @LineMagic(aliases = {"addMavenDependency", "maven"})
    public void addMavenDependencies(List<String> args) {
        MagicsArgs schema = MagicsArgs.builder()
                .varargs("deps")
                .keyword("from")
                .onlyKnownKeywords()
                .onlyKnownFlags()
                .build();

        Map<String, List<String>> vals = schema.parse(args);
        List<String> deps = vals.get("deps");
        List<String> from = vals.get("from");

        List<RemoteRepository> repositoriesFrom = new ArrayList<>();
        for (String urlString : from) {
            try {
                URL url = new URL(urlString);
                repositoriesFrom.add(CommonRepositories.maven("from-" + url.getHost(), url.toString()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        for (String dep : deps) {
            try {
                List<String> resolvedJars = resolveMavenDependency(dep, repositoriesFrom).stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toList());
                addJarsToClasspath(resolvedJars);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve dependency: " + dep, e);
            }
        }
    }

    @LineMagic(aliases = {"mavenRepo"})
    public void addMavenRepo(List<String> args) {
        MagicsArgs schema = MagicsArgs.builder().required("id").required("url").build();
        Map<String, List<String>> vals = schema.parse(args);

        String id = vals.get("id").get(0);
        String url = vals.get("url").get(0);

        addRemoteRepo(id, url);
    }

    @CellMagic
    public void loadFromPOM(List<String> args, String body) throws Exception {
        try {
            File tempPomPath = File.createTempFile("jjava-maven-", ".pom").getAbsoluteFile();
            tempPomPath.deleteOnExit();

            String rawPom = solidifyPartialPOM(body);
            Files.write(tempPomPath.toPath(), rawPom.getBytes(StandardCharsets.UTF_8));

            List<String> loadArgs = new ArrayList<>(args.size() + 1);
            loadArgs.add(tempPomPath.getAbsolutePath());
            loadArgs.addAll(args);

            loadFromPOM(loadArgs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LineMagic
    public void loadFromPOM(List<String> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Loading from POM requires at least the path to the POM file");
        }

        MagicsArgs schema = MagicsArgs.builder()
                .required("pomPath")
                .onlyKnownKeywords().onlyKnownFlags().build();

        Map<String, List<String>> vals = schema.parse(args);

        String pomPath = vals.get("pomPath").get(0);

        File pomFile = new File(pomPath);
        ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        org.apache.maven.model.building.Result<? extends Model> modelResult = null;
        try {
            modelResult = modelBuilder.buildRawModel(pomFile, ModelBuildingRequest.VALIDATION_LEVEL_STRICT, true);
            Model model = modelResult.get();
            List<RemoteRepository> pomRepositories = model.getRepositories().stream()
                    .map(repo -> new RemoteRepository.Builder(repo.getId(), repo.getName(), repo.getUrl()).build())
                    .collect(Collectors.toList());
            for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
                String coordinates = dep.getManagementKey() + ":" + dep.getVersion();
                try {
                    List<String> resolvedJars = resolveMavenDependency(coordinates, pomRepositories).stream()
                            .map(File::getAbsolutePath)
                            .collect(Collectors.toList());
                    addJarsToClasspath(resolvedJars);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resolve dependency: " + dep.getManagementKey(), e);
                }
            }
        } catch (Exception e) {
            String message = "Failed to process POM file: " + pomPath;
            if (modelResult != null)
                message += "\n" + StreamSupport.stream(modelResult.getProblems().spliterator(), false)
                        .map(String::valueOf)
                        .collect(Collectors.joining("\n"));
            throw new RuntimeException(message, e);
        }
    }
}
