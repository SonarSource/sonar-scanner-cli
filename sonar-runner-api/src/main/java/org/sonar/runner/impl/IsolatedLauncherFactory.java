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
import java.nio.file.Paths;
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
    String serverUrl = props.getProperty("sonar.host.url");
    String home = props.getProperty("sonar.userHome");

    builder.setAreaForGlobal(serverUrl);
    if (home != null) {
      builder.setSonarHome(Paths.get(home));
    }
    return builder.build();
  }

  private ClassLoader createClassLoader(List<File> jarFiles, ClassloadRules maskRules) {
    IsolatedClassloader classloader = new IsolatedClassloader(getClass().getClassLoader(), maskRules);
    classloader.addFiles(jarFiles);

    return classloader;
  }

  public IsolatedLauncher createLauncher(Properties props, ClassloadRules rules) {
    if (props.containsKey(InternalProperties.RUNNER_DUMP_TO_FILE)) {
      String version = props.getProperty(InternalProperties.RUNNER_VERSION_SIMULATION);
      if (version == null) {
        version = "5.2";
      }
      return new SimulatedLauncher(version, logger);
    }
    ServerConnection serverConnection = ServerConnection.create(props, getCache(props), logger);
    JarDownloader jarDownloader = new JarDownloader(serverConnection, logger, props);

    return createLauncher(jarDownloader, rules);
  }

  IsolatedLauncher createLauncher(final JarDownloader jarDownloader, final ClassloadRules rules) {
    return AccessController.doPrivileged(new PrivilegedAction<IsolatedLauncher>() {
      @Override
      public IsolatedLauncher run() {
        try {
          List<File> jarFiles = jarDownloader.download();
          logger.debug("Create isolated classloader...");
          ClassLoader cl = createClassLoader(jarFiles, rules);
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
