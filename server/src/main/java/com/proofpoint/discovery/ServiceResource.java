package com.proofpoint.discovery;

import com.google.inject.Inject;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.discovery.event.QueryEvent;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.stats.TimedStat;
import com.proofpoint.units.Duration;
import org.weakref.jmx.Managed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

import static com.google.common.collect.Sets.union;

@Path("/v1/service")
public class ServiceResource
{
    private final DynamicStore dynamicStore;
    private final StaticStore staticStore;
    private final NodeInfo node;
    private final DiscoveryEvents events;
    private final TimedStat byTypeAndPoolStats;
    private final TimedStat byTypeStats;
    private final TimedStat allServicesStats;

    @Inject
    public ServiceResource(DynamicStore dynamicStore, StaticStore staticStore, NodeInfo node, DiscoveryEvents events, DiscoveryConfig discoveryConfig)
    {
        this.dynamicStore = dynamicStore;
        this.staticStore = staticStore;
        this.node = node;
        this.events = events;
        this.byTypeStats = new TimedStat(discoveryConfig.getStatsWindowSize());
        this.byTypeAndPoolStats = new TimedStat(discoveryConfig.getStatsWindowSize());
        this.allServicesStats = new TimedStat(discoveryConfig.getStatsWindowSize());

    }

    @GET
    @Path("{type}/{pool}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type, @PathParam("pool") String pool)
    {
        long startTime = System.nanoTime();
        boolean success = false;
        QueryEvent.Builder event = events.getQueryEventBuilder(type, pool);
        try {
            Set<Service> serviceSet = union(dynamicStore.get(type, pool), staticStore.get(type, pool));
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            success = true;
            return s;
        }
        finally {
            event.post();
            if (success) {
                byTypeAndPoolStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @GET
    @Path("{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices(@PathParam("type") String type)
    {
        long startTime = System.nanoTime();
        boolean success = false;
        QueryEvent.Builder event = events.getQueryEventBuilder(type);
        try {
            Set<Service> serviceSet = union(dynamicStore.get(type), staticStore.get(type));
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            success = true;
            return s;
        }
        finally {
            event.post();
            if (success) {
                byTypeStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Services getServices()
    {
        long startTime = System.nanoTime();
        boolean success = false;
        QueryEvent.Builder event = events.getQueryEventBuilder();
        try {
            Set<Service> serviceSet = union(dynamicStore.getAll(), staticStore.getAll());
            event.setServiceSet(serviceSet);
            Services s = new Services(node.getEnvironment(), serviceSet);
            event.setSuccess();
            success = true;
            return s;
        }
        finally {
            event.post();
            if (success) {
                allServicesStats.addValue(Duration.nanosSince(startTime));
            }
        }
    }

    @Managed
    public TimedStat getByTypeAndPoolStats()
    {
        return byTypeAndPoolStats;
    }

    @Managed
    public TimedStat getByTypeStats()
    {
        return byTypeStats;
    }

    @Managed
    public TimedStat getAllServicesStats()
    {
        return allServicesStats;
    }
}
