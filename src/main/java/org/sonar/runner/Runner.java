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
package org.sonar.runner;

import org.sonar.runner.utils.SonarRunnerVersion;

import org.sonar.runner.bootstrapper.BootstrapClassLoader;
import org.sonar.runner.bootstrapper.BootstrapException;
import org.sonar.runner.bootstrapper.Bootstrapper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

/**
 * @since 1.1
 */
public final class Runner {

  public static final String PROPERTY_PROJECT_DIR = "sonar.runner.projectDir";
  public static final String PROPERTY_RUNNER_VERSION = "sonar.runner.version";

  /**
   * @deprecated Replaced by sonar.verbose since 1.2
   */
  @Deprecated
  public static final String PROPERTY_OLD_DEBUG_MODE = "runner.debug";

  /**
   * @since 1.2
   */
  public static final String PROPERTY_VERBOSE = "sonar.verbose";

  /**
   * @since 1.4
   */
  public static final String PROPERTY_WORK_DIRECTORY = "sonar.working.directory";
  public static final String DEF_VALUE_WORK_DIRECTORY = ".sonar";

  /**
   * Array of prefixes of versions of Sonar without support of this runner.
   */
  private static final String[] UNSUPPORTED_VERSIONS = {"1", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10"};

  private File projectDir;
  private File workDir;
  private Properties properties;

  private Runner(Properties props) {
    this.properties = props;
    initDirs();
  }

  public static Runner create(Properties props) {
    return new Runner(props);
  }

  public void execute() {
    String sonarRunnerVersion = SonarRunnerVersion.getVersion();
    properties.put(PROPERTY_RUNNER_VERSION, sonarRunnerVersion);
    Bootstrapper bootstrapper = new Bootstrapper("SonarRunner/" + sonarRunnerVersion, getSonarServerURL(), getWorkDir());
    checkSonarVersion(bootstrapper);
    delegateExecution(createClassLoader(bootstrapper));
  }

  protected String getSonarServerURL() {
    return properties.getProperty("sonar.host.url", "http://localhost:9000");
  }

  private void initDirs() {
    String path = properties.getProperty("project.home", ".");
    projectDir = new File(path);
    if (!projectDir.isDirectory() || !projectDir.exists()) {
      throw new IllegalArgumentException("Project home must be an existing directory: " + path);
    }
    // project home exists: add its absolute path as "sonar.runner.projectDir" property
    properties.put(PROPERTY_PROJECT_DIR, projectDir.getAbsolutePath());
    workDir = initWorkDir();
  }

  private File initWorkDir() {
    String customWorkDir = properties.getProperty(PROPERTY_WORK_DIRECTORY);
    if (customWorkDir == null || customWorkDir.trim().length() == 0) {
      return new File(projectDir, DEF_VALUE_WORK_DIRECTORY);
    }
    return defineCustomizedWorkDir(new File(customWorkDir));
  }

  private File defineCustomizedWorkDir(File customWorkDir) {
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(projectDir, customWorkDir.getPath());
  }

  protected File getProjectDir() {
    return projectDir;
  }

  /**
   * @return work directory, default is ".sonar" in project directory
   */
  protected File getWorkDir() {
    return workDir;
  }

  /**
   * @return global properties, project properties and command-line properties
   */
  protected Properties getProperties() {
    return properties;
  }

  protected void checkSonarVersion(Bootstrapper bootstrapper) {
    String serverVersion = bootstrapper.getServerVersion();
    if (isUnsupportedVersion(serverVersion)) {
      throw new BootstrapException("Sonar " + serverVersion
        + " does not support Standalone Runner. Please upgrade Sonar to version 2.6 or more.");
    }
  }

  private BootstrapClassLoader createClassLoader(Bootstrapper bootstrapper) {
    URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
    return bootstrapper.createClassLoader(
        new URL[] {url}, // Add JAR with Sonar Runner - it's a Jar which contains this class
        getClass().getClassLoader());
  }

  static boolean isUnsupportedVersion(String version) {
    for (String unsupportedVersion : UNSUPPORTED_VERSIONS) {
      if (isVersion(version, unsupportedVersion)) {
        return true;
      }
    }
    return false;
  }

  static boolean isVersion(String version, String prefix) {
    return version.startsWith(prefix + ".") || version.equals(prefix);
  }

  /**
   * Loads Launcher class from specified {@link org.sonar.batch.bootstrapper.BootstrapClassLoader} and passes control to it.
   *
   * @see Launcher#execute()
   */
  private void delegateExecution(BootstrapClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.runner.model.Launcher");
      Constructor<?> constructor = launcherClass.getConstructor(Properties.class);
      Object launcher = constructor.newInstance(getProperties());
      Method method = launcherClass.getMethod("execute");
      method.invoke(launcher);
    } catch (InvocationTargetException e) {
      // Unwrap original exception
      throw new BootstrapException(e.getTargetException());
    } catch (Exception e) {
      // Catch all other exceptions, which relates to reflection
      throw new BootstrapException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }
}
