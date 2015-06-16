/*
 * SonarQube Runner - API
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
package org.sonar.runner.api;

import org.sonar.home.log.LogListener;

import org.sonar.runner.impl.Logs;
import org.sonar.runner.batch.IsolatedLauncher;
import org.sonar.runner.impl.IsolatedLauncherFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Implementation of {@link Runner} that is executed in the same JVM. The application can inject
 * some extensions into Sonar IoC container (see {@link #addExtensions(Object...)}. It can be
 * used for example in the Maven Sonar plugin to register Maven components like MavenProject
 * or MavenPluginExecutor.
 * @since 2.2
 */
public class EmbeddedRunner extends Runner<EmbeddedRunner> {
  private final IsolatedLauncherFactory launcherFactory;
  private IsolatedLauncher launcher;
  private String sqVersion;
  private final List<Object> extensions = new ArrayList<Object>();
  private static final String MASK_RULES_PROP = "sonarRunner.maskRules";

  EmbeddedRunner(IsolatedLauncherFactory bl) {
    this.launcherFactory = bl;
  }

  /**
   * Create a new instance.
   */
  public static EmbeddedRunner create() {
    return new EmbeddedRunner(new IsolatedLauncherFactory());
  }

  public static EmbeddedRunner create(LogListener logListener) {
    Logs.setListener(logListener);
    return new EmbeddedRunner(new IsolatedLauncherFactory());
  }

  /**
   * Sonar is executed in an almost fully isolated classloader (mask everything by default). This method allows to unmask some classes based on
   * a prefix of their fully qualified name. It is related to the extensions provided by {@link #addExtensions(Object...)}.
   * Complex mask/unmask rules can be defined by chaining several ordered calls to {@link #unmask(String)} and {@link #mask(String)}.
   * Registered mask/unmask rules are considered in their registration order and as soon as a matching prefix is found
   * the class is masked/unmasked accordingly.
   * If no matching prefix can be found then by default class is masked.
   */
  public EmbeddedRunner unmask(String fqcnPrefix) {
    return addMaskRule("UNMASK", fqcnPrefix);
  }

  /**
   * @see EmbeddedRunner#unmask(String)
   */
  public EmbeddedRunner mask(String fqcnPrefix) {
    return addMaskRule("MASK", fqcnPrefix);
  }

  private EmbeddedRunner addMaskRule(String type, String fqcnPrefix) {
    String existingRules = globalProperty(MASK_RULES_PROP, "");
    if (!"".equals(existingRules)) {
      existingRules += ",";
    }
    existingRules += type + "|" + fqcnPrefix;
    return setGlobalProperty(MASK_RULES_PROP, existingRules);
  }

  /**
   * @deprecated since 2.3 use {@link #unmask(String)}
   */
  @Deprecated
  public EmbeddedRunner setUnmaskedPackages(String... packages) {
    for (String packagePrefix : packages) {
      unmask(packagePrefix + ".");
    }
    return this;
  }

  public EmbeddedRunner addExtensions(Object... objects) {
    extensions.addAll(Arrays.asList(objects));
    return this;
  }

  List<Object> extensions() {
    return extensions;
  }

  @Override
  protected void doStart() {
    launcher = launcherFactory.createLauncher(globalProperties());
    if (Utils.isAtLeast52(launcher.getVersion())) {
      launcher.start(globalProperties(), extensions, Logs.getListener());
    }
  }

  @Override
  protected void doStop() {
    if (Utils.isAtLeast52(launcher.getVersion())) {
      launcher.stop();
    }
  }

  @Override
  protected void doExecute(Properties analysisProperties) {
    if (Utils.isAtLeast52(launcher.getVersion())) {
      launcher.execute(analysisProperties);
    } else {
      Properties prop = new Properties();
      prop.putAll(globalProperties());
      prop.putAll(analysisProperties);
      launcher.executeOldVersion(prop, extensions);
    }
  }
}
