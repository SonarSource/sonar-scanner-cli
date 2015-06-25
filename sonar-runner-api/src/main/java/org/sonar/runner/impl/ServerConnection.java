/*
 * SonarQube Runner - API
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

import com.github.kevinsawicki.http.HttpRequest;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.sonar.home.cache.Logger;
import org.sonar.home.cache.PersistentCache;

class ServerConnection {

  private static final String SONAR_SERVER_CAN_NOT_BE_REACHED = "Sonar server ''{0}'' can not be reached";
  private static final String STATUS_RETURNED_BY_URL_IS_INVALID = "Status returned by url : ''{0}'' is invalid : {1}";
  static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  static final int READ_TIMEOUT_MILLISECONDS = 60000;
  private static final Pattern CHARSET_PATTERN = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

  private final String serverUrl;
  private final String userAgent;

  private final PersistentCache wsCache;
  private final boolean isModePreview;
  private final Logger logger;

  private ServerConnection(String serverUrl, String app, String appVersion, boolean preview, PersistentCache cache, Logger logger) {
    this.logger = logger;
    this.serverUrl = removeEndSlash(serverUrl);
    this.userAgent = app + "/" + appVersion;
    this.wsCache = cache;
    this.isModePreview = preview;
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
    String analysisMode = properties.getProperty("sonar.analysis.mode");
    boolean preview = "preview".equalsIgnoreCase(analysisMode);

    return new ServerConnection(serverUrl, app, appVersion, preview, cache, logger);
  }

  private class StringDownloader implements Callable<String> {
    private String url;

    StringDownloader(String url) {
      this.url = url;
    }

    @Override
    public String call() throws Exception {
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
        return httpRequest.body(charset);
      } finally {
        if (httpRequest != null) {
          httpRequest.disconnect();
        }
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

  String downloadStringCache(String path) throws Exception {
    String fullUrl = serverUrl + path;
    try {
      if (isModePreview) {
        return wsCache.getString(serverUrl, new StringDownloader(fullUrl));
      } else {
        return new StringDownloader(fullUrl).call();
      }
    } catch (HttpRequest.HttpRequestException e) {
      if (e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException) {
        logger.error(MessageFormat.format(SONAR_SERVER_CAN_NOT_BE_REACHED, serverUrl));
      }
      throw e;
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
