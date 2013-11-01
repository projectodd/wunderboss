package io.wunderboss.rack;

import org.projectodd.restafari.spi.RequestContext;
import org.projectodd.restafari.spi.resource.Resource;
import org.projectodd.restafari.spi.resource.async.ObjectResource;
import org.projectodd.restafari.spi.resource.async.ResourceSink;
import org.projectodd.restafari.spi.resource.async.Responder;
import org.projectodd.restafari.spi.resource.async.SimplePropertyResource;
import org.projectodd.restafari.spi.state.ObjectResourceState;

public class RackApplicationResource implements ObjectResource {

    public RackApplicationResource(RackResource parent, String context) {
        this.parent = parent;
        this.context = context;
    }
    @Override
    public void update(RequestContext ctx, ObjectResourceState state, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void readContent(RequestContext ctx, ResourceSink sink) {
        sink.accept(new SimplePropertyResource(this, "context", this.context));
        sink.close();
    }

    @Override
    public Resource parent() {
        return this.parent;
    }

    @Override
    public String id() {
        return this.context;
    }

    @Override
    public void read(RequestContext ctx, String id, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(RequestContext ctx, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private RackResource parent;
    private String context;
}
