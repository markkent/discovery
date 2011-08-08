package com.proofpoint.discovery.event;

import java.util.concurrent.TimeUnit;

import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Service;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

@EventType("appservices:type=discovery,name=StaticDelete")
public class StaticDeleteEvent
{
    public static class Builder
    {
        private final long startTime = System.nanoTime();
        private final Id<Service> id;
        private final EventClient eventClient;
        private boolean success = false;
        private String remoteAddress;

        
        Builder(Id<Service> id, EventClient eventClient)
        {
            this.id = id;
            this.eventClient = eventClient;
        }

        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public StaticDeleteEvent post()
        {
            StaticDeleteEvent event = build();
            eventClient.post(event);
            return event;
        }

        public StaticDeleteEvent build()
        {
            return new StaticDeleteEvent(new Duration (System.nanoTime() - startTime, TimeUnit.NANOSECONDS), id, success, remoteAddress);
        }

        public Builder setRemoteAddress(String address)
        {
            remoteAddress = address;
            return this;
        }
    }
    
    private final Duration duration;
    private final Id<Service> id;
    private final boolean success;
    private final String remoteAddress;
    
    StaticDeleteEvent(Duration duration, Id<Service> id, boolean success, String remoteAddress)
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
