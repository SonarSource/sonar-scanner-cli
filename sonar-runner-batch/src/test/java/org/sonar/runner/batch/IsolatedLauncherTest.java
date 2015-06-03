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
  IsolatedLauncher launcher = new IsolatedLauncher();

  @Test
  public void should_create_batch() {
    props.setProperty("sonar.projectBaseDir", "src/test/java_sample");
    props.setProperty("sonar.projectKey", "sample");
    props.setProperty("sonar.projectName", "Sample");
    props.setProperty("sonar.projectVersion", "1.0");
    props.setProperty("sonar.sources", "src");
    Batch batch = launcher.createBatch(props, Collections.emptyList());

    assertThat(batch).isNotNull();
  }

  @Test
  public void testGetSqlLevel() throws Exception {
    assertThat(IsolatedLauncher.getSqlLevel(props)).isEqualTo("WARN");

    props.setProperty("sonar.showSql", "true");
    assertThat(IsolatedLauncher.getSqlLevel(props)).isEqualTo("DEBUG");

    props.setProperty("sonar.showSql", "false");
    assertThat(IsolatedLauncher.getSqlLevel(props)).isEqualTo("WARN");
  }

  @Test
  public void testGetSqlResultsLevel() throws Exception {
    assertThat(IsolatedLauncher.getSqlResultsLevel(props)).isEqualTo("WARN");

    props.setProperty("sonar.showSqlResults", "true");
    assertThat(IsolatedLauncher.getSqlResultsLevel(props)).isEqualTo("DEBUG");

    props.setProperty("sonar.showSqlResults", "false");
    assertThat(IsolatedLauncher.getSqlResultsLevel(props)).isEqualTo("WARN");
  }

  @Test
  public void shouldDetermineVerboseMode() {
    assertThat(launcher.isDebug(props)).isFalse();

    props.setProperty("sonar.verbose", "true");
    assertThat(launcher.isDebug(props)).isTrue();
  }
}
