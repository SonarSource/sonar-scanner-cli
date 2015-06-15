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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class BatchLauncherMain {
  private final IsolatedLauncherFactory launcherFactory;

  BatchLauncherMain(IsolatedLauncherFactory factory) {
    this.launcherFactory = factory;
  }

  void execute(String[] args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Missing path to properties file");
    }
    Properties props = loadProperties(args[0]);
    IsolatedLauncher launcher = launcherFactory.createLauncher(props);
    launcher.start(props, Collections.emptyList());
    launcher.execute(props);
    launcher.stop();
  }

  private static Properties loadProperties(String arg) throws IOException {
    Properties props = new Properties();
    try (FileInputStream input = new FileInputStream(arg)) {
      props.load(input);
      // just to be clean, do not forward properties that do not make sense in fork mode
      props.remove(InternalProperties.RUNNER_MASK_RULES);
    }

    return props;
  }

  public static void main(String[] args) throws IOException {
    new BatchLauncherMain(new IsolatedLauncherFactory()).execute(args);
  }
}
