/*
 * Sonar Standalone Runner
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

/**
 * Exception thrown by the Sonar Runner when something bad happens.
 * 
 * @since 1.2
 */
public class RunnerException extends RuntimeException {

  private static final long serialVersionUID = 4810407777585753030L;

  /**
   * See {@link RuntimeException}
   */
  public RunnerException(String message) {
    super(message);
  }

  /**
   * See {@link RuntimeException}
   */
  public RunnerException(Throwable cause) {
    super(cause);
  }

  /**
   * See {@link RuntimeException}
   */
  public RunnerException(String message, Throwable cause) {
    super(message, cause);
  }

}
