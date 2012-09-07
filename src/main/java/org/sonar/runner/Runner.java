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

import org.sonar.runner.bootstrapper.BootstrapClassLoader;
import org.sonar.runner.bootstrapper.BootstrapException;
import org.sonar.runner.bootstrapper.Bootstrapper;
import org.sonar.runner.utils.SonarRunnerVersion;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
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
  public static final String PROPERTY_PROJECT_DIR = "sonar.projectDir";

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
  private Properties properties;

  private Runner(Properties props) {
    this.properties = props;
    // set the default values for the Sonar Runner - they can be overriden with #setEnvironmentInformation
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_KEY, "Runner");
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_VERSION, SonarRunnerVersion.getVersion());
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
    props.put(PROPERTY_PROJECT_DIR, basedir.getAbsolutePath());
    return new Runner(props);
  }

  /**
   * Runs a Sonar analysis.
   */
  public void execute() {
    Bootstrapper bootstrapper = new Bootstrapper("SonarRunner/" + SonarRunnerVersion.getVersion(), getSonarServerURL(), getWorkDir());
    checkSonarVersion(bootstrapper);
    delegateExecution(createClassLoader(bootstrapper));
  }

  protected String getSonarServerURL() {
    return properties.getProperty("sonar.host.url", "http://localhost:9000");
  }

  private void initDirs() {
    String path = properties.getProperty(PROPERTY_PROJECT_DIR, ".");
    projectDir = new File(path);
    if (!projectDir.isDirectory()) {
      throw new RunnerException("Project home must be an existing directory: " + path);
    }
    // project home exists: add its absolute path as "sonar.runner.projectDir" property
    properties.put(PROPERTY_PROJECT_DIR, projectDir.getAbsolutePath());
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
        + " does not support Standalone Runner. Please upgrade Sonar to version 2.11 or more.");
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

  /**
   * Allows to overwrite the environment information when Sonar Runner is embedded in a specific tool (for instance, with the Sonar Ant Task).
   * 
   * @param key the key of the tool that embeds Sonar Runner
   * @param version the version of this tool
   */
  public void setEnvironmentInformation(String key, String version) {
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_KEY, key);
    this.properties.put(PROPERTY_ENVIRONMENT_INFORMATION_VERSION, version);
  }
}
