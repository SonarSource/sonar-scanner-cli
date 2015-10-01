/*
 * SonarQube Runner - API
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.runner.batch.IsolatedLauncher;
import org.sonar.runner.cache.Logger;
import org.sonar.runner.impl.ClassloadRules;
import org.sonar.runner.impl.IsolatedLauncherFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.anyBoolean;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedRunnerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void should_create() {
    assertThat(EmbeddedRunner.create(mock(LogOutput.class))).isNotNull().isInstanceOf(EmbeddedRunner.class);
  }

  private IsolatedLauncherFactory batchLauncher;
  private IsolatedLauncher launcher;
  private EmbeddedRunner runner;

  @Before
  public void setUp() {
    batchLauncher = mock(IsolatedLauncherFactory.class);
    launcher = mock(IsolatedLauncher.class);
    when(launcher.getVersion()).thenReturn("5.2");
    when(batchLauncher.createLauncher(any(Properties.class), any(ClassloadRules.class), anyBoolean())).thenReturn(launcher);
    runner = new EmbeddedRunner(batchLauncher, mock(Logger.class), mock(LogOutput.class));
  }

  @Test
  public void test_sync_project() {
    String projectKey = "proj";
    runner.start();
    runner.syncProject(projectKey);
    verify(launcher).syncProject(projectKey);
  }

  @Test
  public void test_server_version() {
    runner.start();
    assertThat(runner.serverVersion()).isEqualTo("5.2");
  }

  @Test
  public void test_run_before_start() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("started");

    runner.runAnalysis(new Properties());
  }

  @Test
  public void test_fail_project_sync_old_sq() {
    when(launcher.getVersion()).thenReturn("5.0");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("version");

    runner.setGlobalProperty("sonar.projectKey", "foo");
    runner.start();
    runner.syncProject("foo");
  }

  @Test
  public void test_app() {
    EmbeddedRunner runner = EmbeddedRunner.create(mock(LogOutput.class)).setApp("Eclipse", "3.1");
    assertThat(runner.app()).isEqualTo("Eclipse");
    assertThat(runner.appVersion()).isEqualTo("3.1");
  }

  @Test
  public void test_back_compatibility() {
    when(launcher.getVersion()).thenReturn("4.5");

    Properties analysisProps = new Properties();
    analysisProps.put("sonar.dummy", "summy");

    runner.setGlobalProperty("sonar.projectKey", "foo");
    runner.start();
    runner.runAnalysis(analysisProps);
    runner.stop();

    verify(batchLauncher).createLauncher(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        return "foo".equals(((Properties) o).getProperty("sonar.projectKey"));
      }
    }), any(ClassloadRules.class), anyBoolean());

    // it should have added a few properties to analysisProperties, and have merged global props
    final String[] mustHaveKeys = {"sonar.working.directory", "sonar.sourceEncoding", "sonar.projectBaseDir",
      "sonar.projectKey", "sonar.dummy"};

    verify(launcher).executeOldVersion(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        Properties m = (Properties) o;
        for (String s : mustHaveKeys) {
          if (!m.containsKey(s)) {
            return false;
          }
        }
        return true;
      }
    }), eq(new LinkedList<>()));
  }

  @Test
  public void should_set_properties() {
    EmbeddedRunner runner = EmbeddedRunner.create(mock(LogOutput.class));
    runner.setGlobalProperty("sonar.projectKey", "foo");
    runner.addGlobalProperties(new Properties() {
      {
        setProperty("sonar.login", "admin");
        setProperty("sonar.password", "gniark");
      }
    });

    assertThat(runner.globalProperty("sonar.projectKey", null)).isEqualTo("foo");
    assertThat(runner.globalProperty("sonar.login", null)).isEqualTo("admin");
    assertThat(runner.globalProperty("sonar.password", null)).isEqualTo("gniark");
    assertThat(runner.globalProperty("not.set", "this_is_default")).isEqualTo("this_is_default");
  }

  @Test
  public void should_launch_batch() {
    runner.setGlobalProperty("sonar.projectKey", "foo");
    runner.start();
    runner.runAnalysis(new Properties());
    runner.stop();

    verify(batchLauncher).createLauncher(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        return "foo".equals(((Properties) o).getProperty("sonar.projectKey"));
      }
    }), any(ClassloadRules.class), anyBoolean());

    // it should have added a few properties to analysisProperties
    final String[] mustHaveKeys = {"sonar.working.directory", "sonar.sourceEncoding", "sonar.projectBaseDir"};

    verify(launcher).execute(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        Properties m = (Properties) o;
        for (String s : mustHaveKeys) {
          if (!m.containsKey(s)) {
            return false;
          }
        }
        return true;
      }
    }));
  }

  @Test
  public void test_issue_adapter() {
    final List<Issue> issuesRecorded = new LinkedList<>();
    IssueListener apiIssueListener = new IssueListener() {
      @Override
      public void handle(Issue issue) {
        issuesRecorded.add(issue);
      }
    };
    IssueListenerAdapter adapter = new IssueListenerAdapter(apiIssueListener);

    org.sonar.runner.batch.IssueListener.Issue batchIssue = new org.sonar.runner.batch.IssueListener.Issue();
    batchIssue.setAssigneeName("assignee");
    adapter.handle(batchIssue);

    assertThat(issuesRecorded).hasSize(1);
    assertThat(issuesRecorded.get(0).getAssigneeName()).isEqualTo("assignee");
  }

  @Test(expected = IllegalArgumentException.class)
  public void reject_issue_listener_old_version() {
    when(launcher.getVersion()).thenReturn("4.5");
    launch_with_issue_listener();
  }

  @Test
  public void launch_with_issue_listener() {
    runner.start();
    runner.runAnalysis(mock(Properties.class), mock(IssueListener.class));
    runner.stop();

    verify(launcher).execute(any(Properties.class), any(org.sonar.runner.batch.IssueListener.class));
  }

  @Test
  public void should_launch_batch_analysisProperties() {
    runner.setGlobalProperty("sonar.projectKey", "foo");
    runner.start();

    Properties analysisProperties = new Properties();
    analysisProperties.put("sonar.projectKey", "value1");
    runner.runAnalysis(analysisProperties);
    runner.stop();

    verify(batchLauncher).createLauncher(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        return "foo".equals(((Properties) o).getProperty("sonar.projectKey"));
      }
    }), any(ClassloadRules.class), anyBoolean());

    verify(launcher).execute(argThat(new ArgumentMatcher<Properties>() {
      @Override
      public boolean matches(Object o) {
        return "value1".equals(((Properties) o).getProperty("sonar.projectKey"));
      }
    }));
  }

  @Test
  public void should_launch_in_simulation_mode() throws IOException {
    batchLauncher = new IsolatedLauncherFactory(mock(Logger.class));
    runner = new EmbeddedRunner(batchLauncher, mock(Logger.class), mock(LogOutput.class));

    File dump = temp.newFile();
    Properties p = new Properties();

    p.setProperty("sonar.projectKey", "foo");
    runner.setGlobalProperty("sonarRunner.dumpToFile", dump.getAbsolutePath());
    runner.start();
    runner.runAnalysis(p);
    runner.stop();

    Properties props = new Properties();
    props.load(new FileInputStream(dump));

    assertThat(props.getProperty("sonar.projectKey")).isEqualTo("foo");
  }

  @Test
  public void should_set_default_platform_encoding() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.task", "scan");
    runner.initSourceEncoding(p);
    assertThat(p.getProperty("sonar.sourceEncoding", null)).isEqualTo(Charset.defaultCharset().name());
  }

  @Test
  public void invalidate_after_stop() {
    runner.start();
    runner.stop();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("started");
    runner.runAnalysis(new Properties());
  }

  @Test
  public void cannot_start_twice() {
    runner.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("started");
    runner.start();
  }

  @Test
  public void should_use_parameterized_encoding() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.task", "scan");
    p.setProperty("sonar.sourceEncoding", "THE_ISO_1234");
    runner.initSourceEncoding(p);
    assertThat(p.getProperty("sonar.sourceEncoding", null)).isEqualTo("THE_ISO_1234");
  }

  @Test
  public void should_not_init_encoding_if_not_project_task() throws Exception {
    Properties p = new Properties();
    p.setProperty("sonar.task", "views");
    runner.initSourceEncoding(p);
    assertThat(p.getProperty("sonar.sourceEncoding", null)).isNull();
  }

}
