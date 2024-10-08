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
package org.dflib.jjava.magics.dependencies;

import org.apache.maven.building.StringSource;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Maven {
    private static final Pattern MAVEN_VAR_PATTERN = Pattern.compile("\\$\\{(?<name>[^}*])}");

    private static final Maven INSTANCE = new Maven(new Properties(), Collections.emptyMap());

    public static Maven getInstance() {
        return INSTANCE;
    }

    // User provider environment overrides.
    private final Properties properties;
    private final Map<String, String> environment;

    public Maven(Properties properties, Map<String, String> environment) {
        this.properties = properties;
        this.environment = environment;
    }

    private String getProperty(String name, String def) {
        String val = this.environment.get(name);
        if (val != null)
            return val;

        val = System.getProperty(name);
        return val != null ? val : def;
    }

    private String getProperty(String name) {
        return this.getProperty(name, null);
    }

    private String getEnv(String name, String def) {
        String val = this.environment.get(name);
        if (val != null)
            return val;

        val = System.getenv(name);
        return val != null ? val : def;
    }

    private String getEnv(String name) {
        return this.getEnv(name, null);
    }

    public Path getUserSystemHomePath() {
        String home = this.getProperty("user.home");
        return Paths.get(home).toAbsolutePath();
    }

    private String replaceMavenVars(String raw) {
        StringBuilder replaced = new StringBuilder();

        Matcher matcher = MAVEN_VAR_PATTERN.matcher(raw);
        while (matcher.find())
            matcher.appendReplacement(replaced,
                    System.getProperty(matcher.group("name"), ""));

        matcher.appendTail(replaced);

        return replaced.toString();
    }

    // Thanks gradle!

    private Path getUserHomePath() {
        return this.getUserSystemHomePath().resolve(".m2");
    }

    private Path getGlobalHomePath() {
        String envM2Home = this.getEnv("M2_HOME");
        return envM2Home != null
                ? Paths.get(envM2Home).toAbsolutePath() : null;
    }

    private Path getUserSettingsPath() {
        return this.getUserHomePath().resolve("settings.xml");
    }

    private Path getGlobalSettingsPath() {
        Path sysHome = this.getGlobalHomePath();
        return sysHome != null ? sysHome.resolve("conf").resolve("settings.xml") : null;
    }

    private Path getDefaultLocalRepoPath() {
        return this.getUserHomePath().resolve("repository");
    }

    private Path readConfiguredLocalRepositoryPath(Path settingsXmlPath) throws IOException, SAXException {
        if (!Files.isRegularFile(settingsXmlPath))
            return null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // We are configuring the factory, the configuration will be fine...
            e.printStackTrace();
            return null;
        }

        try (InputStream in = Files.newInputStream(settingsXmlPath)) {
            Document settingsDoc = builder.parse(in);
            NodeList settings = settingsDoc.getElementsByTagName("settings");
            if (settings.getLength() == 0)
                return null;

            for (int i = 0; i < settings.getLength(); i++) {
                Node setting = settings.item(i);
                switch (setting.getNodeName()) {
                    case "localRepository":
                        String localRepository = setting.getTextContent();
                        localRepository = this.replaceMavenVars(localRepository);
                        return Paths.get(localRepository);
                }
            }
        }

        return null;
    }

    // TODO just use the effective settings
    public Path getConfiguredLocalRepositoryPath() throws IOException, SAXException {
        Path userSettingsXmlPath = this.getUserSettingsPath();
        Path path = this.readConfiguredLocalRepositoryPath(userSettingsXmlPath);

        if (path == null) {
            Path globalSettingsXmlPath = this.getGlobalSettingsPath();
            if (globalSettingsXmlPath != null)
                path = this.readConfiguredLocalRepositoryPath(globalSettingsXmlPath);
        }

        return path == null ? this.getDefaultLocalRepoPath() : path;
    }

    public ModelBuildingResult readEffectiveModel(CharSequence pom) throws ModelBuildingException {
        return this.readEffectiveModel(req ->
                req.setModelSource((ModelSource) new StringSource(pom))
        );
    }

    public ModelBuildingResult readEffectiveModel(File pom) throws ModelBuildingException {
        return this.readEffectiveModel(req ->
                req.setPomFile(pom)
        );
    }

    private ModelBuildingResult readEffectiveModel(Function<ModelBuildingRequest, ModelBuildingRequest> configuration) throws ModelBuildingException {
        DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();

        ModelBuildingRequest request = new DefaultModelBuildingRequest();

        request.setSystemProperties(System.getProperties());
        request.setUserProperties(this.properties);

        // Allow force selection of active profile
        // request.setActiveProfileIds()
        // request.setInactiveProfileIds()

        // Better error messages for bad poms
        request.setLocationTracking(true);

        // Don't run plugins, in most cases this is what we want. I don't know of any
        // that would affect the POM.
        request.setProcessPlugins(false);

        request = configuration.apply(request);

        return modelBuilder.build(request);
    }
}
