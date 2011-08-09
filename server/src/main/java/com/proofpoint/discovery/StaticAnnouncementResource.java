package com.proofpoint.discovery;

import com.google.common.base.Objects;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.discovery.event.StaticAnnouncementEvent;
import com.proofpoint.discovery.event.StaticDeleteEvent;
import com.proofpoint.discovery.event.StaticListEvent;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.stats.TimedStat;
import com.proofpoint.units.Duration;
import org.weakref.jmx.Managed;
import org.weakref.jmx.Nested;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Set;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/v1/announcement/static")
public class StaticAnnouncementResource
{
    private final StaticStore store;
    private final NodeInfo nodeInfo;
    private final DiscoveryEvents events;
    private final TimedStat staticPostStats;
    private final TimedStat staticGetStats;
    private final TimedStat staticDeleteStats;

    @Inject
    public StaticAnnouncementResource(StaticStore store, NodeInfo nodeInfo, DiscoveryEvents events, DiscoveryConfig discoveryConfig)
    {
        this.store = store;
        this.nodeInfo = nodeInfo;
        this.events = events;
        this.staticDeleteStats = new TimedStat(discoveryConfig.getStatsWindowSize());
        this.staticGetStats = new TimedStat(discoveryConfig.getStatsWindowSize());
        this.staticPostStats = new TimedStat(discoveryConfig.getStatsWindowSize());
    }

    @POST
    @Consumes("application/json")
    public Response post(StaticAnnouncement announcement, @Context UriInfo uriInfo, @Context HttpServletRequest request)
    {
        long startTime = System.nanoTime();
        boolean success = false;
        StaticAnnouncementEvent.Builder event = events.getStaticAnnouncementEventBuilder();
        try
        {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            
            event.setAnnouncement (announcement);


            if (!nodeInfo.getEnvironment().equals(announcement.getEnvironment())) {
                return Response.status(BAD_REQUEST)
                        .entity(format("Environment mismatch. Expected: %s, Provided: %s", nodeInfo.getEnvironment(), announcement.getEnvironment()))
                        .build();
            }
    
            Id<Service> id = Id.random();
            String location = Objects.firstNonNull(announcement.getLocation(), "/somewhere/" + id);
    
            Service service = Service.copyOf(announcement)
                        .setId(id)
                        .setLocation(location)
                        .build();
    
            event.setId (id);
            store.put(service);
    
            URI uri = UriBuilder.fromUri(uriInfo.getBaseUri()).path(StaticAnnouncementResource.class).path("{id}").build(id);
            Response response = Response.created(uri).entity(service).build();
            event.setSuccess();
            success = true;
            return response;
        }
        finally
        {
            event.post();
            if (success) {
                staticPostStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @GET
    @Produces("application/json")
    public Services get()
    {
        long startTime = System.nanoTime();
        boolean success = false;
        StaticListEvent.Builder event = events.getStaticListEventBuilder();
        try
        {
            Set<Service> serviceSet = store.getAll();
            Services services = new Services(nodeInfo.getEnvironment(), serviceSet);
            event.setServiceSet (serviceSet).setSuccess();
            success = true;
            return services;
        }
        finally
        {
            event.post();
            if (success) {
                staticGetStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") Id<Service> id, @Context HttpServletRequest request)
    {
        long startTime = System.nanoTime();
        boolean success = false;
        StaticDeleteEvent.Builder event = events.getStaticDeleteEventBuilder(id);
        try
        {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            store.delete(id);
            event.setSuccess();
            success = true;
        }
        finally
        {
            event.post();
            if (success) {
                staticDeleteStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @Managed
    @Nested
    public TimedStat getStaticPostStats()
    {
        return staticPostStats;
    }

    @Managed
    @Nested
    public TimedStat getStaticGetStats()
    {
        return staticGetStats;
    }

    @Managed
    @Nested
    public TimedStat getStaticDeleteStats()
    {
        return staticDeleteStats;
    }
}
