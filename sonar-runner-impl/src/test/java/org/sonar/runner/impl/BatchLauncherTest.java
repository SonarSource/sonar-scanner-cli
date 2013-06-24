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
import org.sonar.runner.batch.IsolatedLauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BatchLauncherTest {

  JarDownloader jarDownloader = mock(JarDownloader.class);

  @Test
  public void should_download_jars_and_execute_batch() {
    TempCleaning tempCleaning = mock(TempCleaning.class);
    BatchLauncher launcher = new BatchLauncher(FakeIsolatedLauncher.class.getName(), tempCleaning);
    Properties props = new Properties();
    props.put("foo", "bar");

    // Unmask the current classloader in order to access FakeIsolatedLauncher
    props.put(InternalProperties.RUNNER_MASK_RULES, "UNMASK|org.sonar.runner.impl.");
    List<Object> extensions = new ArrayList<Object>();

    FakeIsolatedLauncher isolatedLauncher = (FakeIsolatedLauncher) launcher.doExecute(jarDownloader, mock(ServerVersion.class), props, extensions);
    assertThat(isolatedLauncher.props.get("foo")).isEqualTo("bar");
    assertThat(isolatedLauncher.extensions).isSameAs(extensions);
    verify(jarDownloader).download();
    verify(tempCleaning).clean();
  }

  @Test
  public void should_use_isolated_classloader() {
    BatchLauncher launcher = new BatchLauncher(FakeIsolatedLauncher.class.getName(), mock(TempCleaning.class));
    Properties props = new Properties();

    // The current classloader in not available -> fail to load FakeIsolatedLauncher
    props.put(InternalProperties.RUNNER_MASK_RULES, "");
    try {
      launcher.doExecute(jarDownloader, mock(ServerVersion.class), props, Collections.emptyList());
      fail();
    } catch (RunnerException e) {
      // success
    }
  }

  @Test
  public void verify_isolated_classloader_name() {
    // the class IsolatedLauncher should not be loaded in the classloader of BatchLauncher,
    // that's why it's referenced by its name
    assertThat(new BatchLauncher().isolatedLauncherClass).isEqualTo(IsolatedLauncher.class.getName());
  }

  @Test
  public void test_real_execution() {
    // verify the creation of dependent components
    Properties props = new Properties();
    List<Object> extensions = Collections.emptyList();
    BatchLauncher launcher = spy(new BatchLauncher());
    doReturn(new Object()).when(launcher).doExecute(any(JarDownloader.class), any(ServerVersion.class), eq(props), eq(extensions));

    launcher.execute(props, extensions);

    verify(launcher).execute(props, extensions);
  }

  public static class FakeIsolatedLauncher {
    public Properties props = null;
    public List<Object> extensions = null;
    private String sonarVersion;

    public void execute(String sonarVersion, Properties props, List<Object> extensions) {
      this.sonarVersion = sonarVersion;
      this.props = props;
      this.extensions = extensions;
    }
  }
}
