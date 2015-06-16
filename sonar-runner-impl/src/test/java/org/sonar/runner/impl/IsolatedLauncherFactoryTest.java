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

import org.sonar.home.log.LogListener;
import org.junit.Before;
import org.sonar.runner.batch.IsolatedLauncher;

import java.util.List;
import java.util.Properties;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IsolatedLauncherFactoryTest {
  IsolatedLauncherFactory factory;
  Properties props;
  TempCleaning tempCleaning;
  JarDownloader jarDownloader;

  @Before
  public void setUp() {
    tempCleaning = mock(TempCleaning.class);
    factory = new IsolatedLauncherFactory(FakeIsolatedLauncher.class.getName(), tempCleaning);
    props = new Properties();
    jarDownloader = mock(JarDownloader.class);
  }

  @Test
  public void should_process_mask_rules() {
    Properties p = new Properties();
    p.put(InternalProperties.RUNNER_MASK_RULES, "UNMASK|org.sonar.runner.impl.");
    p.put("a", "b");

    String[][] maskRules = IsolatedLauncherFactory.getMaskRules(p);

    assertThat(maskRules).hasSize(1);
    assertThat(maskRules[0]).hasSize(2);
    assertThat(maskRules[0][0]).isEqualTo("UNMASK");
    assertThat(maskRules[0][1]).isEqualTo("org.sonar.runner.impl.");
  }

  @Test
  public void should_download_jars_and_execute_batch() {
    props.put("foo", "bar");

    // Unmask the current classloader in order to access FakeIsolatedLauncher
    props.put(InternalProperties.RUNNER_MASK_RULES, "UNMASK|org.sonar.runner.impl.");

    IsolatedLauncher isolatedLauncher = factory.createLauncher(jarDownloader, props);
    isolatedLauncher.execute(props);

    verify(jarDownloader).download();
    verify(tempCleaning).clean();
    assertThat(FakeIsolatedLauncher.props.get("foo")).isEqualTo("bar");
    assertThat(isolatedLauncher.getClass().getClassLoader().getClass().getSimpleName()).isEqualTo("IsolatedClassloader");
  }

  @Test
  public void should_use_isolated_classloader() {
    // The current classloader in not available -> fail to load FakeIsolatedLauncher
    props.put(InternalProperties.RUNNER_MASK_RULES, "");
    try {
      factory.createLauncher(jarDownloader, props);
      fail();
    } catch (RunnerException e) {
      // success
    }
  }

  public static class FakeIsolatedLauncher implements IsolatedLauncher {
    public static Properties props = null;

    @Override
    public void start(Properties properties, List<Object> extensions) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void execute(Properties properties) {
      FakeIsolatedLauncher.props = properties;
    }

    @Override
    public void start(Properties properties, List<Object> extensions, LogListener logListener) {
    }

    @Override
    public void executeOldVersion(Properties properties, List<Object> extensions) {
    }

    @Override
    public String getVersion() {
      return null;
    }
  }
}
