/*
 * Sonar Runner - Implementation
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
import java.util.List;
import java.util.Properties;

public class BatchLauncher {

  public void execute(Properties properties, List<Object> extensions) {
    FileDownloader fileDownloader = FileDownloader.create(properties);
    ServerVersion serverVersion = new ServerVersion(fileDownloader);
    List<File> jarFiles;
    if (serverVersion.is35Compatible()) {
      jarFiles = new Jars35(fileDownloader, new JarExtractor()).download();
    } else if (serverVersion.is30Compatible()) {
      String workDir = properties.getProperty("sonar.working.directory");
      jarFiles = new Jars30(fileDownloader).download(new File(workDir), new JarExtractor());
    } else {
      throw new IllegalStateException("Sonar " + serverVersion.version()
        + " is not supported. Please upgrade Sonar to version 3.0 or more.");
    }

    String unmaskedPackages = properties.getProperty(InternalProperties.RUNNER_UNMASKED_PACKAGES, "");
    IsolatedClassloader classloader = new IsolatedClassloader(getClass().getClassLoader(), unmaskedPackages.split(":"));
    classloader.addFiles(jarFiles);
    delegateExecution(classloader, properties, extensions);
  }

  private void delegateExecution(IsolatedClassloader classloader, Properties properties, List<Object> extensions) {
    ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(classloader);
      Class<?> launcherClass = classloader.findClass("org.sonar.runner.batch.IsolatedLauncher");
      Method executeMethod = launcherClass.getMethod("execute", Properties.class, List.class);
      Object launcher = launcherClass.newInstance();
      executeMethod.invoke(launcher, properties, extensions);
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
}
