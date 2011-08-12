package com.proofpoint.discovery;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.proofpoint.discovery.client.DiscoveryClient;
import com.proofpoint.discovery.client.DiscoveryException;
import com.proofpoint.discovery.client.ServiceAnnouncement;
import com.proofpoint.discovery.client.ServiceDescriptors;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.units.Duration;

import java.util.Set;

public class LocalDiscoveryClient implements DiscoveryClient
{
    private final DynamicStore dynamicStore;
    private final NodeInfo nodeInfo;

    @Inject
    public LocalDiscoveryClient(DynamicStore dynamicStore, NodeInfo nodeInfo)
    {
        this.dynamicStore = dynamicStore;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public CheckedFuture<Duration, DiscoveryException> announce(Set<ServiceAnnouncement> services)
    {
        ImmutableSet.Builder<DynamicServiceAnnouncement> announcementBuilder = ImmutableSet.builder();
        for (ServiceAnnouncement service : services) {
            announcementBuilder.add(new DynamicServiceAnnouncement(Id.<Service>valueOf(service.getId()), service.getType(), service.getProperties()));
        }

        dynamicStore.put(Id.<Node>valueOf(nodeInfo.getNodeId()), new DynamicAnnouncement(nodeInfo.getEnvironment(), nodeInfo.getPool(), nodeInfo.getLocation(), announcementBuilder.build()));
        return Futures.immediateCheckedFuture(DiscoveryClient.DEFAULT_DELAY);
    }

    @Override
    public CheckedFuture<Void, DiscoveryException> unannounce()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CheckedFuture<ServiceDescriptors, DiscoveryException> getServices(String type, String pool)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CheckedFuture<ServiceDescriptors, DiscoveryException> refreshServices(ServiceDescriptors serviceDescriptors)
    {
        throw new UnsupportedOperationException();
    }
}
