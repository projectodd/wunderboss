/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.web.async.websocket;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * This filter is used to force the JsrWebSocketFilter in
 * Wildfly to find the endpoint registered at the servlet path.
 * Endpoints are looked up for servletPath + pathInfo, but we can't
 * know all the pathInfos we need to register the endpoint under,
 * because they are handled by frameworks in ruby/clojure at the app-level.
 *
 * So, this filter sends a request down the chain (if the request is
 * a websocket upgrade request) that doesn't have a pathInfo
 * and returns "/" for servletPath when it is "" to workaround an EAP issue
 * (https://bugzilla.redhat.com/show_bug.cgi?id=1256945).
 *
 * It also grabs the original ServletRequest in a ThreadLocal for the
 * downstream handshake to use.
 *
 * It also also handles the case where the websocket connection is aborted
 * by the user by returning an async response.
 */
public class WebSocketHelpyHelpertonFilter implements Filter {

    public static final ThreadLocal<HttpServletRequest> requestTL = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest &&
                !(request instanceof PathMungingRequestJacket) && // this filter has already been applied
                "websocket".equalsIgnoreCase(((HttpServletRequest) request).getHeader("Upgrade"))) {
            requestTL.set((HttpServletRequest)request);
            try {
                chain.doFilter(new PathMungingRequestJacket((HttpServletRequest) request), response);
            } catch (WebsocketConnectionAbortException abort) {
                HttpServletResponse httpResponse = (HttpServletResponse)response;
                httpResponse.setStatus(abort.status());
                // clear the headers already set by the websockets handler
                for(String header : httpResponse.getHeaderNames()) {
                    httpResponse.setHeader(header, null);
                }
                for(Map.Entry<String, String> header : abort.headers().entrySet()) {
                    httpResponse.setHeader(header.getKey(), header.getValue());
                }
                httpResponse.getOutputStream().print(abort.body());
            } finally {
                requestTL.remove();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    class PathMungingRequestJacket extends HttpServletRequestWrapper {
        public PathMungingRequestJacket(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getServletPath() {
            String superPath = super.getServletPath();

            return "".equals(superPath) ? "/" : superPath;
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
