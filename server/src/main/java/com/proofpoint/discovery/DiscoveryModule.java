package com.proofpoint.discovery;

import com.google.common.net.InetAddresses;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.proofpoint.cassandra.CassandraServerInfo;
import com.proofpoint.configuration.ConfigurationModule;
import com.proofpoint.discovery.client.ServiceSelectorFactory;
import com.proofpoint.discovery.event.DiscoveryEventConfig;
import com.proofpoint.discovery.event.DiscoveryEvents;
import com.proofpoint.event.client.EventBinder;
import com.proofpoint.node.NodeInfo;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.clock.MillisecondsClockResolution;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.joda.time.DateTime;
import org.weakref.jmx.guice.MBeanModule;
import static java.lang.String.format;

public class DiscoveryModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        //HTTP Services
        binder.bind(DynamicAnnouncementResource.class).in(Scopes.SINGLETON);
        binder.bind(StaticAnnouncementResource.class).in(Scopes.SINGLETON);
        MBeanModule.newExporter(binder).export(StaticAnnouncementResource.class).withGeneratedName();
        binder.bind(ServiceResource.class).in(Scopes.SINGLETON);
        
        //Events
        binder.bind(DiscoveryEvents.class).in(Scopes.SINGLETON);
        EventBinder.eventBinder(binder).bindEventClient(DiscoveryEvents.getAllEventClasses());
        ConfigurationModule.bindConfig(binder).to(DiscoveryEventConfig.class);
        MBeanModule.newExporter(binder).export(DiscoveryEventConfig.class).withGeneratedName();

        binder.bind(DynamicStore.class).to(CassandraDynamicStore.class).in(Scopes.SINGLETON);
        binder.bind(CassandraDynamicStore.class).in(Scopes.SINGLETON);

        binder.bind(StaticStore.class).to(CassandraStaticStore.class).in(Scopes.SINGLETON);
        binder.bind(CassandraStaticStore.class).in(Scopes.SINGLETON);

        binder.bind(DateTime.class).toProvider(RealTimeProvider.class);

        ConfigurationModule.bindConfig(binder).to(DiscoveryConfig.class);
        ConfigurationModule.bindConfig(binder).to(CassandraStoreConfig.class);
        
        binder.bind(ServiceSelectorFactory.class).to(LocalServiceSelectorFactory.class);
        
        binder.bind(CassandraSchemaInitialization.class).asEagerSingleton();
        MBeanModule.newExporter(binder).export(DynamicAnnouncementResource.class).withGeneratedName();
        MBeanModule.newExporter(binder).export(ServiceResource.class).withGeneratedName();
        MBeanModule.newExporter(binder).export(CassandraDynamicStore.class).withGeneratedName();
        MBeanModule.newExporter(binder).export(CassandraStaticStore.class).withGeneratedName();
        MBeanModule.newExporter(binder).export(StatisticsAggregator.class).withGeneratedName();


    }

    @Provides
    public Cluster getCluster(CassandraServerInfo cassandraInfo, NodeInfo nodeInfo)
    {
        CassandraHostConfigurator configurator = new CassandraHostConfigurator(format("%s:%s",
                                                                                      InetAddresses.toUriString(nodeInfo.getPublicIp()),
                                                                                      cassandraInfo.getRpcPort()));
        configurator.setClockResolution(new MillisecondsClockResolution());

        return HFactory.getOrCreateCluster("discovery", configurator);
    }
    
}
