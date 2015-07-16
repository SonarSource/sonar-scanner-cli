/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;
import org.sonar.home.cache.Logger;
import org.sonar.home.cache.PersistentCache;
import org.sonar.home.cache.PersistentCacheBuilder;
import org.sonar.runner.batch.IsolatedLauncher;

public class IsolatedLauncherFactory {
  static final String ISOLATED_LAUNCHER_IMPL = "org.sonar.runner.batch.BatchIsolatedLauncher";
  private final TempCleaning tempCleaning;
  private final String launcherImplClassName;
  private final Logger logger;

  /**
   * For unit tests
   */
  IsolatedLauncherFactory(String isolatedLauncherClassName, TempCleaning tempCleaning, Logger logger) {
    this.tempCleaning = tempCleaning;
    this.launcherImplClassName = isolatedLauncherClassName;
    this.logger = logger;
  }

  public IsolatedLauncherFactory(Logger logger) {
    this(ISOLATED_LAUNCHER_IMPL, new TempCleaning(logger), logger);
  }

  private PersistentCache getCache(Properties props) {
    PersistentCacheBuilder builder = new PersistentCacheBuilder(logger);

    if (!"true".equals(props.getProperty("sonar.enableHttpCache"))) {
      builder.forceUpdate(true);
    }

    return builder.build();
  }

  private ClassLoader createClassLoader(List<File> jarFiles) {
    IsolatedClassloader classloader = new IsolatedClassloader(getClass().getClassLoader());
    classloader.addFiles(jarFiles);

    return classloader;
  }

  public IsolatedLauncher createLauncher(Properties props) {
    ServerConnection serverConnection = ServerConnection.create(props, getCache(props), logger);
    JarDownloader jarDownloader = new JarDownloader(serverConnection, logger);

    return createLauncher(jarDownloader);
  }

  IsolatedLauncher createLauncher(final JarDownloader jarDownloader) {
    return AccessController.doPrivileged(new PrivilegedAction<IsolatedLauncher>() {
      @Override
      public IsolatedLauncher run() {
        try {
          List<File> jarFiles = jarDownloader.download();
          logger.debug("Create isolated classloader...");
          ClassLoader cl = createClassLoader(jarFiles);
          IsolatedLauncher objProxy = IsolatedLauncherProxy.create(cl, IsolatedLauncher.class, launcherImplClassName, logger);
          tempCleaning.clean();

          return objProxy;
        } catch (Exception e) {
          // Catch all other exceptions, which relates to reflection
          throw new RunnerException("Unable to execute SonarQube", e);
        }
      }
    });
  }
}
