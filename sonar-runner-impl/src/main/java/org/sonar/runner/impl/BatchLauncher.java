/*
 * SonarQube Runner - Implementation
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
package org.sonar.runner.impl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;

public class BatchLauncher {
  final String isolatedLauncherClass;
  private final TempCleaning tempCleaning;

  /**
   * For unit tests
   */
  BatchLauncher(String isolatedLauncherClass, TempCleaning tempCleaning) {
    this.isolatedLauncherClass = isolatedLauncherClass;
    this.tempCleaning = tempCleaning;
  }

  public BatchLauncher() {
    this("org.sonar.runner.batch.IsolatedLauncher", new TempCleaning());
  }

  public void execute(Properties props, List<Object> extensions) {
    ServerConnection serverConnection = ServerConnection.create(props);
    ServerVersion serverVersion = new ServerVersion(serverConnection);
    JarDownloader jarDownloader = new JarDownloader(props, serverConnection, serverVersion);
    doExecute(jarDownloader, props, extensions);
  }

  private static String[][] getMaskRules(final Properties props) {
    String maskRulesProp = props.getProperty(InternalProperties.RUNNER_MASK_RULES, null);
    String[] maskRulesConcat = maskRulesProp != null ? maskRulesProp.split(",") : new String[0];
    String[][] maskRules = new String[maskRulesConcat.length][2];
    for (int i = 0; i < maskRulesConcat.length; i++) {
      String[] splitted = maskRulesConcat[i].split("\\|");
      maskRules[i][0] = splitted[0];
      maskRules[i][1] = splitted.length > 1 ? splitted[1] : "";
    }
    return maskRules;
  }

  /**
   * @return the {@link org.sonar.runner.batch.IsolatedLauncher} instance for unit tests
   */
  Object doExecute(final JarDownloader jarDownloader, final Properties props, final List<Object> extensions) {
    Object launcher = AccessController.doPrivileged(new PrivilegedAction<Object>() {
      public Object run() {
        List<File> jarFiles = jarDownloader.download();
        String[][] maskRules = getMaskRules(props);
        IsolatedClassloader classloader = new IsolatedClassloader(getClass().getClassLoader(), maskRules);
        classloader.addFiles(jarFiles);
        Object launcher = delegateExecution(classloader, props, extensions);
        tempCleaning.clean();
        return launcher;
      }

      private Object delegateExecution(IsolatedClassloader classloader, Properties properties, List<Object> extensions) {
        ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
          Thread.currentThread().setContextClassLoader(classloader);
          Class<?> launcherClass = classloader.loadClass(isolatedLauncherClass);
          Method executeMethod = launcherClass.getMethod("execute", Properties.class, List.class);
          Object launcher = launcherClass.newInstance();
          executeMethod.invoke(launcher, properties, extensions);
          return launcher;
        } catch (InvocationTargetException e) {
          // Unwrap original exception
          throw new RunnerException("Unable to execute Sonar", e.getTargetException());
        } catch (Exception e) {
          // Catch all other exceptions, which relates to reflection
          throw new RunnerException("Unable to execute Sonar", e);
        } finally {
          Thread.currentThread().setContextClassLoader(initialContextClassLoader);
        }
      }
    });
    return launcher;
  }

}
