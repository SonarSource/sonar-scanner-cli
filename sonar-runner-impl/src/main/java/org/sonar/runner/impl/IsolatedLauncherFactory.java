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

import org.sonar.runner.batch.IsolatedLauncher;
import org.sonar.home.cache.PersistentCacheBuilder;
import org.sonar.home.cache.PersistentCache;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;

public class IsolatedLauncherFactory {
  static final String ISOLATED_LAUNCHER_IMPL = "org.sonar.runner.batch.BatchIsolatedLauncher";
  private final TempCleaning tempCleaning;
  private final String launcherImplClassName;

  /**
   * For unit tests
   */
  IsolatedLauncherFactory(String isolatedLauncherClassName, TempCleaning tempCleaning) {
    this.tempCleaning = tempCleaning;
    this.launcherImplClassName = isolatedLauncherClassName;
  }

  public IsolatedLauncherFactory() {
    this(ISOLATED_LAUNCHER_IMPL, new TempCleaning());
  }

  private static PersistentCache getCache(Properties props) {
    PersistentCacheBuilder builder = new PersistentCacheBuilder();

    if (!"true".equals(props.getProperty("sonar.enableHttpCache"))) {
      builder.forceUpdate(true);
    }

    return builder.build();
  }

  static String[][] getMaskRules(final Properties props) {
    String maskRulesProp = props.getProperty(InternalProperties.RUNNER_MASK_RULES, null);
    String[] maskRulesConcat = maskRulesProp != null ? maskRulesProp.split(",") : (new String[0]);
    String[][] maskRules = new String[maskRulesConcat.length][2];
    for (int i = 0; i < maskRulesConcat.length; i++) {
      String[] splitted = maskRulesConcat[i].split("\\|");
      maskRules[i][0] = splitted[0];
      maskRules[i][1] = splitted.length > 1 ? splitted[1] : "";
    }
    return maskRules;
  }

  private static void addIsolatedLauncherMaskRule(Properties props) {
    String unmask = "UNMASK|org.sonar.runner.batch.IsolatedLauncher";
    String currentRules = (String) props.get(InternalProperties.RUNNER_MASK_RULES);

    if (currentRules == null) {
      props.put(InternalProperties.RUNNER_MASK_RULES, unmask);
    } else {
      props.put(InternalProperties.RUNNER_MASK_RULES, currentRules + "," + unmask);
    }
  }

  private ClassLoader createClassLoader(List<File> jarFiles, final Properties props) {
    Properties copy = new Properties();
    copy.putAll(props);
    addIsolatedLauncherMaskRule(copy);
    String[][] maskRules = getMaskRules(copy);
    IsolatedClassloader classloader = new IsolatedClassloader(getClass().getClassLoader(), maskRules);
    classloader.addFiles(jarFiles);

    return classloader;
  }

  public IsolatedLauncher createLauncher(Properties props) {
    ServerConnection serverConnection = ServerConnection.create(props, getCache(props));
    JarDownloader jarDownloader = new JarDownloader(serverConnection);

    return createLauncher(jarDownloader, props);
  }

  IsolatedLauncher createLauncher(final JarDownloader jarDownloader, final Properties props) {
    return AccessController.doPrivileged(new PrivilegedAction<IsolatedLauncher>() {
      @Override
      public IsolatedLauncher run() {
        try {
          List<File> jarFiles = jarDownloader.download();
          Logs.debug("Create isolated classloader...");
          ClassLoader cl = createClassLoader(jarFiles, props);
          IsolatedLauncher objProxy = IsolatedLauncherProxy.create(cl, IsolatedLauncher.class, launcherImplClassName);
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
