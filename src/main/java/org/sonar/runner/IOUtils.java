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

import java.io.*;

/**
 * Internal class used only by the Runner as we don't want it to depend on third-party libs.
 * This class should not be used by Sonar Runner consumers.
 */
final class IOUtils {

  private IOUtils() {
    // only static methods
  }

  /**
   * The default buffer size to use.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Unconditionally close a <code>Closeable</code>.
   */
  static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
    }
  }

  /**
   * Get the contents of a <code>Reader</code> as a String.
   */
  static String toString(Reader input) throws IOException {
    StringWriter sw = new StringWriter();
    copyLarge(input, sw);
    return sw.toString();
  }

  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   */
  static long copyLarge(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   */
  static long copyLarge(Reader input, Writer output) throws IOException {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Deletes a file (not a directory).
   */
  static boolean deleteFileQuietly(File file) {
    if (file == null) {
      return false;
    }
    try {
      return file.delete();
    } catch (Exception e) {
      return false;
    }
  }

}
