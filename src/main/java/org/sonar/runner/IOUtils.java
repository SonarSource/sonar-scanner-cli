/*
 * Sonar Runner
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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

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
   * Duplicated from Commons IO
   */
  static boolean deleteQuietly(File file) {
    if (file == null) {
      return false;
    }
    try {
      if (file.isDirectory()) {
        cleanDirectory(file);
      }
    } catch (Exception ignored) {
    }

    try {
      return file.delete();
    } catch (Exception ignored) {
      return false;
    }
  }

  private static void cleanDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    File[] files = directory.listFiles();
    // null if security restricted
    if (files == null) {
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (File file : files) {
      try {
        forceDelete(file);
      } catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }

  private static void forceDelete(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    } else {
      boolean filePresent = file.exists();
      if (!file.delete()) {
        if (!filePresent) {
          throw new FileNotFoundException("File does not exist: " + file);
        }
        String message =
            "Unable to delete file: " + file;
        throw new IOException(message);
      }
    }
  }

  private static void deleteDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    cleanDirectory(directory);

    if (!directory.delete()) {
      String message =
          "Unable to delete directory " + directory + ".";
      throw new IOException(message);
    }
  }

  /**
   * Parse out a charset from a content type header.
   *
   * @param contentType e.g. "text/html; charset=EUC-JP"
   * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
   */
  static String getCharsetFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }

    Matcher m = CHARSET_PATTERN.matcher(contentType);
    if (m.find()) {
      return m.group(1).trim().toUpperCase();
    }
    return null;
  }

}
