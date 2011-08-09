package com.proofpoint.discovery.event;

import java.util.concurrent.TimeUnit;

import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Node;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

@EventType("appservices:type=discovery,name=DynamicDelete")
public class DynamicDeleteEvent
{
    public static class Builder
    {
        private final long startTime = System.nanoTime();
        private final Id<Node> id;
        private final EventClient eventClient;
        private final DiscoveryEventConfig config;
        private boolean success = false;
        private String remoteAddress;

        Builder(Id<Node> id, EventClient eventClient, DiscoveryEventConfig config)
        {
            this.id = id;
            this.eventClient = eventClient;
            this.config = config;
        }

        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public DynamicDeleteEvent post()
        {
            DynamicDeleteEvent event = build();
            if (config.isEventEnabled(DiscoveryEventType.DYNAMICDELETE)) {
                eventClient.post(event);
            }
            return event;
        }

        public DynamicDeleteEvent build()
        {
            return new DynamicDeleteEvent(new Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), id, success, remoteAddress);
        }

        public Builder setRemoteAddress(String address)
        {
            remoteAddress = address;
            return this;
        }
    }

    private final Duration duration;
    private final Id<Node> id;
    private final boolean success;
    private final String remoteAddress;

    DynamicDeleteEvent(Duration duration, Id<Node> id, boolean success, String remoteAddress)
    {
        this.duration = duration;
        this.id = id;
        this.success = success;
        this.remoteAddress = remoteAddress;
    }

    @EventField
    public double getDuration()
    {
        return duration.toMillis();
    }

    @EventField
    public String getId()
    {
        return String.valueOf(id);
    }

    @EventField
    public boolean isSuccess()
    {
        return success;
    }

    @EventField
    public String getRemoteAddress()
    {
        return remoteAddress;
    }
}
