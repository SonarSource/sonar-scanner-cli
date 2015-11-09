/*
 * SonarQube Scanner
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
package org.sonar.runner.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.sonar.runner.api.EmbeddedRunner;

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

  private final Shutdown shutdown;
  private final Cli cli;
  private final Conf conf;
  private EmbeddedRunner runner;
  private BufferedReader inputReader;
  private RunnerFactory runnerFactory;
  private Logs logger;

  Main(Shutdown shutdown, Cli cli, Conf conf, RunnerFactory runnerFactory, Logs logger) {
    this.shutdown = shutdown;
    this.cli = cli;
    this.conf = conf;
    this.runnerFactory = runnerFactory;
    this.logger = logger;
  }

  public static void main(String[] args) {
    Exit exit = new Exit();
    Logs logs = new Logs(System.out, System.err);
    Cli cli = new Cli(exit, logs).parse(args);
    cli.verify();
    Shutdown shutdown = new Shutdown(exit, cli.isInteractive());
    Main main = new Main(shutdown, cli, new Conf(cli, logs), new RunnerFactory(logs), logs);
    main.execute();
  }

  void execute() {
    Stats stats = new Stats(logger).start();

    try {
      Properties p = conf.properties();
      init(p);
      runner.start();

      if (cli.isInteractive()) {
        interactiveLoop(p);
      } else {
        runAnalysis(stats, p);
      }
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during Sonar runner execution", e, cli.isDisplayStackTrace());
      shutdown.exit(Exit.ERROR);
    }

    runner.stop();
    shutdown.exit(Exit.SUCCESS);
  }

  private void interactiveLoop(Properties p) throws IOException {
    do {
      Stats stats = new Stats(logger).start();
      try {
        runAnalysis(stats, p);
      } catch (Exception e) {
        displayExecutionResult(stats, "FAILURE");
        showError("Error during Sonar runner execution", e, cli.isDisplayStackTrace());
      }
    } while (waitForUser());
  }

  private void init(Properties p) throws IOException {
    SystemInfo.print(logger);
    if (cli.isDisplayVersionOnly()) {
      shutdown.exit(Exit.SUCCESS);
    }

    if (cli.isDisplayStackTrace()) {
      logger.info("Error stacktraces are turned on.");
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

    shutdown.signalReady(true);
    if (shutdown.shouldExit()) {
      // exit before displaying message
      return false;
    }

    System.out.println("");
    System.out.println("<Press enter to restart analysis or Ctrl+C to exit the interactive mode>");
    String line = inputReader.readLine();
    shutdown.signalReady(false);

    return line != null;
  }

  // Visible for testing
  void setInputReader(BufferedReader inputReader) {
    this.inputReader = inputReader;
  }

  private void displayExecutionResult(Stats stats, String resultMsg) {
    logger.info("------------------------------------------------------------------------");
    logger.info("EXECUTION " + resultMsg);
    logger.info("------------------------------------------------------------------------");
    stats.stop();
    logger.info("------------------------------------------------------------------------");
  }

  private void showError(String message, Throwable e, boolean showStackTrace) {
    if (showStackTrace) {
      logger.error(message, e);
      if (!cli.isDebugMode()) {
        logger.error("");
        suggestDebugMode();
      }
    } else {
      logger.error(message);
      if (e != null) {
        logger.error(e.getMessage());
        String previousMsg = "";
        for (Throwable cause = e.getCause(); cause != null
          && cause.getMessage() != null
          && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
          logger.error("Caused by: " + cause.getMessage());
          previousMsg = cause.getMessage();
        }
      }
      logger.error("");
      logger.error("To see the full stack trace of the errors, re-run SonarQube Runner with the -e switch.");
      if (!cli.isDebugMode()) {
        suggestDebugMode();
      }
    }
  }

  private void suggestDebugMode() {
    logger.error("Re-run SonarQube Runner using the -X switch to enable full debug logging.");
  }

}
