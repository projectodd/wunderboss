package org.projectodd.wunderboss.ruby.rack;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class RackEnvironment {

    // When adding a key to the enum be sure to add its string equivalent
    // to the map below
    static enum RACK_KEY {
        RACK_INPUT, RACK_ERRORS, REQUEST_METHOD, SCRIPT_NAME,
        PATH_INFO, QUERY_STRING, SERVER_NAME, SERVER_PORT,
        CONTENT_TYPE, REQUEST_URI, REMOTE_ADDR, URL_SCHEME,
        VERSION, MULTITHREAD, MULTIPROCESS, RUN_ONCE, CONTENT_LENGTH,
        HTTPS
    }
    static final int NUM_RACK_KEYS = RACK_KEY.values().length;
    static final Map<String, RACK_KEY> RACK_KEY_MAP = new HashMap<String, RACK_KEY>() {{
        put("rack.input", RACK_KEY.RACK_INPUT);
        put("rack.errors", RACK_KEY.RACK_ERRORS);
        put("REQUEST_METHOD", RACK_KEY.REQUEST_METHOD);
        put("SCRIPT_NAME", RACK_KEY.SCRIPT_NAME);
        put("PATH_INFO", RACK_KEY.PATH_INFO);
        put("QUERY_STRING", RACK_KEY.QUERY_STRING);
        put("SERVER_NAME", RACK_KEY.SERVER_NAME);
        put("SERVER_PORT", RACK_KEY.SERVER_PORT);
        put("CONTENT_TYPE", RACK_KEY.CONTENT_TYPE);
        put("REQUEST_URI", RACK_KEY.REQUEST_URI);
        put("REMOTE_ADDR", RACK_KEY.REMOTE_ADDR);
        put("rack.url_scheme", RACK_KEY.URL_SCHEME);
        put("rack.version", RACK_KEY.VERSION);
        put("rack.multithread", RACK_KEY.MULTITHREAD);
        put("rack.multiprocess", RACK_KEY.MULTIPROCESS);
        put("rack.run_once", RACK_KEY.RUN_ONCE);
        put("CONTENT_LENGTH", RACK_KEY.CONTENT_LENGTH);
        put("HTTPS", RACK_KEY.HTTPS);
    }};

    public RackEnvironment(Ruby runtime) throws IOException {
        this.runtime = runtime;
        rackVersion = RubyArray.newArray(runtime);
        rackVersion.add(RubyFixnum.one(runtime));
        rackVersion.add(RubyFixnum.one(runtime));
        errors = new RubyIO(runtime, runtime.getErr());
        errors.setAutoclose(false);
    }

    public RubyHash getEnv(final HttpServerExchange exchange,
                           final RackChannel inputChannel,
                           final String contextPath) throws IOException {
        HeaderMap headers = exchange.getRequestHeaders();
        // TODO: Should we only use this faster RackEnvironmentHash if we detect
        // specific JRuby versions that we know are compatible?
        final RackEnvironmentHash env = new RackEnvironmentHash(runtime, headers);
        env.lazyPut(RACK_KEY.RACK_INPUT, inputChannel, false);
        env.lazyPut(RACK_KEY.RACK_ERRORS, errors, false);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = exchange.getRequestURI();

        // strip contextPath and servletPath from pathInfo
        if (pathInfo.startsWith(contextPath) && !contextPath.equals("/")) {
            pathInfo = pathInfo.substring(contextPath.length());
        }

        String scriptName = contextPath;
        // SCRIPT_NAME should be an empty string for the root
        if (scriptName.equals("/")) {
            scriptName = "";
        }

        env.lazyPut(RACK_KEY.REQUEST_METHOD, exchange.getRequestMethod(), true);
        env.lazyPut(RACK_KEY.SCRIPT_NAME, scriptName, false);
        env.lazyPut(RACK_KEY.PATH_INFO, pathInfo, false);
        env.lazyPut(RACK_KEY.QUERY_STRING, exchange.getQueryString(), false);
        env.lazyPut(RACK_KEY.SERVER_NAME, exchange.getHostName(), false);
        env.lazyPut(RACK_KEY.SERVER_PORT, exchange.getDestinationAddress().getPort() + "", true);
        env.lazyPut(RACK_KEY.CONTENT_TYPE, headers.getFirst(Headers.CONTENT_TYPE) + "", true);
        env.lazyPut(RACK_KEY.REQUEST_URI, scriptName + pathInfo, false);
        env.lazyPut(RACK_KEY.REMOTE_ADDR, getRemoteAddr(exchange), true);
        env.lazyPut(RACK_KEY.URL_SCHEME, exchange.getRequestScheme(), true);
        env.lazyPut(RACK_KEY.VERSION, rackVersion, false);
        env.lazyPut(RACK_KEY.MULTITHREAD, RubyBoolean.newBoolean(runtime, true), false);
        env.lazyPut(RACK_KEY.MULTIPROCESS, RubyBoolean.newBoolean(runtime, true), false);
        env.lazyPut(RACK_KEY.RUN_ONCE, RubyBoolean.newBoolean(runtime, false), false);


        final int contentLength = getContentLength(headers);
        if (contentLength >= 0) {
            env.lazyPut(RACK_KEY.CONTENT_LENGTH, contentLength + "", true);
        }

        if ("https".equals(exchange.getRequestScheme())) {
            env.lazyPut(RACK_KEY.HTTPS, "on", true);
        }

        return env;
    }

    private static String getRemoteAddr(HttpServerExchange exchange) {
        InetSocketAddress sourceAddress = exchange.getSourceAddress();
        if(sourceAddress == null) {
            return "";
        }
        InetAddress address = sourceAddress.getAddress();
        if(address == null) {
            return "";
        }
        return address.getHostAddress();
    }

    private static int getContentLength(HeaderMap headers) {
        final String contentLengthStr = headers.getFirst(Headers.CONTENT_LENGTH);
        if (contentLengthStr == null || contentLengthStr.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(contentLengthStr);
    }

    private Ruby runtime;
    private RubyArray rackVersion;
    private RubyIO errors;

    private static final Logger log = Logger.getLogger(RackEnvironment.class);

}