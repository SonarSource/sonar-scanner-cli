/*
 * Sonar Standalone Runner
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.runner.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.runner.RunnerException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class that creates a Sonar project definition based on a set of properties.
 * 
 * @since 1.5
 */
public final class SonarProjectBuilder {

  private static final String PROPERTY_SONAR_MODULES = "sonar.modules";
  private static final String PROPERTY_MODULE_FILE = "file";
  private static final String PROPERTY_MODULE_PATH = "path";
  /**
   * @since 1.4
   */
  private static final String PROPERTY_WORK_DIRECTORY = "sonar.working.directory";
  private static final String DEF_VALUE_WORK_DIRECTORY = ".sonar";

  /**
   * Array of all mandatory properties required for a root project.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_ROOT = {"sonar.projectKey", "sonar.projectName", "sonar.projectVersion", "sources"};

  /**
   * Array of all mandatory properties required for a child project.
   */
  private static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {"sonar.projectKey", "sonar.projectName"};

  /**
   * Properties that must not be passed from the parent project to its children.
   */
  private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Lists.newArrayList("sonar.modules", "sonar.projectDescription");

  private File rootBaseDir;
  private Properties properties;

  private SonarProjectBuilder(File baseDir, Properties properties) {
    this.rootBaseDir = baseDir;
    this.properties = properties;
  }

  public static SonarProjectBuilder create(File baseDir, Properties properties) {
    return new SonarProjectBuilder(baseDir, properties);
  }

  public ProjectDefinition generateProjectDefinition() {
    checkMandatoryProperties("root project", properties, MANDATORY_PROPERTIES_FOR_ROOT);
    ProjectDefinition rootProject = defineProject(rootBaseDir, properties);
    defineChildren(rootProject, properties);
    return rootProject;
  }

  private void defineChildren(ProjectDefinition rootProject, Properties properties) {
    if (properties.containsKey(PROPERTY_SONAR_MODULES)) {
      for (String module : getListFromProperty(properties, PROPERTY_SONAR_MODULES)) {
        Properties moduleProps = extractModuleProperties(module, properties);
        ProjectDefinition childProject = null;
        if (moduleProps.containsKey(PROPERTY_MODULE_FILE)) {
          childProject = loadChildProjectFromPropertyFile(rootProject, moduleProps, module);
        } else {
          childProject = loadChildProjectFromProperties(rootProject, moduleProps, module);
        }
        // the child project may have children as well
        defineChildren(childProject, moduleProps);
        // and finally add this child project to its parent
        rootProject.addSubProject(childProject);
      }
    }
  }

  private ProjectDefinition loadChildProjectFromProperties(ProjectDefinition rootProject, Properties childProps, String moduleId) {
    checkMandatoryProperties(moduleId, childProps, MANDATORY_PROPERTIES_FOR_CHILD);
    mergeParentProperties(childProps, rootProject.getProperties());
    File baseDir = null;
    if (childProps.containsKey(PROPERTY_MODULE_PATH)) {
      baseDir = getFileFromPath(childProps.getProperty(PROPERTY_MODULE_PATH), rootProject.getBaseDir());
    } else {
      baseDir = new File(rootProject.getBaseDir(), moduleId);
    }
    if (!baseDir.isDirectory()) {
      throw new RunnerException("The base directory of the module '" + moduleId + "' does not exist: " + baseDir.getAbsolutePath());
    }

    return defineProject(baseDir, childProps);
  }

  private ProjectDefinition loadChildProjectFromPropertyFile(ProjectDefinition rootProject, Properties moduleProps, String moduleId) {
    File propertyFile = getFileFromPath(moduleProps.getProperty(PROPERTY_MODULE_FILE), rootProject.getBaseDir());
    if (!propertyFile.isFile()) {
      throw new RunnerException("The properties file of the module '" + moduleId + "' does not exist: " + propertyFile.getAbsolutePath());
    }
    Properties childProps = new Properties();
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(propertyFile);
      childProps.load(fileInputStream);
    } catch (IOException e) {
      throw new RunnerException("Impossible to read the property file: " + propertyFile.getAbsolutePath(), e);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
    checkMandatoryProperties(moduleId, childProps, MANDATORY_PROPERTIES_FOR_CHILD);
    mergeParentProperties(childProps, rootProject.getProperties());

    return defineProject(propertyFile.getParentFile(), childProps);
  }

