/*
 * SonarQube Runner - API
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

import org.sonar.runner.impl.BatchLauncherMain;
import org.sonar.runner.impl.JarExtractor;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner executed in a new JVM.
 *
 * @since 2.2
 */
public class ForkedRunner extends Runner<ForkedRunner> {

  private static final int ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

  private final Map<String, String> jvmEnvVariables = new HashMap<String, String>();
  private final List<String> jvmArguments = new ArrayList<String>();
  private String javaExecutable;
  private StreamConsumer stdOut = null, stdErr = null;
  private final JarExtractor jarExtractor;
  private final CommandExecutor commandExecutor;

  private ProcessMonitor processMonitor;

  ForkedRunner(JarExtractor jarExtractor, CommandExecutor commandExecutor, @Nullable ProcessMonitor processMonitor) {
    this.jarExtractor = jarExtractor;
    this.commandExecutor = commandExecutor;
    this.processMonitor = processMonitor;
  }

  ForkedRunner(JarExtractor jarExtractor, CommandExecutor commandExecutor) {
    this(jarExtractor, commandExecutor, null);
  }

  /**
   * Create new instance. Never return null.
   */
  public static ForkedRunner create() {
    return new ForkedRunner(new JarExtractor(), CommandExecutor.create());
  }

  /**
   * Create new instance. Never return null.
   */
  public static ForkedRunner create(ProcessMonitor processMonitor) {
    return new ForkedRunner(new JarExtractor(), CommandExecutor.create(), processMonitor);
  }

  /**
   * Path to the java executable. The JVM of the client app is used by default
   * (see the system property java.home)
   */
  public ForkedRunner setJavaExecutable(@Nullable String s) {
    this.javaExecutable = s;
    return this;
  }

  public List<String> jvmArguments() {
    return new ArrayList<String>(jvmArguments);
  }

  /**
   * See {@link #addJvmArguments(java.util.List)}
   */
  public ForkedRunner addJvmArguments(String... s) {
    return addJvmArguments(Arrays.asList(s));
  }

  /**
   * JVM arguments, for example "-Xmx512m"
   */
  public ForkedRunner addJvmArguments(List<String> args) {
    jvmArguments.addAll(args);
    return this;
  }

  /**
   * Set a JVM environment variable. By default no variables are set.
   */
  public ForkedRunner setJvmEnvVariable(String key, String value) {
    jvmEnvVariables.put(key, value);
    return this;
  }

  /**
   * Add some JVM environment variables. By default no variables are set.
   */
  public ForkedRunner addJvmEnvVariables(Map<String, String> map) {
    jvmEnvVariables.putAll(map);
    return this;
  }

  /**
   * Subscribe to the standard output. By default output is {@link System.out}
   */
  public ForkedRunner setStdOut(@Nullable StreamConsumer stream) {
    this.stdOut = stream;
    return this;
  }

  /**
   * Subscribe to the error output. By default output is {@link System.err}
   */
  public ForkedRunner setStdErr(@Nullable StreamConsumer stream) {
    this.stdErr = stream;
    return this;
  }

  @Override
  protected void doExecute() {
    ForkCommand forkCommand = createCommand();
    try {
      fork(forkCommand);
    } finally {
      deleteTempFiles(forkCommand);
    }
  }

  ForkCommand createCommand() {
    File propertiesFile = writeProperties();
    File jarFile = jarExtractor.extractToTemp("sonar-runner-impl");
    if (javaExecutable == null) {
      javaExecutable = new Os().thisJavaExe().getAbsolutePath();
    }
    Command command = Command.builder()
      .setExecutable(javaExecutable)
      .addEnvVariables(jvmEnvVariables)
      .addArguments(jvmArguments)
      .addArguments("-cp", jarFile.getAbsolutePath(), BatchLauncherMain.class.getName(), propertiesFile.getAbsolutePath())
      .build();
    return new ForkCommand(command, jarFile, propertiesFile);
  }

  private File writeProperties() {
    try {
      File file = File.createTempFile("sonar-project", ".properties");
      try (OutputStream output = new FileOutputStream(file)) {
        properties().store(output, "Generated by sonar-runner");
        return file;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to export sonar-runner properties", e);
    }
  }

  private void deleteTempFiles(ForkCommand forkCommand) {
    Utils.deleteQuietly(forkCommand.jarFile);
    Utils.deleteQuietly(forkCommand.propertiesFile);
  }

  private void fork(ForkCommand forkCommand) {
    if (stdOut == null) {
      stdOut = new PrintStreamConsumer(System.out);
    }
    if (stdErr == null) {
      stdErr = new PrintStreamConsumer(System.err);
    }
    int status = commandExecutor.execute(forkCommand.command, stdOut, stdErr, ONE_DAY_IN_MILLISECONDS, processMonitor);
    if (status != 0) {
      if (processMonitor != null && processMonitor.stop()) {
        stdOut.consumeLine(String.format("SonarQube Runner was stopped [status=%s]", status));
      } else {
        throw new IllegalStateException("Error status [command: " + forkCommand.command + "]: " + status);
      }
    }
  }

  static class ForkCommand {
    Command command;
    File jarFile;
    File propertiesFile;

    private ForkCommand(Command command, File jarFile, File propertiesFile) {
      this.command = command;
      this.jarFile = jarFile;
      this.propertiesFile = propertiesFile;
    }
  }
}
