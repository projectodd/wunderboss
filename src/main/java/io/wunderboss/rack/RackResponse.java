package io.wunderboss.rack;

import javax.servlet.http.HttpServletResponse;

import io.wunderboss.RuntimeHelper;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

public class RackResponse {
    public static final String RESPONSE_HANDLER_RB = "io/wunderboss/rack/response_handler";
    public static final String RESPONSE_HANDLER_CLASS_NAME = "WunderBoss::Rack::ResponseHandler";
    public static final String RESPONSE_HANDLER_METHOD_NAME = "handle";

    private IRubyObject rackResponse;

    public RackResponse(IRubyObject rackResponse) {
        this.rackResponse = rackResponse;
    }

    public void respond(HttpServletResponse response) {
        Ruby ruby = rackResponse.getRuntime();
        RuntimeHelper.invokeClassMethod( ruby, RESPONSE_HANDLER_CLASS_NAME, RESPONSE_HANDLER_METHOD_NAME, new Object[] { rackResponse, response});
    }

}


