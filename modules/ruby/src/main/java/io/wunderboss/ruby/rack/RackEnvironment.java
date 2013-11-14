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
        env.put(toUsAsciiRubyString("rack.input"), inputChannel);
        env.put(toUsAsciiRubyString("rack.errors"), errors);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = request.getRequestURI();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String queryString = request.getQueryString();

        // strip contextPath and servletPath from pathInfo
        if (pathInfo.startsWith(contextPath) && !contextPath.equals("/")) {
            pathInfo = pathInfo.substring(contextPath.length());
        }
        if (pathInfo.startsWith(servletPath) && !servletPath.equals("/")) {
            pathInfo = pathInfo.substring(servletPath.length());
        }

        String scriptName = contextPath + servletPath;
        // SCRIPT_NAME should be an empty string for the root
        if (scriptName.equals("/")) {
            scriptName = "";
        }

        env.put(toUsAsciiRubyString("REQUEST_METHOD"), toUsAsciiRubyString(request.getMethod()));
        env.put(toUsAsciiRubyString("SCRIPT_NAME"), toRubyString(scriptName));
        env.put(toUsAsciiRubyString("PATH_INFO"), toRubyString(pathInfo));
        env.put(toUsAsciiRubyString("QUERY_STRING"), toRubyString(queryString == null ? "" : queryString));
        env.put(toUsAsciiRubyString("SERVER_NAME"), toRubyString(request.getServerName()));
        env.put(toUsAsciiRubyString("SERVER_PORT"), toUsAsciiRubyString(request.getServerPort() + ""));
        env.put(toUsAsciiRubyString("CONTENT_TYPE"), toRubyString(request.getContentType() + ""));
        env.put(toUsAsciiRubyString("REQUEST_URI"), toRubyString(scriptName + pathInfo));
        env.put(toUsAsciiRubyString("REMOTE_ADDR"), toUsAsciiRubyString(request.getRemoteAddr()));
        env.put(toUsAsciiRubyString("rack.url_scheme"), toUsAsciiRubyString(request.getScheme()));
        env.put(toUsAsciiRubyString("rack.version"), rackVersion);
        env.put(toUsAsciiRubyString("rack.multithread"), RubyBoolean.newBoolean(runtime, true));
        env.put(toUsAsciiRubyString("rack.multiprocess"), RubyBoolean.newBoolean(runtime, true));
        env.put(toUsAsciiRubyString("rack.run_once"), RubyBoolean.newBoolean(runtime, false));

        int contentLength = request.getContentLength();
        if (contentLength >= 0) {
            env.put(toUsAsciiRubyString("CONTENT_LENGTH"), toUsAsciiRubyString(contentLength + ""));
        }

        if ("https".equals(request.getScheme())) {
            env.put(toUsAsciiRubyString("HTTPS"), toUsAsciiRubyString("on"));
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // RACK spec says not to create HTTP_CONTENT_TYPE or HTTP_CONTENT_LENGTH heaers
            if (!headerName.equals("Content-Type") && !headerName.equals("Content-Length")) {
                env.put(toUsAsciiRubyString(rackHeaderNameToBytes(headerName)),
                        toUsAsciiRubyString(request.getHeader(headerName)));
            }
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

    private RubyString toUsAsciiRubyString(final String string) {
        byte[] bytes = new byte[string.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) string.charAt(i);
        }
        return toUsAsciiRubyString(bytes);
    }

    private RubyString toUsAsciiRubyString(final byte[] bytes) {
        return RubyString.newStringNoCopy(runtime, bytes);
    }

    private static byte[] rackHeaderNameToBytes(String headerName) {
        // This is a more performant implemention of:
        // "HTTP_" + headerName.toUpperCase().replace('-', '_');
        byte[] envNameBytes = new byte[headerName.length() + 5];
        envNameBytes[0] = 'H';
        envNameBytes[1] = 'T';
        envNameBytes[2] = 'T';
        envNameBytes[3] = 'P';
        envNameBytes[4] = '_';
        for (int i = 5; i < envNameBytes.length; i++) {
            envNameBytes[i] = (byte) rackHeaderize(headerName.charAt(i - 5));
        }
        return envNameBytes;
    }

    private static char rackHeaderize(char c) {
        if (c == '-') {
            c = '_';
        }
        return toUpperCase(c);
    }

    private static char toUpperCase(char c) {
        if (c >= 'a' && c <= 'z') {
            c -= 32;
        }
        return c;
    }

    private static final Logger log = Logger.getLogger(RackEnvironment.class);

    private Ruby runtime;
    private RubyArray rackVersion;
    private RubyIO errors;
    private static final Charset charset = Charset.forName("UTF-8");


}