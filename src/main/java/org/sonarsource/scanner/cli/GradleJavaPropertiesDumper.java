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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.gradle.api.JavaVersion;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.idea.IdeaCompilerOutput;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaDependencyScope;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.gradle.tooling.model.java.InstalledJdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.scanner.cli.GradleJavaModel.GeneratedProperties;
import org.sonarsource.scanner.cli.GradleJavaModel.Module;
import org.sonarsource.scanner.cli.GradleJavaModel.Project;

final class GradleJavaPropertiesDumper {
  private static final Logger LOG = LoggerFactory.getLogger(GradleJavaPropertiesDumper.class);

  private static final String SONAR_PROJECT_PROPERTIES = "sonar-project.properties";
  private static final String SONAR_MODULES = "sonar.modules";
  private static final String NO_EMITTABLE_MODULES =
    "No Java source or test directories were found in the Gradle IDEA model.";

  private final Function<Path, IdeaProject> ideaProjectLoader;

  GradleJavaPropertiesDumper() {
    this(GradleJavaPropertiesDumper::loadIdeaProject);
  }

  GradleJavaPropertiesDumper(Function<Path, IdeaProject> ideaProjectLoader) {
    this.ideaProjectLoader = ideaProjectLoader;
  }

  void dump(Path projectRoot) {
    Path targetFile = targetFile(projectRoot);
    if (Files.exists(targetFile)) {
      throw new IllegalStateException("The generated properties target already exists: " + targetFile);
    }

    GeneratedProperties properties = generate(projectRoot, ideaProjectLoader.apply(projectRoot));
    writeFile(targetFile, render(properties));

    for (String warning : properties.getWarnings()) {
      LOG.warn(warning);
    }
    LOG.info("Generated {}", targetFile);
  }

  GeneratedProperties generate(Path projectRoot, IdeaProject ideaProject) {
    return buildProperties(toProject(projectRoot, ideaProject));
  }

  GeneratedProperties buildProperties(Project project) {
    List<Module> sortedModules = new ArrayList<>(project.modules);
    sortedModules.sort(Comparator.comparing(module -> module.gradleProjectPath));

    List<EmittableModule> emittableModules = findEmittableModules(sortedModules);
    if (emittableModules.isEmpty()) {
      throw new IllegalStateException(NO_EMITTABLE_MODULES);
    }

    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    List<String> warnings = new ArrayList<>();
    EmittableModule rootModule = findRootModule(project.rootProjectDir, emittableModules);
    List<EmittableModule> childModules = new ArrayList<>(emittableModules);
    if (rootModule != null) {
      childModules.remove(rootModule);
    }

    properties.put("sonar.projectBaseDir", project.rootProjectDir.toString());
    if (rootModule == null) {
      properties.put("sonar.sources", "");
      properties.put("sonar.tests", "");
    } else {
      addModuleProperties("", rootModule, sortedModules, warnings, properties);
    }
    putRootExclusions(project.rootProjectDir, childModules, properties);

    Map<EmittableModule, String> moduleIds = assignModuleIds(childModules);
    if (!moduleIds.isEmpty()) {
      properties.put(SONAR_MODULES, String.join(",", moduleIds.values()));
      for (EmittableModule module : childModules) {
        addModuleProperties(moduleIds.get(module) + ".", module, sortedModules, warnings, properties);
      }
    }

    return new GeneratedProperties(properties, warnings);
  }

  String render(GeneratedProperties propertyFile) {
    StringBuilder content = new StringBuilder();
    for (Map.Entry<String, String> entry : propertyFile.getProperties().entrySet()) {
      content.append(escapeKey(entry.getKey()))
        .append('=')
        .append(escapeValue(entry.getValue()))
        .append(System.lineSeparator());
    }
    return content.toString();
  }

  Path targetFile(Path projectRoot) {
    return projectRoot.resolve(SONAR_PROJECT_PROPERTIES);
  }

  private static IdeaProject loadIdeaProject(Path projectRoot) {
    GradleConnector connector = GradleConnector.newConnector()
      .forProjectDirectory(projectRoot.toFile());

    try (ProjectConnection connection = connector.connect()) {
      return connection.getModel(IdeaProject.class);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load the Gradle IDEA model from " + projectRoot, e);
    }
  }

