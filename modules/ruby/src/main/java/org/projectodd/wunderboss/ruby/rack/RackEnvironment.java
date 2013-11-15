package org.projectodd.wunderboss.ruby.rack;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
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
import org.projectodd.wunderboss.ruby.RubyHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

    public RubyHash getEnv(final HttpServerExchange exchange,
                           final RackChannel inputChannel,
                           final String contextPath) throws IOException {
        final RubyHash env = new RubyHash(runtime);
        env.put(toUsAsciiRubyString("rack.input"), inputChannel);
        env.put(toUsAsciiRubyString("rack.errors"), errors);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = exchange.getRequestURI();
        HeaderMap headers = exchange.getRequestHeaders();

        // strip contextPath and servletPath from pathInfo
        if (pathInfo.startsWith(contextPath) && !contextPath.equals("/")) {
            pathInfo = pathInfo.substring(contextPath.length());
        }

        String scriptName = contextPath;
        // SCRIPT_NAME should be an empty string for the root
        if (scriptName.equals("/")) {
            scriptName = "";
        }

        env.put(toUsAsciiRubyString("REQUEST_METHOD"), toUsAsciiRubyString(exchange.getRequestMethod().toString()));
        env.put(toUsAsciiRubyString("SCRIPT_NAME"), toRubyString(scriptName));
        env.put(toUsAsciiRubyString("PATH_INFO"), toRubyString(pathInfo));
        env.put(toUsAsciiRubyString("QUERY_STRING"), toRubyString(exchange.getQueryString()));
        env.put(toUsAsciiRubyString("SERVER_NAME"), toRubyString(exchange.getHostName()));
        env.put(toUsAsciiRubyString("SERVER_PORT"), toUsAsciiRubyString(exchange.getDestinationAddress().getPort() + ""));
        env.put(toUsAsciiRubyString("CONTENT_TYPE"), toUsAsciiRubyString(headers.getFirst(Headers.CONTENT_TYPE) + ""));
        env.put(toUsAsciiRubyString("REQUEST_URI"), toRubyString(scriptName + pathInfo));
        env.put(toUsAsciiRubyString("REMOTE_ADDR"), toUsAsciiRubyString(getRemoteAddr(exchange)));
        env.put(toUsAsciiRubyString("rack.url_scheme"), toUsAsciiRubyString(exchange.getRequestScheme()));
        env.put(toUsAsciiRubyString("rack.version"), rackVersion);
        env.put(toUsAsciiRubyString("rack.multithread"), RubyBoolean.newBoolean(runtime, true));
        env.put(toUsAsciiRubyString("rack.multiprocess"), RubyBoolean.newBoolean(runtime, true));
        env.put(toUsAsciiRubyString("rack.run_once"), RubyBoolean.newBoolean(runtime, false));


        final int contentLength = getContentLength(headers);
        if (contentLength >= 0) {
            env.put(toUsAsciiRubyString("CONTENT_LENGTH"), toUsAsciiRubyString(contentLength + ""));
        }

        if ("https".equals(exchange.getRequestScheme())) {
            env.put(toUsAsciiRubyString("HTTPS"), toUsAsciiRubyString("on"));
        }

        long iterCookie = headers.fastIterateNonEmpty();
        while (iterCookie != -1L) {
            HeaderValues headerValues = headers.fiCurrent(iterCookie);
            String headerName = headerValues.getHeaderName().toString();
            // RACK spec says not to create HTTP_CONTENT_TYPE or HTTP_CONTENT_LENGTH headers
            if (!headerName.equals("Content-Type") && !headerName.equals("Content-Length")) {
                String headerValue = headerValues.get(0);
                int valueIndex = 1;
                while (valueIndex < headerValues.size()) {
                    headerValue += "\n" + headerValues.get(valueIndex++);
                }
                env.put(toUsAsciiRubyString(rackHeaderNameToBytes(headerName)),
                        toUsAsciiRubyString(headerValue));
            }
            iterCookie = headers.fiNextNonEmpty(iterCookie);
        }

        if (log.isTraceEnabled()) {
            log.trace("Created: " + env.inspect());
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

    private RubyString toRubyString(final String string) {
        // TODO: Does Rack specify a particular encoding for all these strings?
        // https://groups.google.com/forum/#!topic/rack-devel/dmlAeKkm52g seems to imply ASCII?
        return RubyString.newString(runtime,
                new ByteList(string.getBytes(charset), false),
                ASCIIEncoding.INSTANCE);
    }

    private RubyString toUsAsciiRubyString(final String string) {
        return RubyHelper.toUsAsciiRubyString(runtime, string);
    }

    private RubyString toUsAsciiRubyString(final byte[] bytes) {
        return RubyHelper.toUsAsciiRubyString(runtime, bytes);
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