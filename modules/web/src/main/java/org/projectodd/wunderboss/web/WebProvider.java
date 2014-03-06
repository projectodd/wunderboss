package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;

public class WebProvider implements ComponentProvider<WebComponent> {

    @Override
    public WebComponent create(String name, Options opts) {
        return new Web(name, opts);
    }
}
