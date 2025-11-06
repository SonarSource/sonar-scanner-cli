/*
 * SonarScanner CLI
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
package testutils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static java.util.stream.Collectors.*;

public class LogTester implements BeforeEachCallback, AfterEachCallback {

  private final ConcurrentListAppender<ILoggingEvent> listAppender = new ConcurrentListAppender<>();

  public LogTester() {
    setLevel(Level.INFO);
  }

  /**
   * Change log level.
   * By default, INFO logs are enabled when LogTester is started.
   */
  public LogTester setLevel(Level level) {
    getRootLogger().setLevel(ch.qos.logback.classic.Level.fromLocationAwareLoggerInteger(level.toInt()));
    return this;
  }

  private static ch.qos.logback.classic.Logger getRootLogger() {
    return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one)
   */
  public List<String> logs() {
    return listAppender.list.stream().map(e -> (LoggingEvent) e)
      .map(LoggingEvent::getFormattedMessage)
      .collect(toList());
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<String> logs(Level level) {
    return listAppender.list.stream().map(e -> (LoggingEvent) e)
      .filter(e -> e.getLevel().equals(ch.qos.logback.classic.Level.fromLocationAwareLoggerInteger(level.toInt())))
      .map(LoggingEvent::getFormattedMessage)
      .collect(toList());
  }

  public LogTester clear() {
    listAppender.list.clear();
    return this;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    getRootLogger().addAppender(listAppender);
    listAppender.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    listAppender.stop();
    listAppender.list.clear();
    getRootLogger().detachAppender(listAppender);
    // Reset the level for following-up test suites
    setLevel(Level.INFO);
  }
}
