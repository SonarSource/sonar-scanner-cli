/*
 * SonarQube Runner - Distribution
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
package org.sonar.runner;

import org.sonar.runner.api.Runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.sonar.runner.impl.Logs;

/**
 * Arguments :
 * <ul>
 * <li>runner.home: optional path to runner home (root directory with sub-directories bin, lib and conf)</li>
 * <li>runner.settings: optional path to runner global settings, usually ${runner.home}/conf/sonar-runner.properties.
 * This property is used only if ${runner.home} is not defined</li>
 * <li>project.home: path to project root directory. If not set, then it's supposed to be the directory where the runner is executed</li>
 * <li>project.settings: optional path to project settings. Default value is ${project.home}/sonar-project.properties.</li>
 * </ul>
 *
 * @since 1.0
 */
public class Main {

  private final Exit exit;
  private final Cli cli;
  private final Conf conf;
  private final RunnerFactory runnerFactory;
  private Runner<?> runner;
  private BufferedReader inputReader;

  Main(Exit exit, Cli cli, Conf conf, RunnerFactory runnerFactory) {
    this.exit = exit;
    this.cli = cli;
    this.conf = conf;
    this.runnerFactory = runnerFactory;
  }

  public static void main(String[] args) {
    Exit exit = new Exit();
    Cli cli = new Cli(exit).parse(args);
    cli.verify();
    Main main = new Main(exit, cli, new Conf(cli), new RunnerFactory());
    main.execute();
  }

  void execute() {
    Stats stats = new Stats().start();

    try {
      Properties p = conf.properties();
      init(p);
      runner.start();

      runAnalysis(stats, p);

      if(cli.isInteractive()) {
        while (waitForUser()) {
          stats = new Stats().start();
          runAnalysis(stats, p);
        } 
      }
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during Sonar runner execution", e, cli.isDisplayStackTrace());
      exit.exit(Exit.ERROR);
    }

    runner.stop();
    exit.exit(Exit.SUCCESS);
  }

  private void init(Properties p) throws IOException {
    SystemInfo.print();
    if (cli.isDisplayVersionOnly()) {
      exit.exit(Exit.SUCCESS);
    }

    if (cli.isDisplayStackTrace()) {
      Logs.info("Error stacktraces are turned on.");
    }

    runner = runnerFactory.create(p);
  }

  private void runAnalysis(Stats stats, Properties p) {
    runner.runAnalysis(p);
    displayExecutionResult(stats, "SUCCESS");
  }

  private boolean waitForUser() throws IOException {
    if (inputReader == null) {
      inputReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
    Logs.info("<Press enter to restart analysis>");

    return inputReader.readLine() != null;
  }

  private static void displayExecutionResult(Stats stats, String resultMsg) {
    Logs.info("------------------------------------------------------------------------");
    Logs.info("EXECUTION " + resultMsg);
    Logs.info("------------------------------------------------------------------------");
    stats.stop();
    Logs.info("------------------------------------------------------------------------");
  }

  public void showError(String message, Throwable e, boolean showStackTrace) {
    if (showStackTrace) {
      Logs.error(message, e);
      if (!cli.isDebugMode()) {
        Logs.error("");
        suggestDebugMode();
      }
    } else {
      Logs.error(message);
      if (e != null) {
        Logs.error(e.getMessage());
        String previousMsg = "";
        for (Throwable cause = e.getCause(); cause != null
          && cause.getMessage() != null
          && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
          Logs.error("Caused by: " + cause.getMessage());
          previousMsg = cause.getMessage();
        }
      }
      Logs.error("");
      Logs.error("To see the full stack trace of the errors, re-run SonarQube Runner with the -e switch.");
      if (!cli.isDebugMode()) {
        suggestDebugMode();
      }
    }
  }

  private static void suggestDebugMode() {
    Logs.error("Re-run SonarQube Runner using the -X switch to enable full debug logging.");
  }

}
