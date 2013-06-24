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

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BatchLauncherMainTest {

  BatchLauncher launcher = mock(BatchLauncher.class);

  @Test
  public void should_load_properties_and_execute() throws Exception {
    URL url = getClass().getResource("/org/sonar/runner/impl/BatchLauncherMainTest/props.properties");
    BatchLauncherMain main = new BatchLauncherMain(launcher);
    main.execute(new String[]{new File(url.toURI()).getAbsolutePath()});

    verify(launcher).execute(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        return ((Properties) o).get("sonar.login").equals("foo");
      }
    }), argThat(new ArgumentMatcher<List<Object>>() {
      @Override
      public boolean matches(Object o) {
        return ((List) o).isEmpty();
      }
    }));
  }

  @Test
  public void should_fail_if_missing_path_to_properties_file() {
    try {
      BatchLauncherMain main = new BatchLauncherMain(launcher);
      main.execute(new String[0]);
      fail();
    } catch (Exception e) {
      // success
    }
  }

  @Test
  public void should_fail_if_properties_file_does_not_exist() {
    try {
      BatchLauncherMain main = new BatchLauncherMain(launcher);
      main.execute(new String[]{"unknown/file.properties"});
      fail();
    } catch (Exception e) {
      // success
    }

  }
}
