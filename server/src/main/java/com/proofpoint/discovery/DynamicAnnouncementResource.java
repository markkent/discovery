package com.proofpoint.discovery;

import com.google.common.base.Objects;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.discovery.event.DynamicAnnouncementEvent;
import com.proofpoint.discovery.event.DynamicDeleteEvent;
import com.proofpoint.node.NodeInfo;

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

    @Inject
    public DynamicAnnouncementResource(DynamicStore dynamicStore, NodeInfo nodeInfo, DiscoveryEvents events)
    {
        this.dynamicStore = dynamicStore;
        this.nodeInfo = nodeInfo;
        this.events = events;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("node_id") Id<Node> nodeId, @Context UriInfo uriInfo, DynamicAnnouncement announcement, @Context HttpServletRequest request)
    {
        DynamicAnnouncementEvent.Builder event = events.getDynamicAnnouncementEventBuilder();
        try {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            event.setAnnouncement(announcement);
            if (!nodeInfo.getEnvironment().equals(announcement.getEnvironment())) {
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
            return Response.status(ACCEPTED).build();
        }
        finally
        {
            event.post();
        }
    }

    @DELETE
    public Response delete(@PathParam("node_id") Id<Node> nodeId, @Context HttpServletRequest request)
    {
        DynamicDeleteEvent.Builder event = events.getDynamicDeleteEventBuilder(nodeId);
        try {
            if (request != null) {
                event.setRemoteAddress(request.getRemoteAddr());
            }
            if (!dynamicStore.delete(nodeId)) {
                return Response.status(NOT_FOUND).build();
            }
            
            event.setSuccess();
            return Response.noContent().build();
        }
        finally {
            event.post();
        }
    }
}
