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

public class EmbeddedRunner extends Runner<EmbeddedRunner> {

  private final List<Object> extensions = new ArrayList<Object>();

  private EmbeddedRunner() {
  }

  public static EmbeddedRunner create() {
    return new EmbeddedRunner();
  }

  public EmbeddedRunner setUnmaskedPackages(String... packages) {
    return setProperty("sonarRunner.unmaskedPackages", Utils.join(packages, ","));
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
    new BatchLauncher().execute(properties(), extensions);
  }
}
