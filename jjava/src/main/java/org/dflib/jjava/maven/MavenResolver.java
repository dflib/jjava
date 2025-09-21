package org.dflib.jjava.maven;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.dflib.jjava.JavaKernel;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MavenResolver {

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2/";
    private static final String DEFAULT_RESOLVER_NAME = "default";

    /**
     * Ivy artifact coordinates in the form organization#name[#branch];revision.
     */
    private static final Pattern IVY_MRID_PATTERN = Pattern.compile(
            "^(?<organization>[-\\w/._+=]*)#(?<name>[-\\w/._+=]+)(?:#(?<branch>[-\\w/._+=]+))?;(?<revision>[-\\w/._+=,\\[\\]{}():@]+)$"
    );

    private static RemoteRepository mavenRepo(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
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

    private final JavaKernel kernel;
    private final List<RemoteRepository> repositories;
    private final Runtime runtime;

    public MavenResolver(JavaKernel kernel) {

        this.kernel = kernel;
        this.repositories = new ArrayList<>();
        this.runtime = Runtimes.INSTANCE.getRuntime();

        repositories.add(mavenRepo("central", MAVEN_CENTRAL_URL));
    }

    public void addRemoteRepo(String name, String url) {
        if (DEFAULT_RESOLVER_NAME.equals(name)) {
            throw new IllegalArgumentException("Illegal repository name, cannot use '" + DEFAULT_RESOLVER_NAME + "'.");
        }
        repositories.add(mavenRepo(name, url));
    }

    public void loadFromRemoteRepos(List<String> repoUrls, List<String> deps) {

        List<RemoteRepository> repositoriesFrom = new ArrayList<>();
        for (String urlString : repoUrls) {
            try {
                URL url = new URL(urlString);
                repositoriesFrom.add(mavenRepo("from-" + url.getHost(), url.toString()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        for (String dep : deps) {
            try {
                List<String> resolved = resolveDependency(dep, repositoriesFrom).stream()
                        .map(File::getAbsolutePath)
                        .collect(Collectors.toList());

                kernel.addToClasspath(resolved);

            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve dependency: " + dep, e);
            }
        }
    }

    public void loadFromPOM(File pomFile) {

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
                    List<String> resolved = resolveDependency(coordinates, pomRepositories).stream()
                            .map(File::getAbsolutePath)
                            .collect(Collectors.toList());

                    kernel.addToClasspath(resolved);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resolve dependency: " + dep.getManagementKey(), e);
                }
            }
        } catch (Exception e) {
            String message = "Failed to process POM file: " + pomFile;
            if (modelResult != null)
                message += "\n" + StreamSupport.stream(modelResult.getProblems().spliterator(), false)
                        .map(String::valueOf)
                        .collect(Collectors.joining("\n"));
            throw new RuntimeException(message, e);
        }
    }

    // TODO support multiple at once. This is necessary for conflict resolution with multiple overlapping dependencies.
    // TODO support classpath resolution
    private List<File> resolveDependency(String coordinates, List<RemoteRepository> repositories) throws Exception {
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
}
