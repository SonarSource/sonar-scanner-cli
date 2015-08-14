/*
 * SonarQube Runner - API
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
package org.sonar.runner.impl;

import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import com.github.kevinsawicki.http.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.sonar.home.cache.Logger;
import org.sonar.home.cache.PersistentCache;

class ServerConnection {

  private static final String SONAR_SERVER_CAN_NOT_BE_REACHED = "Sonar server ''{0}'' can not be reached";
  private static final String STATUS_RETURNED_BY_URL_IS_INVALID = "Status returned by url : ''{0}'' is invalid : {1}";
  static final int CONNECT_TIMEOUT_MILLISECONDS = 5000;
  static final int READ_TIMEOUT_MILLISECONDS = 60000;
  private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

  private final String serverUrl;
  private final String userAgent;

  private final PersistentCache wsCache;
  private final boolean isCacheEnable;
  private final Logger logger;

  private ServerConnection(String serverUrl, String app, String appVersion, boolean isCacheEnable, PersistentCache cache, Logger logger) {
    this.logger = logger;
    this.serverUrl = removeEndSlash(serverUrl);
    this.userAgent = app + "/" + appVersion;
    this.wsCache = cache;
    this.isCacheEnable = isCacheEnable;
  }

  private static String removeEndSlash(String url) {
    if (url == null) {
      return null;
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  static ServerConnection create(Properties properties, PersistentCache cache, Logger logger) {
    String serverUrl = properties.getProperty("sonar.host.url");
    String app = properties.getProperty(InternalProperties.RUNNER_APP);
    String appVersion = properties.getProperty(InternalProperties.RUNNER_APP_VERSION);
    boolean enableCache = isCacheEnabled(properties);

    return new ServerConnection(serverUrl, app, appVersion, enableCache, cache, logger);
  }

  private static boolean isCacheEnabled(Properties properties) {
    String analysisMode = properties.getProperty("sonar.analysis.mode");
    return "issues".equalsIgnoreCase(analysisMode);
  }

  /**
   * 
   * @throws HttpRequestException If there is an underlying IOException related to the connection
   * @throws IOException If the HTTP response code is != 200
   */
  private String downloadString(String url, boolean saveCache) throws HttpRequestException, IOException {
    HttpRequest httpRequest = null;
    try {
      httpRequest = newHttpRequest(new URL(url));
      String charset = getCharsetFromContentType(httpRequest.contentType());
      if (charset == null || "".equals(charset)) {
        charset = "UTF-8";
      }
      if (!httpRequest.ok()) {
        throw new IOException(MessageFormat.format(STATUS_RETURNED_BY_URL_IS_INVALID, url, httpRequest.code()));
      }

      byte[] body = httpRequest.bytes();
      if (saveCache) {
        try {
          wsCache.put(url, body);
        } catch (IOException e) {
          logger.warn("Failed to cache WS call: " + e.getMessage());
        }
      }
      return new String(body, charset);
    } finally {
      if (httpRequest != null) {
        httpRequest.disconnect();
      }
    }
  }

  void download(String path, File toFile) {
    String fullUrl = serverUrl + path;
    try {
      logger.debug("Download " + fullUrl + " to " + toFile.getAbsolutePath());
      HttpRequest httpRequest = newHttpRequest(new URL(fullUrl));
      if (!httpRequest.ok()) {
        throw new IOException(MessageFormat.format(STATUS_RETURNED_BY_URL_IS_INVALID, fullUrl, httpRequest.code()));
      }
      httpRequest.receive(toFile);

    } catch (Exception e) {
      if (e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException) {
        logger.error(MessageFormat.format(SONAR_SERVER_CAN_NOT_BE_REACHED, serverUrl));
      }
      FileUtils.deleteQuietly(toFile);
      throw new IllegalStateException("Fail to download: " + fullUrl, e);
    }
  }

  /**
   * Tries to fetch from server and falls back to cache. If both attempts fail, it throws the exception 
   * linked to the server connection failure.
   */
  String downloadStringCache(String path) throws IOException {
    String fullUrl = serverUrl + path;
    try {
      return downloadString(fullUrl, isCacheEnable);
    } catch (HttpRequest.HttpRequestException e) {
      if (isCausedByConnection(e) && isCacheEnable) {
        return fallbackToCache(fullUrl, e);
      }

      logger.error(MessageFormat.format(SONAR_SERVER_CAN_NOT_BE_REACHED, serverUrl));
      throw e;
    }
  }

  private static boolean isCausedByConnection(Exception e) {
    return e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException ||
      e.getCause() instanceof java.net.SocketTimeoutException;
  }

  private String fallbackToCache(String fullUrl, HttpRequest.HttpRequestException originalException) {
    logger.info(MessageFormat.format(SONAR_SERVER_CAN_NOT_BE_REACHED + ", trying cache", serverUrl));

    try {
      String cached = wsCache.getString(fullUrl, null);
      if (cached != null) {
        return cached;
      }
      logger.error(MessageFormat.format(SONAR_SERVER_CAN_NOT_BE_REACHED + " and had a cache miss", serverUrl));
      throw originalException;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to access cache", e);
    }

  }

  private HttpRequest newHttpRequest(URL url) {
    HttpRequest request = HttpRequest.get(url);
    request.trustAllCerts().trustAllHosts();
    request.acceptGzipEncoding().uncompress(true);
    request.connectTimeout(CONNECT_TIMEOUT_MILLISECONDS).readTimeout(READ_TIMEOUT_MILLISECONDS);
    request.userAgent(userAgent);
    return request;
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
