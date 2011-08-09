package com.proofpoint.discovery.event;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.proofpoint.discovery.Service;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

@EventType("appservices:type=discovery,name=ServiceQuery")
public class QueryEvent
{
    public static class Builder
    {
        private final long startTime = System.nanoTime();
        private final EventClient eventClient;
        private final DiscoveryEventConfig config;
        private final String type, pool;
        private boolean success;
        private int serviceCount;

        Builder(EventClient eventClient, String type, String pool, DiscoveryEventConfig config)
        {
            this.eventClient = eventClient;
            this.type = type;
            this.pool = pool;
            this.config = config;
        }

        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public QueryEvent post()
        {
            QueryEvent event = build();
            if (config.isEventEnabled(DiscoveryEventType.SERVICEQUERY)) {
                eventClient.post(event);
            }
            return event;
        }

        public QueryEvent build()
        {
            return new QueryEvent(new Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), serviceCount, type, pool, success);
        }

        public Builder setServiceSet(Set<Service> serviceSet)
        {
            serviceCount = (serviceSet != null) ? serviceSet.size() : 0;
            return this;
        }
    }

    private final Duration duration;
    private final String type, pool;
    private final int resultCount;
    private final boolean success;

    QueryEvent(Duration duration, int resultCount, String type, String pool, boolean success)
    {
        this.duration = duration;
        this.resultCount = resultCount;
        this.type = type;
        this.pool = pool;
        this.success = success;
    }

    @EventField
    public double getDuration()
    {
        return duration.toMillis();
    }

    @EventField
    public int getResultCount()
    {
        return resultCount;
    }

    @EventField
    public String getType()
    {
        return type;
    }

    @EventField
    public String getPool()
    {
        return pool;
    }

    @EventField
    public boolean isSuccess()
    {
        return success;
    }
}
