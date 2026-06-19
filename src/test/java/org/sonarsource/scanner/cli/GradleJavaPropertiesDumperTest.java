/*
 * SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.JavaVersion;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.ProjectIdentifier;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.scanner.cli.GradleJavaModel.GeneratedProperties;
import org.sonarsource.scanner.cli.GradleJavaModel.Module;
import org.sonarsource.scanner.cli.GradleJavaModel.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GradleJavaPropertiesDumperTest {

  private final GradleJavaPropertiesDumper dumper = new GradleJavaPropertiesDumper();

  @Test
  void should_build_properties_for_root_and_child_modules(@TempDir Path rootDir) throws Exception {
    Path rootSources = Files.createDirectories(rootDir.resolve("src/main/java"));
    Path rootTests = Files.createDirectories(rootDir.resolve("src/test/java"));
    Path rootBinaries = Files.createDirectories(rootDir.resolve("build/classes/java/main"));
    Path rootTestBinaries = Files.createDirectories(rootDir.resolve("build/classes/java/test"));
    Path appDir = Files.createDirectories(rootDir.resolve("app"));
    Path appSources = Files.createDirectories(appDir.resolve("src/main/java"));
    Path appTests = Files.createDirectories(appDir.resolve("src/test/java"));
    Path appBinaries = Files.createDirectories(appDir.resolve("build/classes/java/main"));
    Path appTestBinaries = Files.createDirectories(appDir.resolve("build/classes/java/test"));
    Path mainLibrary = Files.createFile(rootDir.resolve("main-lib.jar"));
    Path testLibrary = Files.createFile(rootDir.resolve("test-lib.jar"));

    Map<String, String> properties = build(rootDir,
      module("root", ":", rootDir)
        .sources(rootSources)
        .tests(rootTests)
        .mainOutput(rootBinaries)
        .testOutput(rootTestBinaries)
        .mainLibraries(mainLibrary)
        .testLibraries(testLibrary)
        .sourceVersion("17"),
      module("app", ":app", appDir)
        .sources(appSources)
        .tests(appTests)
        .mainOutput(appBinaries)
        .testOutput(appTestBinaries)
        .mainLibraries(mainLibrary)
        .testLibraries(testLibrary)
        .sourceVersion("17")).getProperties();

    assertThat(properties).containsEntry("sonar.projectBaseDir", rootDir.toString());
    assertThat(properties).containsEntry("sonar.sources", rootSources.toString());
    assertThat(properties).containsEntry("sonar.tests", rootTests.toString());
    assertThat(properties).containsEntry("sonar.java.binaries", rootBinaries.toString());
    assertThat(properties).containsEntry("sonar.java.test.binaries", rootTestBinaries.toString());
    assertThat(properties).containsEntry("sonar.java.libraries", mainLibrary.toString());
    assertThat(properties).containsEntry("sonar.java.test.libraries", mainLibrary + "," + testLibrary);
    assertThat(properties).containsEntry("sonar.exclusions", "app/**");
    assertThat(properties).containsEntry("sonar.modules", "app");
    assertThat(properties).containsEntry("app.sonar.projectBaseDir", appDir.toString());
    assertThat(properties).containsEntry("app.sonar.exclusions", "");
    assertThat(properties).containsEntry("app.sonar.sources", appSources.toString());
    assertThat(properties).containsEntry("app.sonar.tests", appTests.toString());
    assertThat(properties).containsEntry("app.sonar.java.binaries", appBinaries.toString());
    assertThat(properties).containsEntry("app.sonar.java.test.binaries", appTestBinaries.toString());
  }

  @Test
  void should_report_missing_and_inferred_binaries(@TempDir Path rootDir) throws Exception {
    Path appDir = Files.createDirectories(rootDir.resolve("app"));
    Path appSources = Files.createDirectories(appDir.resolve("src/main/java"));
    Path testSources = Files.createDirectories(appDir.resolve("src/integrationTest/java"));
    Path inferredTestBinaries = Files.createDirectories(appDir.resolve("build/classes/java/integrationTest"));

    GeneratedProperties generated = build(appDir,
      module("app", ":app", appDir)
        .sources(appSources)
        .tests(testSources)
        .mainOutput(appDir.resolve("build/classes/java/main"))
        .dependencies("support")
        .sourceVersion("17"));

    assertThat(generated.getProperties()).containsEntry("sonar.java.test.binaries", inferredTestBinaries.toString());
    assertThat(generated.getProperties()).doesNotContainKey("sonar.java.binaries");
    assertThat(generated.getWarnings()).anySatisfy(warning ->
      assertThat(warning).contains("production output directory").contains("does not exist"));
    assertThat(generated.getWarnings()).anySatisfy(warning ->
      assertThat(warning).contains("depends on module 'support'"));
  }

  @Test
  void should_not_warn_about_non_emitted_dependency_with_existing_binaries(@TempDir Path rootDir) throws Exception {
    Path consumerDir = Files.createDirectories(rootDir.resolve("consumer"));
    Path consumerSources = Files.createDirectories(consumerDir.resolve("src/main/java"));
    Path supportDir = Files.createDirectories(rootDir.resolve("support"));
    Path supportBinaries = Files.createDirectories(supportDir.resolve("build/classes/java/main"));

    GeneratedProperties generated = build(rootDir,
      module("consumer", ":consumer", consumerDir)
        .sources(consumerSources)
        .dependencies("support")
        .sourceVersion("17"),
      module("support", ":support", supportDir)
        .mainOutput(supportBinaries)
        .sourceVersion("17"));

    assertThat(generated.getProperties()).containsEntry("consumer.sonar.sources", consumerSources.toString());
    assertThat(generated.getWarnings()).noneMatch(warning -> warning.contains("depends on module 'support'"));
  }

  @Test
  void should_normalize_overlapping_source_and_test_paths(@TempDir Path rootDir) throws Exception {
    Path sourceParent = Files.createDirectories(rootDir.resolve("build/generated/sources/proto"));
    Path testChild = Files.createDirectories(sourceParent.resolve("integrationTest/java"));
    Path broadTest = Files.createDirectories(rootDir.resolve("src/test"));
    Files.createDirectories(broadTest.resolve("java"));

    Map<String, String> properties = build(rootDir,
      module("proto", ":proto", rootDir)
        .sources(sourceParent)
        .tests(testChild, broadTest)
        .sourceVersion("17")).getProperties();

    assertThat(properties).doesNotContainKey("sonar.sources");
    assertThat(properties.get("sonar.tests").split(","))
      .containsExactlyInAnyOrder(broadTest.toString(), testChild.toString());
  }

  @Test
  void should_skip_aggregator_modules_without_sources_or_tests(@TempDir Path rootDir) throws Exception {
    Path containerDir = Files.createDirectories(rootDir.resolve("server/plugins"));
    Path childDir = Files.createDirectories(containerDir.resolve("sonar-xoo-plugin"));
    Path childSources = Files.createDirectories(childDir.resolve("src/main/java"));

    Map<String, String> properties = build(rootDir,
      module("root", ":", rootDir).sourceVersion("17"),
      module("server_plugins", ":server:plugins", containerDir).sourceVersion("17"),
      module("server_plugins_sonar_xoo_plugin", ":server:plugins:sonar-xoo-plugin", childDir)
        .sources(childSources)
        .sourceVersion("17")).getProperties();

    assertThat(properties).containsEntry("sonar.sources", "");
    assertThat(properties).containsEntry("sonar.tests", "");
    assertThat(properties).containsEntry("sonar.exclusions", "server/plugins/sonar-xoo-plugin/**");
    assertThat(properties).containsEntry("sonar.modules", "server_plugins_sonar_xoo_plugin");
    assertThat(properties).containsEntry("server_plugins_sonar_xoo_plugin.sonar.projectBaseDir", childDir.toString());
    assertThat(properties).doesNotContainKeys("server_plugins.sonar.projectBaseDir", "root.sonar.projectBaseDir");
  }

  @Test
  void should_reject_modules_with_no_java_source_or_test_directories(@TempDir Path rootDir) throws Exception {
    Path library = Files.createFile(rootDir.resolve("main-lib.jar"));

    assertThatIllegalStateException()
      .isThrownBy(() -> build(rootDir, module("lib-only", ":lib-only", rootDir).mainLibraries(library).sourceVersion("17")))
      .withMessage("No Java source or test directories were found in the Gradle IDEA model.");
  }

  @Test
  void should_write_generated_file_without_overwriting_existing_file(@TempDir Path rootDir) throws Exception {
    Path sources = Files.createDirectories(rootDir.resolve("src/main/java"));
    IdeaProject ideaProject = ideaProject(rootDir, sources);
    GradleJavaPropertiesDumper fileDumper = new GradleJavaPropertiesDumper(projectRoot -> ideaProject);

    fileDumper.dump(rootDir);

    Path targetFile = rootDir.resolve("sonar-project.properties");
    assertThat(targetFile).content().contains("sonar.sources=" + sources);
    assertThatIllegalStateException()
      .isThrownBy(() -> fileDumper.dump(rootDir))
      .withMessage("The generated properties target already exists: " + targetFile);
  }

  @Test
  void should_inherit_project_java_source_version_when_module_has_no_language_level(@TempDir Path rootDir) throws Exception {
    Path sourceDir = Files.createDirectories(rootDir.resolve("src/main/java"));

    Map<String, String> properties = dumper.generate(rootDir, ideaProject(rootDir, sourceDir)).getProperties();

    assertThat(properties).containsEntry("sonar.java.source", "17");
  }

  @Test
  void should_render_properties_in_stable_order() {
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put("sonar.projectBaseDir", "/tmp/project");
    properties.put("sonar.modules", "app");

    String rendered = dumper.render(new GeneratedProperties(properties, List.of()));

    assertThat(rendered.split(System.lineSeparator()))
      .containsExactly("sonar.projectBaseDir=/tmp/project", "sonar.modules=app");
  }

  private GeneratedProperties build(Path rootDir, ModuleBuilder... modules) {
    List<Module> javaModules = new ArrayList<>();
    for (ModuleBuilder module : modules) {
      javaModules.add(module.build());
    }
    return dumper.buildProperties(new Project(rootDir, javaModules));
  }

  private static ModuleBuilder module(String name, String path, Path projectDir) {
    return new ModuleBuilder(name, path, projectDir);
  }

  private static IdeaProject ideaProject(Path projectDir, Path sourceDir) {
    IdeaProject ideaProject = mock(IdeaProject.class);
    IdeaJavaLanguageSettings projectJavaSettings = mock(IdeaJavaLanguageSettings.class);
    when(projectJavaSettings.getLanguageLevel()).thenReturn(JavaVersion.VERSION_17);
    when(ideaProject.getJavaLanguageSettings()).thenReturn(projectJavaSettings);
    doReturn(domainObjectSet(ideaModule(projectDir, sourceDir))).when(ideaProject).getChildren();
    return ideaProject;
  }

  private static IdeaModule ideaModule(Path projectDir, Path sourceDir) {
    IdeaSourceDirectory ideaSourceDirectory = mock(IdeaSourceDirectory.class);
    when(ideaSourceDirectory.getDirectory()).thenReturn(sourceDir.toFile());

    IdeaContentRoot contentRoot = mock(IdeaContentRoot.class);
    doReturn(domainObjectSet(ideaSourceDirectory)).when(contentRoot).getSourceDirectories();
    when(contentRoot.getTestDirectories()).thenReturn(domainObjectSet());

    ProjectIdentifier projectIdentifier = mock(ProjectIdentifier.class);
    when(projectIdentifier.getProjectPath()).thenReturn(":");

    GradleProject gradleProject = mock(GradleProject.class);
    when(gradleProject.getProjectDirectory()).thenReturn(projectDir.toFile());
    when(gradleProject.getProjectIdentifier()).thenReturn(projectIdentifier);

    IdeaModule module = mock(IdeaModule.class);
    when(module.getName()).thenReturn("root");
    when(module.getGradleProject()).thenReturn(gradleProject);
    doReturn(domainObjectSet(contentRoot)).when(module).getContentRoots();
    when(module.getDependencies()).thenReturn(domainObjectSet());
    return module;
  }

  @SafeVarargs
  private static <T> DomainObjectSet<T> domainObjectSet(T... values) {
    return new TestDomainObjectSet<>(List.of(values));
  }

  private static final class ModuleBuilder {
    private final String name;
    private final String gradleProjectPath;
    private final Path projectDir;
    private List<Path> sourceDirs = List.of();
    private List<Path> testDirs = List.of();
    private Path mainOutputDir;
    private Path testOutputDir;
    private List<Path> mainLibraries = List.of();
    private List<Path> testLibraries = List.of();
    private List<String> moduleDependencies = List.of();
    private String sourceVersion;

    private ModuleBuilder(String name, String gradleProjectPath, Path projectDir) {
      this.name = name;
      this.gradleProjectPath = gradleProjectPath;
      this.projectDir = projectDir;
    }

    private ModuleBuilder sources(Path... paths) {
      sourceDirs = List.of(paths);
      return this;
    }

    private ModuleBuilder tests(Path... paths) {
      testDirs = List.of(paths);
      return this;
    }

    private ModuleBuilder mainOutput(Path path) {
      mainOutputDir = path;
      return this;
    }

    private ModuleBuilder testOutput(Path path) {
      testOutputDir = path;
      return this;
    }

    private ModuleBuilder mainLibraries(Path... paths) {
      mainLibraries = List.of(paths);
      return this;
    }

    private ModuleBuilder testLibraries(Path... paths) {
      testLibraries = List.of(paths);
      return this;
    }

    private ModuleBuilder dependencies(String... names) {
      moduleDependencies = List.of(names);
      return this;
    }

    private ModuleBuilder sourceVersion(String sourceVersion) {
      this.sourceVersion = sourceVersion;
      return this;
    }

    private Module build() {
      return new Module(name, gradleProjectPath, projectDir, sourceDirs, testDirs,
        mainOutputDir, testOutputDir, mainLibraries, testLibraries, moduleDependencies, sourceVersion, null);
    }
  }

  private static final class TestDomainObjectSet<T> extends AbstractSet<T> implements DomainObjectSet<T> {
    private final List<T> values;

    private TestDomainObjectSet(List<T> values) {
      this.values = new ArrayList<>(values);
    }

    @Override
    public List<T> getAll() {
      return values;
    }

    @Override
    public T getAt(int index) throws IndexOutOfBoundsException {
      return values.get(index);
    }

    @Override
    public Iterator<T> iterator() {
      return values.iterator();
    }

    @Override
    public int size() {
      return values.size();
    }
  }
}
