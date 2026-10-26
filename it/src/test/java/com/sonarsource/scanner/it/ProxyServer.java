/*
 * SonarSource :: IT :: SonarScanner CLI
 * Copyright (C) SonarSource Sàrl
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
package com.sonarsource.scanner.it;

import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

class ProxyServer {

  private final ConcurrentLinkedDeque<String> requestsSeenByProxy = new ConcurrentLinkedDeque<>();
  private final ConcurrentLinkedDeque<String> connectRequestsSeenByProxy = new ConcurrentLinkedDeque<>();

  private final Server server;
  private final int port;

  private ProxyServer(Server server, int port) {
    this.server = server;
    this.port = port;
  }

  /** Starts an unauthenticated proxy. */
  static ProxyServer start() throws Exception {
    return start(false, null, null);
  }

  /** Starts a proxy requiring Basic auth (Proxy-Authorization) on all requests and CONNECT tunnels. */
  static ProxyServer start(String user, String password) throws Exception {
    return start(true, user, password);
  }

  private static ProxyServer start(boolean withProxyAuth, String user, String password) throws Exception {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLocalHost());

    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(500);

    Server server = new Server(threadPool);
    ProxyServer proxy = new ProxyServer(server, port);

    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecureScheme("https");
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);

    // Authentication wraps both handlers so HTTP forwarding and CONNECT use the same challenge.
    server.setHandler(proxy.new TrackingHandler(withProxyAuth, user, password));

    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    http.setPort(port);
    server.addConnector(http);

    server.start();
    return proxy;
  }

  int getPort() {
    return port;
  }

  Collection<String> getRequestsSeenByProxy() {
    return requestsSeenByProxy;
  }

  Collection<String> getConnectRequestsSeenByProxy() {
    return connectRequestsSeenByProxy;
  }

  void stop() throws Exception {
    server.stop();
    requestsSeenByProxy.clear();
    connectRequestsSeenByProxy.clear();
  }

  private class TrackingHandler extends Handler.Wrapper {

    private final boolean requireAuth;
    private final String user;
    private final String password;

    TrackingHandler(boolean requireAuth, String user, String password) {
      super(new ConnectHandler(new ProxyHandler.Forward()));
      this.requireAuth = requireAuth;
      this.user = user;
      this.password = password;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      if (requireAuth && !hasValidCredentials(request)) {
        response.setStatus(HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407);
        response.getHeaders().put(HttpHeader.PROXY_AUTHENTICATE, "Basic realm=\"proxy\"");
        response.getHeaders().put(HttpHeader.CONTENT_LENGTH, "0");
        callback.succeeded();
        return true;
      }
      if (HttpMethod.CONNECT.is(request.getMethod())) {
        connectRequestsSeenByProxy.add(request.getHttpURI().getAuthority());
      } else {
        requestsSeenByProxy.add(request.getHttpURI().getPath());
      }
      return super.handle(request, response, callback);
    }

    private boolean hasValidCredentials(Request request) {
      String credentials = request.getHeaders().get(HttpHeader.PROXY_AUTHORIZATION);
      if (credentials != null && credentials.startsWith("Basic ")) {
        String decoded = new String(Base64.getDecoder().decode(credentials.substring(6)), StandardCharsets.ISO_8859_1);
        int colon = decoded.indexOf(':');
        if (colon > 0) {
          String reqUser = decoded.substring(0, colon);
          String reqPass = decoded.substring(colon + 1);
          return user.equals(reqUser) && password.equals(reqPass);
        }
      }
      return false;
    }
  }
}
