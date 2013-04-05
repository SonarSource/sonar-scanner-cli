/*
 * Sonar Runner - Distribution
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

import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.impl.Logs;

import java.util.Properties;

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

  public static void main(String[] args) {
    Cli cli = new Cli().parse(args);
    new Main(cli).execute();
  }

  private final Cli cli;

  Main(Cli cli) {
    this.cli = cli;
  }

  void execute() {
    SystemInfo.print();
    if (!cli.isDisplayVersionOnly()) {
      int status = doExecute(new Conf(cli));
      System.exit(status);
    }
  }

  private int doExecute(Conf conf) {
    if (cli.isDisplayStackTrace()) {
      Logs.info("Error stacktraces are turned on.");
    }
    Stats stats = new Stats().start();
    try {
      Properties properties = conf.load();
      EmbeddedRunner.create().addProperties(properties).execute();
     // Logs.info("Work directory: " + runner.getWorkDir().getCanonicalPath());

    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during Sonar runner execution", e, cli.isDisplayStackTrace());
      return 1;
    }
    displayExecutionResult(stats, "SUCCESS");
    return 0;
  }

  private void displayExecutionResult(Stats stats, String resultMsg) {
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
      Logs.error("To see the full stack trace of the errors, re-run Sonar Runner with the -e switch.");
      if (!cli.isDebugMode()) {
        suggestDebugMode();
      }
    }
  }

  private void suggestDebugMode() {
    Logs.error("Re-run Sonar Runner using the -X switch to enable full debug logging.");
  }


}
