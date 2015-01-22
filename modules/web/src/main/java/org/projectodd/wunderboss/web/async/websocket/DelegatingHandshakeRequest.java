/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

import javax.websocket.server.HandshakeRequest;
import java.net.URI;
import java.security.Principal;
import java.util.*;
import javax.servlet.ServletContext;


public class DelegatingHandshakeRequest implements HandshakeRequest {

    public DelegatingHandshakeRequest(HandshakeRequest delegate, ServletContext context, String subcontext) {
        this.delegate = delegate;
        this.context = context;
        this.subcontext = subcontext;
    }

    public int getOriginPort() {
        return getOrigin().getPort();
    }

    public String getOriginHost() {
        return getOrigin().getHost();
    }

    public String getOriginScheme() {
        return getOrigin().getScheme();
    }

    public String getContextPath() {
        String v = context.getContextPath() + subcontext;
        return v.endsWith("/") ? v.substring(0, v.length()-1) : v;
    }

    public String getPathInfo() {
        return "/" + URI.create(getContextPath()).relativize(getRequestURI()).getPath();
    }
        
    @Override
    public Map<String, List<String>> getHeaders() {
        return this.delegate.getHeaders();
    }

    @Override
    public Object getHttpSession() {
        return this.delegate.getHttpSession();
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return this.delegate.getParameterMap();
    }

    @Override
    public String getQueryString() {
        return this.delegate.getQueryString();
    }

    @Override
    public URI getRequestURI() {
        return this.delegate.getRequestURI();
    }

    @Override
    public Principal getUserPrincipal() {
        return this.delegate.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return this.delegate.isUserInRole(role);
    }

    URI getOrigin() {
        if (origin == null) {
            List<String> v = getHeaders().get("origin");
            origin = URI.create( v==null ? "" : v.get(0) );
        }
        return origin;
    }

    private HandshakeRequest delegate;
    private ServletContext context;
    private String subcontext;
    private URI origin;
}
