package com.proofpoint.discovery;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.proofpoint.discovery.client.DiscoveryClient;
import com.proofpoint.discovery.client.DiscoveryException;
import com.proofpoint.discovery.client.ServiceAnnouncement;
import com.proofpoint.discovery.client.ServiceDescriptor;
import com.proofpoint.discovery.client.ServiceDescriptors;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.units.Duration;

import java.util.Set;

public class LocalDiscoveryClient implements DiscoveryClient
{
    private final StaticStore staticStore;
    private final DynamicStore dynamicStore;
    private final NodeInfo nodeInfo;

    @Inject
    public LocalDiscoveryClient(DynamicStore dynamicStore, StaticStore staticStore, NodeInfo nodeInfo)
    {
        this.staticStore = staticStore;
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
        Preconditions.checkNotNull(type, "type is null");
        Preconditions.checkNotNull(pool, "pool is null");
        return lookup(type, pool);
    }

    @Override
    public CheckedFuture<ServiceDescriptors, DiscoveryException> refreshServices(ServiceDescriptors serviceDescriptors)
    {
        Preconditions.checkNotNull(serviceDescriptors, "serviceDescriptors is null");
        return lookup(serviceDescriptors.getType(), serviceDescriptors.getPool());
    }

    private CheckedFuture<ServiceDescriptors, DiscoveryException> lookup(String type, String pool)
    {
        ImmutableList.Builder<ServiceDescriptor> serviceDescriptorBuilder = ImmutableList.builder();
        for (Service service : Sets.union(staticStore.get(type, pool), dynamicStore.get(type, pool))) {
            serviceDescriptorBuilder.add(
                    new ServiceDescriptor(
                            service.getId().get(),
                            service.getNodeId().get().toString(),
                            service.getType(),
                            service.getPool(),
                            service.getLocation(),
                            service.getProperties()));
        }
        return Futures.immediateCheckedFuture(
                new ServiceDescriptors(type, pool,
                        serviceDescriptorBuilder.build(), DiscoveryClient.DEFAULT_DELAY, null));
    }
}
