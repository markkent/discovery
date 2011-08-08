package com.proofpoint.discovery;

import com.google.common.base.Objects;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.discovery.event.DynamicAnnouncementEvent;
import com.proofpoint.discovery.event.DynamicDeleteEvent;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.stats.TimedStat;
import com.proofpoint.units.Duration;
import org.weakref.jmx.Managed;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/v1/announcement/{node_id}")
public class DynamicAnnouncementResource
{
    private final NodeInfo nodeInfo;
    private final DynamicStore dynamicStore;
    private final DiscoveryEvents events;
    private final TimedStat dynamicPutStats;
    private final TimedStat dynamicDeleteStats;
    private final AtomicInteger notFoundCount = new AtomicInteger(0);
    private final AtomicInteger environmentMismatchCount = new AtomicInteger(0);

    @Inject
    public DynamicAnnouncementResource(DynamicStore dynamicStore, NodeInfo nodeInfo, DiscoveryEvents events, DiscoveryConfig discoveryConfig)
    {
        this.dynamicStore = dynamicStore;
        this.nodeInfo = nodeInfo;
        this.events = events;
        this.dynamicDeleteStats = new TimedStat(discoveryConfig.getStatsWindowSize());
        this.dynamicPutStats = new TimedStat(discoveryConfig.getStatsWindowSize());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("node_id") Id<Node> nodeId, @Context UriInfo uriInfo, DynamicAnnouncement announcement, @Context HttpServletRequest request)
    {
        long startTime = System.nanoTime();
        boolean successful = false;
        DynamicAnnouncementEvent.Builder event = events.getDynamicAnnouncementEventBuilder();
        try {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            event.setAnnouncement(announcement);
            if (!nodeInfo.getEnvironment().equals(announcement.getEnvironment())) {
                environmentMismatchCount.incrementAndGet();
                return Response.status(BAD_REQUEST)
                        .entity(format("Environment mismatch. Expected: %s, Provided: %s", nodeInfo.getEnvironment(), announcement.getEnvironment()))
                        .build();
            }
    
            String location = Objects.firstNonNull(announcement.getLocation(), "/somewhere/" + nodeId.toString());
    
            DynamicAnnouncement announcementWithLocation = DynamicAnnouncement.copyOf(announcement)
                    .setLocation(location)
                    .build();
    
            dynamicStore.put(nodeId, announcementWithLocation);
            event.setSuccess();
            successful = true;
            return Response.status(ACCEPTED).build();
        }
        finally
        {
            event.post();
            if(successful) {
                dynamicPutStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @DELETE
    public Response delete(@PathParam("node_id") Id<Node> nodeId, @Context HttpServletRequest request)
    {
        long startTime = System.nanoTime();
        boolean successful = false;
        DynamicDeleteEvent.Builder event = events.getDynamicDeleteEventBuilder(nodeId);
        try {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            if (!dynamicStore.delete(nodeId)) {
                notFoundCount.incrementAndGet();
                return Response.status(NOT_FOUND).build();
            }
            
            event.setSuccess();
            successful = true;
            return Response.noContent().build();
        }
        finally {
            event.post();
            if(successful) {
                dynamicDeleteStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @Managed
    public TimedStat getDynamicPutStats()
    {
        return dynamicPutStats;
    }

    @Managed
    public TimedStat getDynamicDeleteStats()
    {
        return dynamicDeleteStats;
    }

    public int getNotFoundCount()
    {
        return notFoundCount.get();
    }

    public int getEnvironmentMismatchCount()
    {
        return environmentMismatchCount.get();
    }
}
