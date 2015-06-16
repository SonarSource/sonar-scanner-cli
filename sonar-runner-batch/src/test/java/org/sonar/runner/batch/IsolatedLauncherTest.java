/*
 * SonarQube Runner - Batch
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
package org.sonar.runner.batch;

import java.util.Collections;
import java.util.Properties;
import org.junit.Test;
import org.sonar.batch.bootstrapper.Batch;

import static org.fest.assertions.Assertions.assertThat;

public class IsolatedLauncherTest {

  Properties props = new Properties();
  BatchIsolatedLauncher launcher = new BatchIsolatedLauncher();

  @Test
  public void should_create_batch() {
    props.setProperty("sonar.projectBaseDir", "src/test/java_sample");
    props.setProperty("sonar.projectKey", "sample");
    props.setProperty("sonar.projectName", "Sample");
    props.setProperty("sonar.projectVersion", "1.0");
    props.setProperty("sonar.sources", "src");
    Batch batch = launcher.createBatch(props, Collections.emptyList(), null);

    assertThat(batch).isNotNull();
  }
}
