/*
 * Sonar CLI
 * Copyright (C) 2009 SonarSource
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

package org.sonar.cli;

import org.sonar.batch.bootstrapper.BatchDownloader;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class BootstrapLauncher {

  private static final String LAUNCHER_CLASS_NAME = "org.sonar.cli.Launcher";

  private String[] args;

  public static void main(String[] args) throws Exception {
    new BootstrapLauncher(args).bootstrap();
  }

  public BootstrapLauncher(String[] args) {
    this.args = args;
  }

  public void bootstrap() throws Exception {
    ClassLoader cl = getInitialClassLoader();

    Class launcherClass = cl.loadClass(LAUNCHER_CLASS_NAME);

    Method[] methods = launcherClass.getMethods();
    Method mainMethod = null;

    for (int i = 0; i < methods.length; ++i) {
      if (!("main".equals(methods[i].getName()))) {
        continue;
      }

      int modifiers = methods[i].getModifiers();

      if (!(Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))) {
        continue;
      }

      if (methods[i].getReturnType() != Void.TYPE) {
        continue;
      }

      Class[] paramTypes = methods[i].getParameterTypes();
      if (paramTypes.length != 1) {
        continue;
      }
      if (paramTypes[0] != String[].class) {
        continue;
      }
      mainMethod = methods[i];
      break;
    }

    if (mainMethod == null) {
      throw new NoSuchMethodException(LAUNCHER_CLASS_NAME + ":main(String[] args)");
    }

    Thread.currentThread().setContextClassLoader(cl);

    mainMethod.invoke(launcherClass, new Object[] { this.args });
  }

  private static ClassLoader getInitialClassLoader() throws Exception {
    BatchDownloader downloader = new BatchDownloader("http://localhost:9000"); // TODO hard-coded value
    List<File> files = downloader.downloadBatchFiles(new File("/tmp/sonar-boot"));
    ChildFirstClassLoader classLoader = new ChildFirstClassLoader();
    for (File file : files) {
      System.out.println(file);
      classLoader.addFile(file);
    }
    // Add JAR with Sonar CLI - it's a Jar which contains this class
    classLoader.addURL(BootstrapLauncher.class.getProtectionDomain().getCodeSource().getLocation());
    return classLoader;
  }
}
