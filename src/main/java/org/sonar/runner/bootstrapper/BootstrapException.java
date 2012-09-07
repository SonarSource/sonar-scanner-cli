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
package org.sonar.runner.bootstrapper;

/**
 * Exception thrown by the bootstrapper when something bad happens.
 */
public class BootstrapException extends RuntimeException {

  private static final long serialVersionUID = -4974995497654796971L;

  /**
   * See {@link RuntimeException}
   */
  public BootstrapException(String message) {
    super(message);
  }

  /**
   * See {@link RuntimeException}
   */
  public BootstrapException(Throwable cause) {
    super(cause);
  }

  /**
   * See {@link RuntimeException}
   */
  public BootstrapException(String message, Throwable cause) {
    super(message, cause);
  }

}
