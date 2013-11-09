package io.wunderboss.ruby.rack;

import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyString;

import java.io.IOException;
import java.util.Enumeration;

public class RackEnvironment {

    public RackEnvironment(Ruby runtime) throws IOException {
        this.runtime = runtime;
        rackVersion = RubyArray.newArray(runtime);
        rackVersion.add(RubyFixnum.one(runtime));
        rackVersion.add(RubyFixnum.one(runtime));
        errors = new RubyIO(this.runtime, this.runtime.getErr());
        errors.setAutoclose(false);
        rackChannelClass = RackChannel.createRackChannelClass(runtime);
    }

    public RubyHash getEnv(HttpServletRequestImpl request) throws IOException {
        RubyHash env = new RubyHash(runtime);

        RackChannel inputChannel = new RackChannel(runtime, rackChannelClass, request.getInputStream());
        env.put(RubyString.newString(runtime, "rack.input"), inputChannel);
        env.put(RubyString.newString(runtime, "rack.errors"), errors);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = request.getRequestURI();
        if (pathInfo.startsWith(request.getContextPath())) {
            pathInfo = pathInfo.substring(request.getContextPath().length());
        }
        if (pathInfo.startsWith(request.getServletPath())) {
            pathInfo = pathInfo.substring(request.getServletPath().length());
        }

        env.put(RubyString.newString(this.runtime, "REQUEST_METHOD"), RubyString.newString(this.runtime, request.getMethod()));
        env.put(RubyString.newString(this.runtime, "SCRIPT_NAME"), RubyString.newString(this.runtime,
                request.getContextPath() + request.getServletPath()));
        env.put(RubyString.newString(this.runtime, "PATH_INFO"), RubyString.newString(this.runtime, pathInfo));
        env.put(RubyString.newString(this.runtime, "QUERY_STRING"), RubyString.newString(this.runtime,
                request.getQueryString() == null ? "" : request.getQueryString()));
        env.put(RubyString.newString(this.runtime, "SERVER_NAME"), RubyString.newString(this.runtime, request.getServerName()));
        env.put(RubyString.newString(this.runtime, "SERVER_PORT"), RubyString.newString(this.runtime, request.getServerPort() + ""));
        env.put(RubyString.newString(this.runtime, "CONTENT_TYPE"), RubyString.newString(this.runtime, request.getContentType() + ""));
        env.put(RubyString.newString(this.runtime, "REQUEST_URI"), RubyString.newString(this.runtime,
                request.getContextPath() + request.getServletPath() + pathInfo));
        env.put(RubyString.newString(this.runtime, "REMOTE_ADDR"), RubyString.newString(this.runtime, request.getRemoteAddr()));
        env.put(RubyString.newString(this.runtime, "rack.url_scheme"), RubyString.newString(this.runtime, request.getScheme()));
        env.put(RubyString.newString(this.runtime, "rack.version"), this.rackVersion);
        env.put(RubyString.newString(this.runtime, "rack.multithread"), RubyBoolean.newBoolean(this.runtime, true));
        env.put(RubyString.newString(this.runtime, "rack.multiprocess"), RubyBoolean.newBoolean(this.runtime, true));
        env.put(RubyString.newString(this.runtime, "rack.run_once"), RubyBoolean.newBoolean(this.runtime, false));

        if (request.getContentLength() >= 0) {
            env.put(RubyString.newString(this.runtime, "CONTENT_LENGTH"), RubyFixnum.newFixnum(this.runtime, request.getContentLength()));
        }

        if ("https".equals(request.getScheme())) {
            env.put(RubyString.newString(this.runtime, "HTTPS"), RubyString.newString(this.runtime, "on"));
        }

        if (request.getHeaderNames() != null) {
            for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
                String headerName = headerNames.nextElement();
                String envName = "HTTP_" + headerName.toUpperCase().replace('-', '_');

                String value = request.getHeader(headerName);

                env.put(RubyString.newString(this.runtime, envName), RubyString.newString(this.runtime, value));
            }
        }

        //env.put(RubyString.newString(this.runtime, "servlet_request"), request);
        //env.put(RubyString.newString(this.runtime, "java.servlet_request"), request);

        if (log.isTraceEnabled()) {
            log.trace("Created: " + env.inspect());
        }
        return env;
    }

    private static final Logger log = Logger.getLogger(RackEnvironment.class);

    private Ruby runtime;
    private RubyArray rackVersion;
    private RubyIO errors;
    private RubyClass rackChannelClass;


}