  @VisibleForTesting
  protected static void checkMandatoryProperties(String moduleId, Properties props, String[] mandatoryProps) {
    StringBuilder missing = new StringBuilder();
    for (String mandatoryProperty : mandatoryProps) {
      if (!props.containsKey(mandatoryProperty)) {
        if (missing.length() > 0) {
          missing.append(", ");
        }
        missing.append(mandatoryProperty);
      }
    }
    if (missing.length() != 0) {
      throw new RunnerException("You must define the following mandatory properties for '" + moduleId + "': " + missing);
    }
  }

  @VisibleForTesting
  protected static void mergeParentProperties(Properties childProps, Properties parentProps) {
    for (Map.Entry<Object, Object> entry : parentProps.entrySet()) {
      String key = (String) entry.getKey();
      if (!childProps.containsKey(key) && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)) {
        childProps.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @VisibleForTesting
  protected static Properties extractModuleProperties(String module, Properties properties) {
    Properties moduleProps = new Properties();
    String propertyPrefix = module + ".";
    int prefixLength = propertyPrefix.length();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      if (key.startsWith(propertyPrefix)) {
        moduleProps.put(key.substring(prefixLength), entry.getValue());
      }
    }
    return moduleProps;
  }

  private ProjectDefinition defineProject(File baseDir, Properties properties) {
    ProjectDefinition definition = ProjectDefinition.create((Properties) properties.clone())
        .setBaseDir(baseDir)
        .setWorkDir(initWorkDir(baseDir));
    if (!properties.containsKey(PROPERTY_SONAR_MODULES)) {
      // this is not a "aggregator" project, so let's specify its sources, tests, ...
      String[] sourceDirs = getListFromProperty(properties, "sources");
      checkExistenceOfDirectories(definition.getKey(), baseDir, sourceDirs);
      definition.addSourceDirs(sourceDirs);
      definition.addTestDirs(getListFromProperty(properties, "tests"));
      for (String dir : getListFromProperty(properties, "binaries")) {
        definition.addBinaryDir(dir);
      }
      for (String pattern : getListFromProperty(properties, "libraries")) {
        for (File file : getLibraries(baseDir, pattern)) {
          definition.addLibrary(file.getAbsolutePath());
        }
      }
    }
    return definition;
  }

  @VisibleForTesting
  protected void checkExistenceOfDirectories(String projectKey, File baseDir, String[] sourceDirs) {
    for (String path : sourceDirs) {
      File sourceFolder = getFileFromPath(path, baseDir);
      if (!sourceFolder.isDirectory()) {
        throw new RunnerException("The source folder '" + path + "' does not exist for '" + projectKey +
          "' project/module (base directory = " + baseDir.getAbsolutePath() + ")");
      }
    }

  }

  @VisibleForTesting
  protected File initWorkDir(File baseDir) {
    String workDir = properties.getProperty(PROPERTY_WORK_DIRECTORY);
    if (StringUtils.isBlank(workDir)) {
      return new File(baseDir, DEF_VALUE_WORK_DIRECTORY);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(baseDir, customWorkDir.getPath());
  }

  /**
   * Returns files matching specified pattern.
   */
  @VisibleForTesting
  protected static File[] getLibraries(File baseDir, String pattern) {
    final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
    final String dirPath, filePattern;
    if (i == -1) {
      dirPath = ".";
      filePattern = pattern;
    } else {
      dirPath = pattern.substring(0, i);
      filePattern = pattern.substring(i + 1);
    }
    FileFilter fileFilter = new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(filePattern));
    File dir = resolvePath(baseDir, dirPath);
    File[] files = dir.listFiles(fileFilter);
    if (files == null || files.length == 0) {
      throw new RunnerException("No files matching pattern \"" + filePattern + "\" in directory \"" + dir + "\"");
    }
    return files;
  }

  private static File resolvePath(File baseDir, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(baseDir, path).getCanonicalFile();
      } catch (IOException e) {
        throw new RunnerException("Unable to resolve path \"" + path + "\"", e);
      }
    }
    return file;
  }

  /**
   * Returns a list of comma-separated values, even if they are separated by whitespace characters (space char, EOL, ...)
   */
  @VisibleForTesting
  protected static String[] getListFromProperty(Properties properties, String key) {
    return StringUtils.stripAll(StringUtils.split(properties.getProperty(key, ""), ','));
  }

  /**
   * Returns the file denoted by the given path, may this path be relative to "baseDir" or absolute.
   */
  @VisibleForTesting
  protected static File getFileFromPath(String path, File baseDir) {
    File propertyFile = new File(path.trim());
    if (!propertyFile.isAbsolute()) {
      propertyFile = new File(baseDir, propertyFile.getPath());
    }
    return propertyFile;
  }

}
