/*
 * SonarQube Scanner
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.scanner.cli;

import java.io.IOException;
import java.util.Properties;
import org.sonarsource.scanner.api.EmbeddedScanner;

/**
 * Arguments :
 * <ul>
 * <li>scanner.home: optional path to Scanner home (root directory with sub-directories bin, lib and conf)</li>
 * <li>scanner.settings: optional path to runner global settings, usually ${scanner.home}/conf/sonar-scanner.properties.
 * This property is used only if ${scanner.home} is not defined</li>
 * <li>project.home: path to project root directory. If not set, then it's supposed to be the directory where the runner is executed</li>
 * <li>project.settings: optional path to project settings. Default value is ${project.home}/sonar-project.properties.</li>
 * </ul>
 *
 * @since 1.0
 */
public class Main {
  private static final String SEPARATOR = "------------------------------------------------------------------------";
  private final Exit exit;
  private final Cli cli;
  private final Conf conf;
  private EmbeddedScanner runner;
  private ScannerFactory runnerFactory;
  private Logs logger;

  Main(Exit exit, Cli cli, Conf conf, ScannerFactory runnerFactory, Logs logger) {
    this.exit = exit;
    this.cli = cli;
    this.conf = conf;
    this.runnerFactory = runnerFactory;
    this.logger = logger;
  }

  public static void main(String[] args) {
    Logs logs = new Logs(System.out, System.err);
    Exit exit = new Exit();
    Cli cli = new Cli(exit, logs).parse(args);
    Main main = new Main(exit, cli, new Conf(cli, logs, System.getenv()), new ScannerFactory(logs), logs);
    main.execute();
  }

  void execute() {
    Stats stats = new Stats(logger).start();

    try {
      Properties p = conf.properties();
      configureLogging(p);
      init(p);
      runner.start();
      logger.info("SonarQube server " + runner.serverVersion());
      runAnalysis(stats, p);
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during SonarQube Scanner execution", e, cli.isDisplayStackTrace() || cli.isDebugEnabled());
      exit.exit(Exit.ERROR);
    }

    runner.stop();
    exit.exit(Exit.SUCCESS);
  }

  private void init(Properties p) throws IOException {
    SystemInfo.print(logger);
    if (cli.isDisplayVersionOnly()) {
      exit.exit(Exit.SUCCESS);
    }

    if (cli.isDisplayStackTrace()) {
      logger.info("Error stacktraces are turned on.");
    }

    runner = runnerFactory.create(p);
  }

  private void configureLogging(Properties props) throws IOException {
    if ("true".equals(props.getProperty("sonar.verbose"))
      || "DEBUG".equalsIgnoreCase(props.getProperty("sonar.log.level"))
      || "TRACE".equalsIgnoreCase(props.getProperty("sonar.log.level"))) {
      logger.setDebugEnabled(true);
      logger.setDisplayStackTrace(true);
    }

    if (cli.isDisplayStackTrace()) {
      logger.setDisplayStackTrace(true);
    }
  }

  private void runAnalysis(Stats stats, Properties p) {
    runner.runAnalysis(p);
    displayExecutionResult(stats, "SUCCESS");
  }

  private void displayExecutionResult(Stats stats, String resultMsg) {
    logger.info(SEPARATOR);
    logger.info("EXECUTION " + resultMsg);
    logger.info(SEPARATOR);
    stats.stop();
    logger.info(SEPARATOR);
  }

  private void showError(String message, Throwable e, boolean showStackTrace) {
    if (showStackTrace) {
      logger.error(message, e);
      if (!cli.isDebugEnabled()) {
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
      logger.error("To see the full stack trace of the errors, re-run SonarQube Scanner with the -e switch.");
      if (!cli.isDebugEnabled()) {
        suggestDebugMode();
      }
    }
  }

  private void suggestDebugMode() {
    logger.error("Re-run SonarQube Scanner using the -X switch to enable full debug logging.");
  }

}
