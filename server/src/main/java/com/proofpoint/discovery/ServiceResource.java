package com.proofpoint.discovery;

import java.util.Set;

import com.google.inject.Inject;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.discovery.event.QueryEvent;
import com.proofpoint.node.NodeInfo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.google.common.collect.Sets.union;

@Path("/v1/service")
public class ServiceResource
{
    private final DynamicStore dynamicStore;
    private final StaticStore staticStore;
    private final NodeInfo node;
    private final DiscoveryEvents events;

    @Inject
    public ServiceResource(DynamicStore dynamicStore, StaticStore staticStore, NodeInfo node, DiscoveryEvents events)
    {
        this.dynamicStore = dynamicStore;
        this.staticStore = staticStore;
        this.node = node;
        this.events = events;
    }

    @GET
    @Path("{type}/{pool}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type, @PathParam("pool") String pool)
    {
        QueryEvent.Builder event = events.getQueryEventBuilder(type, pool);
        try {
            Set<Service> serviceSet = union(dynamicStore.get(type, pool), staticStore.get(type, pool));
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            return s;
        }
        finally {
            event.post();
        }
    }

    @GET
    @Path("{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type)
    {
        QueryEvent.Builder event = events.getQueryEventBuilder(type);
        try {
            Set<Service> serviceSet = union(dynamicStore.get(type), staticStore.get(type));
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            return s;
        }
        finally {
            event.post();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices()
    {
        QueryEvent.Builder event = events.getQueryEventBuilder();
        try {
            Set<Service> serviceSet = union(dynamicStore.getAll(), staticStore.getAll());
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            return s;
        }
        finally {
            event.post();
        }
    }
}
