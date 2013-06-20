/*
 * Sonar Runner - API
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

import org.sonar.runner.impl.BatchLauncher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link Runner} that is executed in the same JVM. The application can inject
 * some extensions into Sonar IoC container (see {@link #addExtensions(Object...)}. It can be
 * used for example in the Maven Sonar plugin to register Maven components like MavenProject
 * or MavenPluginExecutor.
 * @since 2.2
 */
public class EmbeddedRunner extends Runner<EmbeddedRunner> {

  private final BatchLauncher batchLauncher;
  private final List<Object> extensions = new ArrayList<Object>();
  private static final String MASK_RULES_PROP = "sonarRunner.maskRules";

  EmbeddedRunner(BatchLauncher bl) {
    this.batchLauncher = bl;
  }

  /**
   * Create a new instance.
   */
  public static EmbeddedRunner create() {
    return new EmbeddedRunner(new BatchLauncher());
  }

  /**
   * Sonar is executed in an almost fully isolated classloader (mask everything by default). his method allow to unmask some classes based on
   * a prefix of their fully qualified name. They relate to the extensions provided by {@link #addExtensions(Object...)}.
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
    String existingRules = property(MASK_RULES_PROP, "");
    if (!"".equals(existingRules)) {
      existingRules += ",";
    }
    existingRules += type + "|" + fqcnPrefix;
    return setProperty(MASK_RULES_PROP, existingRules);
  }

  /**
   * @deprecated since 2.3 use {@link #setUnmaskedClassRegexp(String)}
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
  protected void doExecute() {
    batchLauncher.execute(properties(), extensions);
  }
}
