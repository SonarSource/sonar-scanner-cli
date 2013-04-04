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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CommandExecutorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File workDir;

  @Before
  public void setUp() throws IOException {
    workDir = tempFolder.newFolder(testName.getMethodName());
  }

  @Test
  public void should_consume_StdOut_and_StdErr() throws Exception {
    final StringBuilder stdOutBuilder = new StringBuilder();
    CommandExecutor.StreamConsumer stdOutConsumer = new CommandExecutor.StreamConsumer() {
      public void consumeLine(String line) {
        stdOutBuilder.append(line).append(System.getProperty("line.separator"));
      }
    };
    final StringBuilder stdErrBuilder = new StringBuilder();
    CommandExecutor.StreamConsumer stdErrConsumer = new CommandExecutor.StreamConsumer() {
      public void consumeLine(String line) {
        stdErrBuilder.append(line).append(System.getProperty("line.separator"));
      }
    };
    Command command = Command.builder().setExecutable(getScript("output")).setDirectory(workDir).build();
    int exitCode = CommandExecutor.create().execute(command, stdOutConsumer, stdErrConsumer, 1000L);
    assertThat(exitCode).isEqualTo(0);

    String stdOut = stdOutBuilder.toString();
    String stdErr = stdErrBuilder.toString();
    assertThat(stdOut).contains("stdOut: first line");
    assertThat(stdOut).contains("stdOut: second line");
    assertThat(stdErr).contains("stdErr: first line");
    assertThat(stdErr).contains("stdErr: second line");
  }

  @Test
  public void stdOut_consumer_can_throw_exception() throws Exception {
    Command command = Command.builder().setExecutable(getScript("output")).setDirectory(workDir).build();
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdOut stream");
    CommandExecutor.create().execute(command, BAD_CONSUMER, NOP_CONSUMER, 1000L);
  }

  @Test
  public void stdErr_consumer_can_throw_exception() throws Exception {
    Command command = Command.builder().setExecutable(getScript("output")).setDirectory(workDir).build();
    thrown.expect(CommandException.class);
    thrown.expectMessage("Error inside stdErr stream");
    CommandExecutor.create().execute(command, NOP_CONSUMER, BAD_CONSUMER, 1000L);
  }

  private static final CommandExecutor.StreamConsumer NOP_CONSUMER = new CommandExecutor.StreamConsumer() {
    public void consumeLine(String line) {
      // nop
    }
  };

  private static final CommandExecutor.StreamConsumer BAD_CONSUMER = new CommandExecutor.StreamConsumer() {
    public void consumeLine(String line) {
      throw new RuntimeException();
    }
  };

  @Test
  public void should_use_working_directory_to_store_argument_and_environment_variable() throws Exception {
    Command command = Command.builder()
        .setDirectory(workDir)
        .setExecutable(getScript("echo"))
        .addArguments("1")
        .setEnvVariable("ENVVAR", "2")
        .build();
    int exitCode = CommandExecutor.create().execute(command, 1000L);
    assertThat(exitCode).isEqualTo(0);
    File logFile = new File(workDir, "echo.log");
    assertThat(logFile).exists();
    String log = FileUtils.readFileToString(logFile);
    assertThat(log).contains(workDir.getAbsolutePath());
    assertThat(log).contains("Parameter: 1");
    assertThat(log).contains("Environment variable: 2");
  }

  @Test
  public void should_stop_after_timeout() throws IOException {
    String executable = getScript("forever");
    long start = System.currentTimeMillis();
    try {
      CommandExecutor.create().execute(Command.builder().setExecutable(executable).setDirectory(workDir).build(), 300L);
      fail();
    } catch (CommandException e) {
      long duration = System.currentTimeMillis() - start;
      // should test >= 300 but it strangly fails during build on windows.
      // The timeout is raised after 297ms (??)
      assertThat(duration).as(e.getMessage()).isGreaterThan(290L);
    }
  }

  @Test
  public void should_fail_if_script_not_found() {
    thrown.expect(CommandException.class);
    CommandExecutor.create().execute(Command.builder().setExecutable("notfound").setDirectory(workDir).build(), 1000L);
  }

  private static String getScript(String name) throws IOException {
    String filename;
    if (new Os().isWindows()) {
      filename = name + ".bat";
    } else {
      filename = name + ".sh";
    }
    return new File("src/test/scripts/" + filename).getCanonicalPath();
  }

}
