package com.proofpoint.discovery.event;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.proofpoint.discovery.Service;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

@EventType("appservices:type=discovery,name=StaticList")
public class StaticListEvent
{
    public static class Builder
    {
        private final long startTime = System.nanoTime();
        private final EventClient eventClient;
        private boolean success;
        private int serviceCount;
        
        Builder (EventClient eventClient)
        {
            this.eventClient = eventClient;
        }

        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public StaticListEvent post()
        {
            StaticListEvent event = build();
            eventClient.post(event);
            return event;
        }

        public StaticListEvent build()
        {
            return new StaticListEvent(new Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), serviceCount, success);
        }

        public Builder setServiceSet(Set<Service> serviceSet)
        {
            serviceCount = (serviceSet != null) ? serviceSet.size() : 0;
            return this;
        }
    }
    
    private final Duration duration;
    private final int resultCount;
    private final boolean success;
    
    StaticListEvent(Duration duration, int resultCount, boolean success)
    {
        this.duration = duration;
        this.resultCount = resultCount;
        this.success = success;
    }
    
    @EventField
    public double getDuration()
    {
        return duration.toMillis();
    }
    
    @EventField
    public int getResultCount ()
    {
        return resultCount;
    }

    @EventField
    public boolean isSuccess()
    {
        return success;
    }
}
