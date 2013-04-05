/*
 * Sonar Runner - Implementation
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
package org.sonar.runner.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileDownloader {


  static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  static final int READ_TIMEOUT_MILLISECONDS = 60000;
  private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

  private final String serverUrl;
  private final String userAgent;

  private FileDownloader(String serverUrl, String app, String appVersion) {
    this.serverUrl = serverUrl;
    this.userAgent = app + "/" + appVersion;
  }

  static FileDownloader create(Properties properties) {
    String serverUrl = properties.getProperty("sonar.host.url");
    String app = properties.getProperty(InternalProperties.RUNNER_APP);
    String appVersion = properties.getProperty(InternalProperties.RUNNER_APP_VERSION);
    return new FileDownloader(serverUrl, app, appVersion);
  }

  void download(String path, File toFile) {
    InputStream input = null;
    FileOutputStream output = null;
    String fullUrl = serverUrl + path;
    try {
      if (Logs.isDebugEnabled()) {
        Logs.debug("Download " + fullUrl + " to " + toFile.getAbsolutePath());
      }
      HttpURLConnection connection = newHttpConnection(new URL(fullUrl));
      int statusCode = connection.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      output = new FileOutputStream(toFile, false);
      input = connection.getInputStream();
      IOUtils.copyLarge(input, output);

    } catch (Exception e) {
      if (e instanceof ConnectException || e instanceof  UnknownHostException) {
        Logs.error("Sonar server '" + serverUrl + "' can not be reached");
      }
      IOUtils.closeQuietly(output);
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException("Fail to download: " + fullUrl, e);

    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }

  String downloadString(String path) throws IOException {
    String fullUrl = serverUrl + path;
    HttpURLConnection conn = newHttpConnection(new URL(fullUrl));
    String charset = getCharsetFromContentType(conn.getContentType());
    if (charset == null || "".equals(charset)) {
      charset = "UTF-8";
    }
    Reader reader = null;
    try {
      int statusCode = conn.getResponseCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("Status returned by url : '" + fullUrl + "' is invalid : " + statusCode);
      }
      reader = new InputStreamReader(conn.getInputStream(), charset);
      return IOUtils.toString(reader);
    } catch (IOException e) {
      if (e instanceof ConnectException || e instanceof  UnknownHostException) {
        Logs.error("Sonar server '" + serverUrl + "' can not be reached");
      }
      throw e;

    } finally {
      IOUtils.closeQuietly(reader);
      conn.disconnect();
    }
  }

  private HttpURLConnection newHttpConnection(URL url) throws IOException {
    //TODO send credentials
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
    connection.setReadTimeout(READ_TIMEOUT_MILLISECONDS);
    connection.setInstanceFollowRedirects(true);
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", userAgent);
    return connection;
  }

  /**
   * Parse out a charset from a content type header.
   *
   * @param contentType e.g. "text/html; charset=EUC-JP"
   * @return "EUC-JP", or null if not found. Charset is trimmed and upper-cased.
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
