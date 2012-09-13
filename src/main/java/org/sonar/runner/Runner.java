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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * Sonar Runner class that can be used to launch Sonar analyses.
 * </p>
 * <p>
 * Configuration is all done through properties:
 * </p>
 * <ul>
 * <li>"sonar.projectDir": the base directory of the project to analyse (this can also be passed via the {@link #create(Properties, File)} constructor)</li>
 * <li>"sonar.working.directory": the working directory, which is "${sonar.projectDir}/.sonar" by default.</li>
 * <li>"sonar.verbose": if set to "true", more information is displayed in the log</li>
 * <li>"sonar.environment.information.key" and "sonar.environment.information.version": can be used to overwrite environment information (can also be
 * set via {@link #setEnvironmentInformation(String, String)} method)</li>
 * <li>... plus all the other Sonar and Sonar plugins properties.</li>
 * </ul>
 *
 * @since 1.1
 */
public final class Runner {

  /**
   * Old property used to activate debug level for logging.
   *
   * @deprecated Replaced by sonar.verbose since 1.2
   */
  @Deprecated
  public static final String PROPERTY_OLD_DEBUG_MODE = "runner.debug";

  /**
   * Property used to increase logging information.
   *
   * @since 1.2
   */
  public static final String PROPERTY_VERBOSE = "sonar.verbose";

  /**
   * Property used to specify the working directory for the runner. May be a relative or absolute path.
   *
   * @since 1.4
   */
  public static final String PROPERTY_WORK_DIRECTORY = "sonar.working.directory";

  /**
   * Default value of the working directory.
   */
  public static final String DEF_VALUE_WORK_DIRECTORY = ".sonar";

  /**
   * Property used to specify the base directory of the project to analyse.
   *
   * @since 1.5
   */
  public static final String PROPERTY_SONAR_PROJECT_BASEDIR = "sonar.projectBaseDir";

  /**
   * Property used to specify the name of the tool that will run a Sonar analysis.
   *
   * @since 1.5
   */
  public static final String PROPERTY_ENVIRONMENT_INFORMATION_KEY = "sonar.environment.information.key";

  /**
   * Property used to specify the version of the tool that will run a Sonar analysis.
   *
   * @since 1.5
   */
  public static final String PROPERTY_ENVIRONMENT_INFORMATION_VERSION = "sonar.environment.information.version";

  /**
   * Array of prefixes of versions of Sonar without support of this runner.
   */
  private static final String[] UNSUPPORTED_VERSIONS = {"1", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10"};

  private File projectDir;
  private File workDir;
  private String[] unmaskedPackages;
  private List<Object> containerExtensions = new ArrayList<Object>();
  private Properties properties;

  private Runner(Properties props) {
    this.properties = props;
    this.unmaskedPackages = new String[0];
    // set the default values for the Sonar Runner - they can be overriden with #setEnvironmentInformation
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_KEY, "Runner");
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_VERSION, Version.getVersion());
    // and init the directories
    initDirs();
  }

  /**
   * Creates a Runner based only on the given properties.
   */
  public static Runner create(Properties props) {
    return new Runner(props);
  }

  /**
   * Creates a Runner based only on the properties and with the given base directory.
   */
  public static Runner create(Properties props, File basedir) {
    props.put(PROPERTY_SONAR_PROJECT_BASEDIR, basedir.getAbsolutePath());
    return new Runner(props);
  }

  /**
   * Runs a Sonar analysis.
   */
  public void execute() {
    Bootstrapper bootstrapper = new Bootstrapper("SonarRunner/" + Version.getVersion(), getSonarServerURL(), getWorkDir());
    checkSonarVersion(bootstrapper);
    delegateExecution(createClassLoader(bootstrapper));
  }

  public String getSonarServerURL() {
    return properties.getProperty("sonar.host.url", "http://localhost:9000");
  }

  private void initDirs() {
    String path = properties.getProperty(PROPERTY_SONAR_PROJECT_BASEDIR, ".");
    projectDir = new File(path);
    if (!projectDir.isDirectory()) {
      throw new RunnerException("Project home must be an existing directory: " + path);
    }
    // project home exists: add its absolute path as "sonar.runner.projectDir" property
    properties.put(PROPERTY_SONAR_PROJECT_BASEDIR, projectDir.getAbsolutePath());
    workDir = initWorkDir();
  }

  private File initWorkDir() {
    String customWorkDir = properties.getProperty(PROPERTY_WORK_DIRECTORY);
    if (customWorkDir == null || customWorkDir.trim().length() == 0) {
      return new File(getProjectDir(), DEF_VALUE_WORK_DIRECTORY);
    }
    return defineCustomizedWorkDir(new File(customWorkDir));
  }

  private File defineCustomizedWorkDir(File customWorkDir) {
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(getProjectDir(), customWorkDir.getPath());
  }

  /**
   * @return the project base directory
   */
  public File getProjectDir() {
    return projectDir;
  }

  /**
   * @return work directory, default is ".sonar" in project directory
   */
  public File getWorkDir() {
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
      throw new RunnerException("Sonar " + serverVersion
        + " is not supported. Please upgrade Sonar to version 2.11 or more.");
    }
  }

  private BootstrapClassLoader createClassLoader(Bootstrapper bootstrapper) {
    URL url = getJarPath();
    return bootstrapper.createClassLoader(
      new URL[]{url}, // Add JAR with Sonar Runner - it's a Jar which contains this class
      getClass().getClassLoader(),
      unmaskedPackages);
  }

  /**
   * For unknown reasons <code>getClass().getProtectionDomain().getCodeSource().getLocation()</code> doesn't work under Ant 1.7.0.
   * So this is a workaround.
   *
   * @return Jar which contains this class
   */
  public static URL getJarPath() {
    String pathToClass = "/" + Runner.class.getName().replace('.', '/') + ".class";
    URL url = Runner.class.getResource(pathToClass);
    if (url != null) {
      String path = url.toString();
      String uri = null;
      if (path.startsWith("jar:file:")) {
        int bang = path.indexOf('!');
        uri = path.substring(4, bang);
      } else if (path.startsWith("file:")) {
        int tail = path.indexOf(pathToClass);
        uri = path.substring(0, tail);
      }
      if (uri != null) {
        try {
          return new URL(uri);
        } catch (MalformedURLException e) { // NOSONAR
        }
      }
    }
    return null;
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

  private void delegateExecution(BootstrapClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.runner.batch.Launcher");
      Constructor<?> constructor = launcherClass.getConstructor(Properties.class, List.class);
      Object launcher = constructor.newInstance(getProperties(), containerExtensions);
      Method method = launcherClass.getMethod("execute");
      method.invoke(launcher);
    } catch (InvocationTargetException e) {
      // Unwrap original exception
      throw new RunnerException(e.getTargetException());
    } catch (Exception e) {
      // Catch all other exceptions, which relates to reflection
      throw new RunnerException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }

  /**
   * Allows to overwrite the environment information when Sonar Runner is embedded in a specific tool (for instance, with the Sonar Ant Task).
   *
   * @param key     the key of the tool that embeds Sonar Runner
   * @param version the version of this tool
   */
  public void setEnvironmentInformation(String key, String version) {
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_KEY, key);
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_VERSION, version);
  }

  public void setUnmaskedPackages(String... unmaskedPackages) {
    this.unmaskedPackages = unmaskedPackages;
  }

  public void addContainerExtension(Object extension) {
    containerExtensions.add(extension);
  }

}
