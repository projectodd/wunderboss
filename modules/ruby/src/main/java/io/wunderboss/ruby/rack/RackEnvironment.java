package io.wunderboss.ruby.rack;

import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.jboss.logging.Logger;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.util.ByteList;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;

public class RackEnvironment {

    public RackEnvironment(Ruby runtime) throws IOException {
        this.runtime = runtime;
        rackVersion = RubyArray.newArray(runtime);
        rackVersion.add(RubyFixnum.one(runtime));
        rackVersion.add(RubyFixnum.one(runtime));
        errors = new RubyIO(runtime, runtime.getErr());
        errors.setAutoclose(false);
    }

    public RubyHash getEnv(HttpServletRequestImpl request, RackChannel inputChannel) throws IOException {
        RubyHash env = new RubyHash(runtime);
        env.put(toRubyString("rack.input"), inputChannel);
        env.put(toRubyString("rack.errors"), errors);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String queryString = request.getQueryString();

        if (pathInfo.startsWith(contextPath)) {
            pathInfo = pathInfo.substring(contextPath.length());
        }
        if (pathInfo.startsWith(servletPath)) {
            pathInfo = pathInfo.substring(servletPath.length());
        }

        env.put(toRubyString("REQUEST_METHOD"), toRubyString(request.getMethod()));
        env.put(toRubyString("SCRIPT_NAME"), toRubyString(contextPath + servletPath));
        env.put(toRubyString("PATH_INFO"), toRubyString(pathInfo));
        env.put(toRubyString("QUERY_STRING"), toRubyString(queryString == null ? "" : queryString));
        env.put(toRubyString("SERVER_NAME"), toRubyString(request.getServerName()));
        env.put(toRubyString("SERVER_PORT"), toRubyString(request.getServerPort() + ""));
        env.put(toRubyString("CONTENT_TYPE"), toRubyString(request.getContentType() + ""));
        env.put(toRubyString("REQUEST_URI"), toRubyString(contextPath + servletPath + pathInfo));
        env.put(toRubyString("REMOTE_ADDR"), toRubyString(request.getRemoteAddr()));
        env.put(toRubyString("rack.url_scheme"), toRubyString(request.getScheme()));
        env.put(toRubyString("rack.version"), rackVersion);
        env.put(toRubyString("rack.multithread"), RubyBoolean.newBoolean(runtime, true));
        env.put(toRubyString("rack.multiprocess"), RubyBoolean.newBoolean(runtime, true));
        env.put(toRubyString("rack.run_once"), RubyBoolean.newBoolean(runtime, false));

        int contentLength = request.getContentLength();
        if (contentLength >= 0) {
            env.put(toRubyString("CONTENT_LENGTH"), RubyFixnum.newFixnum(runtime, contentLength));
        }

        if ("https".equals(request.getScheme())) {
            env.put(toRubyString("HTTPS"), toRubyString("on"));
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String envName = "HTTP_" + headerName.toUpperCase().replace('-', '_');

            String value = request.getHeader(headerName);

            env.put(toRubyString(envName), toRubyString(value));
        }

        //env.put(toRubyString("servlet_request"), request);
        //env.put(toRubyString("java.servlet_request"), request);

        if (log.isTraceEnabled()) {
            log.trace("Created: " + env.inspect());
        }
        return env;
    }

    private RubyString toRubyString(final String string) {
        // TODO: Does Rack specify a particular encoding for all these strings?
        // https://groups.google.com/forum/#!topic/rack-devel/dmlAeKkm52g seems to imply ASCII?
        return RubyString.newString(runtime,
                new ByteList(string.getBytes(charset), false),
                ASCIIEncoding.INSTANCE);
    }

    private static final Logger log = Logger.getLogger(RackEnvironment.class);

    private Ruby runtime;
    private RubyArray rackVersion;
    private RubyIO errors;
    private static final Charset charset = Charset.forName("UTF-8");


}