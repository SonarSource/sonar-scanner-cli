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

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sonar.batch.bootstrapper.BootstrapClassLoader;
import org.sonar.batch.bootstrapper.BootstrapException;
import org.sonar.batch.bootstrapper.Bootstrapper;
import org.sonar.batch.bootstrapper.BootstrapperIOUtils;

public class Main {

  private static boolean debug = false;

  private File workDir;
  private Properties properties = new Properties();
  private Bootstrapper bootstrapper;

  public static void main(String[] args) {
    log("Sonar Standalone Runner version: " + getRunnerVersion());
    Map<String, String> cmdProps = parseArguments(args);
    new Main().execute(cmdProps);
  }

  public void execute(Map<String, String> cmdProps) {
    String home = System.getProperty("runner.home");
    // Load global configuration
    loadProperties(new File(home + "/conf/sonar-runner.properties"));
    // Load project configuration
    loadProperties(new File(getProjectDir(), "sonar-project.properties"));
    // Load properties from command-line
    for (Map.Entry<String, String> entry : cmdProps.entrySet()) {
      properties.setProperty(entry.getKey(), entry.getValue());
    }

    String serverUrl = properties.getProperty("sonar.host.url", "http://localhost:9000");
    log("Sonar server: " + serverUrl);
    log("Sonar work directory: " + getWorkDir().getAbsolutePath());
    bootstrapper = new Bootstrapper("SonarRunner/" + getRunnerVersion(), serverUrl, getWorkDir());
    checkSonarVersion();
    delegateExecution(createClassLoader());
  }

  /**
   * @return project directory (current directory)
   */
  public File getProjectDir() {
    return new File(".").getAbsoluteFile();
  }

  /**
   * @return work directory, default is ".sonar" in project directory
   */
  public File getWorkDir() {
    if (workDir == null) {
      workDir = new File(getProjectDir(), ".sonar");
    }
    return workDir;
  }

  /**
   * @return global properties, project properties and command-line properties
   */
  public Properties getProperties() {
    return properties;
  }

  public boolean isDebugEnabled() {
    return debug;
  }

  /**
   * Loads {@link Launcher} from specified {@link BootstrapClassLoader} and passes control to it.
   * 
   * @see Launcher#execute()
   */
  private void delegateExecution(BootstrapClassLoader sonarClassLoader) {
    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(sonarClassLoader);
      Class<?> launcherClass = sonarClassLoader.findClass("org.sonar.runner.Launcher");
      Constructor<?> constructor = launcherClass.getConstructor(Main.class);
      Object launcher = constructor.newInstance(this);
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
   * Loads properties from specified file.
   */
  private void loadProperties(File file) {
    InputStream in = null;
    try {
      in = new FileInputStream(file);
      properties.load(in);
    } catch (FileNotFoundException e) {
      throw new BootstrapException(e);
    } catch (IOException e) {
      throw new BootstrapException(e);
    } finally {
      BootstrapperIOUtils.closeQuietly(in);
    }
  }

  private void checkSonarVersion() {
    String serverVersion = bootstrapper.getServerVersion();
    log("Sonar version: " + serverVersion);
    if (isVersionPriorTo2Dot6(serverVersion)) {
      throw new BootstrapException("Sonar " + serverVersion
          + " does not support Standalone Runner. Please upgrade Sonar to version 2.6 or more.");
    }
  }

  private BootstrapClassLoader createClassLoader() {
    URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
    return bootstrapper.createClassLoader(
        new URL[] { url }, // Add JAR with Sonar Runner - it's a Jar which contains this class
        getClass().getClassLoader());
  }

  static boolean isVersionPriorTo2Dot6(String version) {
    return isVersion(version, "1")
        || isVersion(version, "2.0")
        || isVersion(version, "2.1")
        || isVersion(version, "2.2")
        || isVersion(version, "2.3")
        || isVersion(version, "2.4")
        || isVersion(version, "2.5");
  }

  private static boolean isVersion(String version, String prefix) {
    return version.startsWith(prefix + ".") || version.equals(prefix);
  }

  public static String getRunnerVersion() {
    InputStream in = null;
    try {
      in = Main.class.getResourceAsStream("/org/sonar/runner/version.txt");
      Properties props = new Properties();
      props.load(in);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new BootstrapException("Could not load the version information for Sonar Standalone Runner", e);
    } finally {
      BootstrapperIOUtils.closeQuietly(in);
    }
  }

  private static Map<String, String> parseArguments(String[] args) {
    HashMap<String, String> cmdProps = new HashMap<String, String>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("-h".equals(arg) || "--help".equals(arg)) {
        printUsage();
      } else if ("-X".equals(arg) || "--debug".equals(arg)) {
        debug = true;
      } else if ("-D".equals(arg) || "--define".equals(arg)) {
        i++;
        if (i >= args.length) {
          printError("Missing argument for option --define");
        }
        arg = args[i];
        final String key, value;
        int j = arg.indexOf('=');
        if (j == -1) {
          key = arg;
          value = "true";
        } else {
          key = arg.substring(0, j);
          value = arg.substring(j + 1);
        }
        cmdProps.put(key, value);
      } else {
        printError("Unrecognized option: " + arg);
      }
    }
    return cmdProps;
  }

  private static void printUsage() {
    log("");
    log("usage: sonar-runner [options]");
    log("");
    log("Options:");
    log(" -h,--help             Display help information");
    log(" -X,--debug            Produce execution debug output");
    log(" -D,--define <arg>     Define property");
    System.exit(0); // NOSONAR
  }

  private static void printError(String message) {
    log("");
    log(message);
    printUsage();
  }

  private static void log(String message) {
    System.out.println(message); // NOSONAR
  }
}
