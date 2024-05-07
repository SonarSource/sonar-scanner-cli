/*
 * SonarScanner CLI
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import ch.qos.logback.classic.Level;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarsource.scanner.lib.ScannerEngineBootstrapper;
import org.sonarsource.scanner.lib.ScannerEngineFacade;
import org.sonarsource.scanner.lib.ScannerProperties;

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
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Exit exit;
  private final Cli cli;
  private final Conf conf;
  private ScannerEngineBootstrapper scannerEngineBootstrapper;
  private final ScannerEngineBootstrapperFactory bootstrapperFactory;

  Main(Exit exit, Cli cli, Conf conf, ScannerEngineBootstrapperFactory bootstrapperFactory) {
    this.exit = exit;
    this.cli = cli;
    this.conf = conf;
    this.bootstrapperFactory = bootstrapperFactory;
  }

  public static void main(String[] args) {
    Exit exit = new Exit();
    Cli cli = new Cli(exit).parse(args);
    Main main = new Main(exit, cli, new Conf(cli, System.getenv()), new ScannerEngineBootstrapperFactory());
    main.analyze();
  }

  void analyze() {
    Stats stats = new Stats().start();

    int status = Exit.INTERNAL_ERROR;
    try {
      Properties p = conf.properties();
      checkSkip(p);
      configureLogging(p);
      init(p);
      try (var engine = scannerEngineBootstrapper.bootstrap()) {
        logServerType(engine);
        var success = engine.analyze((Map) p);
        if (success) {
          displayExecutionResult(stats, "SUCCESS");
          status = Exit.SUCCESS;
        } else {
          displayExecutionResult(stats, "FAILURE");
          status = Exit.SCANNER_ENGINE_ERROR;
        }
      }
    } catch (Throwable e) {
      displayExecutionResult(stats, "FAILURE");
      showError(e, cli.isDebugEnabled());
      status = isUserError(e) ? Exit.USER_ERROR : Exit.INTERNAL_ERROR;
    } finally {
      exit.exit(status);
    }
  }

  private static void logServerType(ScannerEngineFacade engine) {
    if (engine.isSonarCloud()) {
      LOG.info("Communicating with SonarCloud");
    } else {
      String serverVersion = engine.getServerVersion();
      LOG.info("Communicating with SonarQube Server {}", serverVersion);
    }
  }

  private void checkSkip(Properties properties) {
    if ("true".equalsIgnoreCase(properties.getProperty(ScannerProperties.SKIP))) {
      LOG.info("SonarScanner CLI analysis skipped");
      exit.exit(Exit.SUCCESS);
    }
  }

  private void init(Properties p) {
    SystemInfo.print();
    if (cli.isDisplayVersionOnly()) {
      exit.exit(Exit.SUCCESS);
    }

    scannerEngineBootstrapper = bootstrapperFactory.create(p, cli.getInvokedFrom());
  }

  private static void configureLogging(Properties props) {
    if ("true".equals(props.getProperty("sonar.verbose"))
      || "DEBUG".equalsIgnoreCase(props.getProperty("sonar.log.level"))
      || "TRACE".equalsIgnoreCase(props.getProperty("sonar.log.level"))) {
      var rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.DEBUG);
    }
  }

  private static void displayExecutionResult(Stats stats, String resultMsg) {
    LOG.info("EXECUTION {}", resultMsg);
    stats.stop();
  }

  private void showError(Throwable e, boolean debug) {
    var message = "Error during SonarScanner CLI execution";
    if (debug || !isUserError(e)) {
      LOG.error(message, e);
    } else {
      LOG.error(message);
      LOG.error(e.getMessage());
      String previousMsg = "";
      for (Throwable cause = e.getCause(); cause != null
        && cause.getMessage() != null
        && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
        LOG.error("Caused by: {}", cause.getMessage());
        previousMsg = cause.getMessage();
      }
    }

    if (!cli.isDebugEnabled()) {
      LOG.error("");
      suggestDebugMode();
    }
  }

  private static boolean isUserError(Throwable e) {
    // class not available at compile time (loaded by isolated classloader)
    return "org.sonar.api.utils.MessageException".equals(e.getClass().getName());
  }

  private void suggestDebugMode() {
    if (!cli.isEmbedded()) {
      LOG.error("Re-run SonarScanner CLI using the -X switch to enable full debug logging.");
    }
  }

}
