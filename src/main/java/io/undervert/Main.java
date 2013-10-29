package io.undervert;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Main {

    public static void main(String[] args) throws Exception {
        new UnderVert().start("localhost", 8080);
    }
}