  private static void writeFile(Path targetFile, String content) {
    try {
      Files.writeString(targetFile, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write " + targetFile, e);
    }
  }

  private Project toProject(Path projectRoot, IdeaProject ideaProject) {
    List<IdeaModule> sortedModules = new ArrayList<>(ideaProject.getChildren());
    sortedModules.sort(Comparator.comparing(this::getGradleProjectPath));
    IdeaJavaLanguageSettings projectJavaLanguageSettings = findProjectJavaLanguageSettings(ideaProject);

    List<Module> modules = new ArrayList<>();
    for (IdeaModule module : sortedModules) {
      Module javaModule = toModule(module, projectJavaLanguageSettings);
      if (javaModule.hasJavaMetadata()) {
        modules.add(javaModule);
      }
    }
    return new Project(projectRoot.toAbsolutePath().normalize(), modules);
  }

  private Module toModule(IdeaModule module, IdeaJavaLanguageSettings projectJavaLanguageSettings) {
    List<Path> sourceDirs = new ArrayList<>();
    List<Path> testDirs = new ArrayList<>();
    for (IdeaContentRoot contentRoot : module.getContentRoots()) {
      addDirectories(sourceDirs, contentRoot.getSourceDirectories());
      addDirectories(testDirs, contentRoot.getTestDirectories());
    }

    List<Path> mainLibraries = new ArrayList<>();
    List<Path> testLibraries = new ArrayList<>();
    List<String> moduleDependencies = new ArrayList<>();
    for (IdeaDependency dependency : module.getDependencies()) {
      if (dependency instanceof IdeaSingleEntryLibraryDependency) {
        classifyLibraryDependency((IdeaSingleEntryLibraryDependency) dependency, mainLibraries, testLibraries);
      } else if (dependency instanceof IdeaModuleDependency) {
        moduleDependencies.add(((IdeaModuleDependency) dependency).getTargetModuleName());
      }
    }

    IdeaJavaLanguageSettings moduleLanguageSettings = module.getJavaLanguageSettings();
    String sourceVersion = firstNonNull(findSourceVersion(moduleLanguageSettings), findSourceVersion(projectJavaLanguageSettings));
    Path jdkHome = firstNonNull(findJdkHome(moduleLanguageSettings), findJdkHome(projectJavaLanguageSettings));

    return new Module(
      module.getName(),
      getGradleProjectPath(module),
      module.getGradleProject().getProjectDirectory().toPath().toAbsolutePath().normalize(),
      deduplicatePaths(sourceDirs),
      deduplicatePaths(testDirs),
      mainOutputDir(module),
      testOutputDir(module),
      deduplicatePaths(mainLibraries),
      deduplicatePaths(testLibraries),
      deduplicateStrings(moduleDependencies),
      sourceVersion,
      jdkHome);
  }

  private static Path mainOutputDir(IdeaModule module) {
    IdeaCompilerOutput compilerOutput = module.getCompilerOutput();
    if (compilerOutput == null || compilerOutput.getOutputDir() == null) {
      return null;
    }
    return compilerOutput.getOutputDir().toPath().toAbsolutePath().normalize();
  }

  private static Path testOutputDir(IdeaModule module) {
    IdeaCompilerOutput compilerOutput = module.getCompilerOutput();
    if (compilerOutput == null || compilerOutput.getTestOutputDir() == null) {
      return null;
    }
    return compilerOutput.getTestOutputDir().toPath().toAbsolutePath().normalize();
  }

  private void addModuleProperties(String prefix, EmittableModule module, List<Module> allModules,
    List<String> warnings, LinkedHashMap<String, String> properties) {
    Module javaModule = module.module;
    properties.put(prefix + "sonar.projectBaseDir", module.projectDir().toString());
    if (!prefix.isEmpty()) {
      properties.put(prefix + "sonar.exclusions", "");
    }
    putListProperty(prefix + "sonar.sources", module.sourceAndTestPaths.sources, properties);
    putListProperty(prefix + "sonar.tests", module.sourceAndTestPaths.tests, properties);

    putBinaryProperty(prefix + "sonar.java.binaries",
      findExistingBinaryDirs(javaModule.mainOutputDir, javaModule.sourceDirs, javaModule.projectDir),
      javaModule.mainOutputDir, javaModule, warnings, properties, false);
    putBinaryProperty(prefix + "sonar.java.test.binaries",
      findExistingBinaryDirs(javaModule.testOutputDir, javaModule.testDirs, javaModule.projectDir),
      javaModule.testOutputDir, javaModule, warnings, properties, true);

    List<Path> mainLibraries = existingPaths(javaModule.mainLibraries);
    List<Path> testLibraries = mergePaths(mainLibraries, existingPaths(javaModule.testLibraries));
    putListProperty(prefix + "sonar.java.libraries", mainLibraries, properties);
    putListProperty(prefix + "sonar.java.test.libraries", testLibraries, properties);

    if (javaModule.sourceVersion != null) {
      properties.put(prefix + "sonar.java.source", javaModule.sourceVersion);
    }

    Path currentJdk = currentJavaHome();
    if (javaModule.jdkHome != null && !javaModule.jdkHome.equals(currentJdk)) {
      properties.put(prefix + "sonar.java.jdkHome", javaModule.jdkHome.toString());
    }

    warnOnMissingModuleDependencies(javaModule, allModules, warnings);
  }

  private void putBinaryProperty(String key, List<Path> binaryDirs, Path reportedBinaryDir, Module module,
    List<String> warnings, LinkedHashMap<String, String> properties, boolean testBinary) {
    if (!binaryDirs.isEmpty()) {
      putListProperty(key, binaryDirs, properties);
    } else if (reportedBinaryDir != null) {
      warnings.add("The " + binaryLabel(testBinary) + " output directory for module '"
        + module.gradleProjectPath + "' does not exist: " + reportedBinaryDir);
    } else {
      warnings.add("No " + binaryLabel(testBinary) + " output directory was reported or found for module '"
        + module.gradleProjectPath + "'.");
    }
  }

  private void warnOnMissingModuleDependencies(Module module, List<Module> allModules, List<String> warnings) {
    for (String dependencyName : module.moduleDependencies) {
      Module dependency = findByName(allModules, dependencyName);
      if (dependency == null || findExistingBinaryDirs(dependency.mainOutputDir, dependency.sourceDirs, dependency.projectDir).isEmpty()) {
        warnings.add("Module '" + module.gradleProjectPath
          + "' depends on module '" + dependencyName + "', but no production binaries were discovered.");
      }
    }
  }

  private List<EmittableModule> findEmittableModules(List<Module> modules) {
    List<EmittableModule> emittableModules = new ArrayList<>();
    for (Module module : modules) {
      SourceAndTestPaths paths = normalizeSourceAndTestPaths(module);
      if (!paths.sources.isEmpty() || !paths.tests.isEmpty()) {
        emittableModules.add(new EmittableModule(module, paths));
      }
    }
    return emittableModules;
  }

  private static EmittableModule findRootModule(Path rootProjectDir, List<EmittableModule> modules) {
    for (EmittableModule module : modules) {
      if (module.projectDir().equals(rootProjectDir)) {
        return module;
      }
    }
    return null;
  }

  private Map<EmittableModule, String> assignModuleIds(List<EmittableModule> modules) {
    Map<EmittableModule, String> moduleIds = new LinkedHashMap<>();
    Set<String> usedIds = new LinkedHashSet<>();
    for (EmittableModule module : modules) {
      String baseId = sanitizeModuleId(module.gradleProjectPath());
      String moduleId = baseId;
      int index = 2;
      while (!usedIds.add(moduleId)) {
        moduleId = baseId + "_" + index;
        index++;
      }
      moduleIds.put(module, moduleId);
    }
    return moduleIds;
  }

  private static String sanitizeModuleId(String gradleProjectPath) {
    if (gradleProjectPath == null || gradleProjectPath.isEmpty() || ":".equals(gradleProjectPath)) {
      return "root";
    }

    String sanitized = gradleProjectPath.substring(1).replace(':', '_');
    sanitized = sanitized.replaceAll("[^A-Za-z0-9_]", "_");
    if (sanitized.isEmpty()) {
      return "root";
    }
    if (Character.isDigit(sanitized.charAt(0))) {
      return "_" + sanitized;
    }
    return sanitized;
  }

  private static List<Path> existingPaths(List<Path> paths) {
    List<Path> existingPaths = new ArrayList<>();
    for (Path path : paths) {
      if (Files.exists(path)) {
        existingPaths.add(path);
      }
    }
    return existingPaths;
  }

  private List<Path> findExistingBinaryDirs(Path reportedBinaryDir, List<Path> sourceDirs, Path projectDir) {
    Set<Path> binaryDirs = new LinkedHashSet<>();
    if (reportedBinaryDir != null && Files.isDirectory(reportedBinaryDir)) {
      binaryDirs.add(reportedBinaryDir);
    }
    binaryDirs.addAll(inferGradleBinaryDirs(projectDir, sourceDirs));
    return new ArrayList<>(binaryDirs);
  }

  private List<Path> inferGradleBinaryDirs(Path projectDir, List<Path> sourceDirs) {
    List<Path> binaryDirs = new ArrayList<>();
    for (String sourceSetName : findGradleSourceSetNames(projectDir, sourceDirs)) {
      addExistingBinaryDir(binaryDirs, projectDir, "java", sourceSetName);
      addExistingBinaryDir(binaryDirs, projectDir, "kotlin", sourceSetName);
      addExistingBinaryDir(binaryDirs, projectDir, "groovy", sourceSetName);
    }
    return binaryDirs;
  }

  private static Set<String> findGradleSourceSetNames(Path projectDir, List<Path> sourceDirs) {
    Set<String> sourceSetNames = new LinkedHashSet<>();
    for (Path sourceDir : sourceDirs) {
      if (sourceDir.startsWith(projectDir)) {
        Path relativeSourceDir = projectDir.relativize(sourceDir);
        if (relativeSourceDir.getNameCount() >= 3 && "src".equals(relativeSourceDir.getName(0).toString())) {
          sourceSetNames.add(relativeSourceDir.getName(1).toString());
        }
      }
    }
    return sourceSetNames;
  }

  private static void addExistingBinaryDir(List<Path> binaryDirs, Path projectDir, String language, String sourceSetName) {
    Path binaryDir = projectDir.resolve("build/classes").resolve(language).resolve(sourceSetName);
    if (Files.isDirectory(binaryDir)) {
      binaryDirs.add(binaryDir);
    }
  }

  private static List<Path> mergePaths(List<Path> first, List<Path> second) {
    Set<Path> merged = new LinkedHashSet<>(first);
    merged.addAll(second);
    return new ArrayList<>(merged);
  }

  private SourceAndTestPaths normalizeSourceAndTestPaths(Module module) {
    List<Path> tests = normalizePathList(existingPaths(module.testDirs));
    List<Path> sources = normalizePathList(existingPaths(module.sourceDirs));
    sources.removeIf(source -> overlapsAny(source, tests));
    return new SourceAndTestPaths(sources, tests);
  }

  private List<Path> normalizePathList(List<Path> values) {
    List<Path> sortedValues = new ArrayList<>(new LinkedHashSet<>(values));
    sortedValues.sort(Comparator
      .comparingInt(Path::getNameCount)
      .thenComparing(Path::toString));

    List<Path> normalizedValues = new ArrayList<>();
    for (Path value : sortedValues) {
      if (!overlapsAny(value, normalizedValues)) {
        normalizedValues.add(value);
      }
    }
    return normalizedValues;
  }

  private static boolean overlapsAny(Path path, List<Path> candidates) {
    for (Path candidate : candidates) {
      boolean pathsOverlap = path.equals(candidate) || path.startsWith(candidate) || candidate.startsWith(path);
      if (pathsOverlap) {
        return true;
      }
    }
    return false;
  }

  private static void putListProperty(String key, List<Path> values, LinkedHashMap<String, String> properties) {
    if (values.isEmpty()) {
      return;
    }

    List<String> serializedPaths = new ArrayList<>();
    for (Path value : values) {
      serializedPaths.add(value.toString());
    }
    properties.put(key, String.join(",", serializedPaths));
  }

  private static void putRootExclusions(Path rootProjectDir, List<EmittableModule> childModules,
    LinkedHashMap<String, String> properties) {
    List<String> exclusionPatterns = new ArrayList<>();
    for (EmittableModule childModule : childModules) {
      String pattern = toRootExclusionPattern(rootProjectDir, childModule.projectDir());
      if (pattern != null) {
        exclusionPatterns.add(pattern);
      }
    }
    if (!exclusionPatterns.isEmpty()) {
      properties.put("sonar.exclusions", String.join(",", exclusionPatterns));
    }
  }

  private static String toRootExclusionPattern(Path rootProjectDir, Path childProjectDir) {
    if (!childProjectDir.startsWith(rootProjectDir)) {
      return null;
    }

    Path relativePath = rootProjectDir.relativize(childProjectDir);
    if (relativePath.getNameCount() == 0) {
      return null;
    }
    return relativePath.toString().replace('\\', '/') + "/**";
  }

  private static Module findByName(List<Module> modules, String name) {
    for (Module module : modules) {
      if (module.name.equals(name)) {
        return module;
      }
    }
    return null;
  }

  private static String binaryLabel(boolean testBinary) {
    return testBinary ? "test" : "production";
  }

  private static IdeaJavaLanguageSettings findProjectJavaLanguageSettings(IdeaProject ideaProject) {
    try {
      return ideaProject.getJavaLanguageSettings();
    } catch (UnsupportedMethodException e) {
      return null;
    }
  }

  private static void addDirectories(List<Path> target, Iterable<? extends IdeaSourceDirectory> directories) {
    for (IdeaSourceDirectory directory : directories) {
      target.add(directory.getDirectory().toPath().toAbsolutePath().normalize());
    }
  }

  private static void classifyLibraryDependency(IdeaSingleEntryLibraryDependency dependency, List<Path> mainLibraries,
    List<Path> testLibraries) {
    if (dependency.getFile() == null) {
      return;
    }

    Path libraryPath = dependency.getFile().toPath().toAbsolutePath().normalize();
    if (!isLibraryArchive(libraryPath)) {
      return;
    }

    if (isTestScope(dependency.getScope())) {
      testLibraries.add(libraryPath);
    } else {
      mainLibraries.add(libraryPath);
    }
  }

  private static String findSourceVersion(IdeaJavaLanguageSettings javaLanguageSettings) {
    if (javaLanguageSettings == null) {
      return null;
    }

    JavaVersion languageLevel = javaLanguageSettings.getLanguageLevel();
    if (languageLevel != null) {
      return languageLevel.getMajorVersion();
    }

    try {
      JavaVersion targetBytecodeVersion = javaLanguageSettings.getTargetBytecodeVersion();
      return targetBytecodeVersion == null ? null : targetBytecodeVersion.getMajorVersion();
    } catch (UnsupportedMethodException e) {
      return null;
    }
  }

  private static Path findJdkHome(IdeaJavaLanguageSettings javaLanguageSettings) {
    if (javaLanguageSettings == null) {
      return null;
    }

    InstalledJdk jdk = javaLanguageSettings.getJdk();
    if (jdk == null || jdk.getJavaHome() == null) {
      return null;
    }

    return jdk.getJavaHome().toPath().toAbsolutePath().normalize();
  }

  private static boolean isLibraryArchive(Path libraryPath) {
    String fileName = libraryPath.getFileName().toString().toLowerCase(Locale.ENGLISH);
    return fileName.endsWith(".jar") || fileName.endsWith(".zip");
  }

  private static boolean isTestScope(IdeaDependencyScope scope) {
    if (scope == null || scope.getScope() == null) {
      return false;
    }
    return scope.getScope().toUpperCase(Locale.ENGLISH).contains("TEST");
  }

  private String getGradleProjectPath(IdeaModule module) {
    return module.getGradleProject().getProjectIdentifier().getProjectPath();
  }

  private static <T> T firstNonNull(T first, T second) {
    return first != null ? first : second;
  }

  private static List<Path> deduplicatePaths(List<Path> values) {
    return new ArrayList<>(new LinkedHashSet<>(values));
  }

  private static List<String> deduplicateStrings(List<String> values) {
    return new ArrayList<>(new LinkedHashSet<>(values));
  }

  private static Path currentJavaHome() {
    String javaHome = System.getProperty("java.home");
    if (javaHome == null || javaHome.trim().isEmpty()) {
      return null;
    }
    return Path.of(javaHome).toAbsolutePath().normalize();
  }

  private static String escapeKey(String value) {
    return escape(value, true);
  }

  private static String escapeValue(String value) {
    return escape(value, false);
  }

  private static String escape(String value, boolean escapeSpace) {
    StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\':
          escaped.append("\\\\");
          break;
        case '\t':
          escaped.append("\\t");
          break;
        case '\n':
          escaped.append("\\n");
          break;
        case '\r':
          escaped.append("\\r");
          break;
        case '=':
        case ':':
        case '#':
        case '!':
          escaped.append('\\').append(c);
          break;
        case ' ':
          if (escapeSpace || i == 0) {
            escaped.append("\\ ");
          } else {
            escaped.append(c);
          }
          break;
        default:
          if (c < 0x20 || c > 0x7e) {
            escaped.append(String.format(Locale.ENGLISH, "\\u%04X", (int) c));
          } else {
            escaped.append(c);
          }
      }
    }
    return escaped.toString();
  }

  private static final class EmittableModule {
    private final Module module;
    private final SourceAndTestPaths sourceAndTestPaths;

    private EmittableModule(Module module, SourceAndTestPaths sourceAndTestPaths) {
      this.module = module;
      this.sourceAndTestPaths = sourceAndTestPaths;
    }

    private String gradleProjectPath() {
      return module.gradleProjectPath;
    }

    private Path projectDir() {
      return module.projectDir;
    }
  }

  private static final class SourceAndTestPaths {
    private final List<Path> sources;
    private final List<Path> tests;

    private SourceAndTestPaths(List<Path> sources, List<Path> tests) {
      this.sources = sources;
      this.tests = tests;
    }
  }
}
