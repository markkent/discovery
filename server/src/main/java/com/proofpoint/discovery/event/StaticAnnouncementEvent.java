package com.proofpoint.discovery.event;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Service;
import com.proofpoint.discovery.StaticAnnouncement;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

/**
 * @see DiscoveryEvents
 */
@EventType("appservices:type=discovery,name=StaticAnnounce")
public class StaticAnnouncementEvent
{
    public static class Builder
    {
        private final EventClient eventClient;
        private final long startTime = System.nanoTime();
        private StaticAnnouncement announcement;
        private boolean success= false;
        private Id<Service> serviceId;
        private String remoteAddress;
        
        Builder (EventClient eventClient)
        {
            Preconditions.checkNotNull(eventClient, "eventClient is null");
            this.eventClient = eventClient;
        }
        
        public Builder setAnnouncement(StaticAnnouncement announcement)
        {
            this.announcement = announcement;
            return this;
        }
        
        public Builder setRemoteAddress (String address)
        {
            remoteAddress = address;
            return this;
        }
        
        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public Builder setId(Id<Service> id)
        {
            serviceId = id;
            return this;
        }
        
        public StaticAnnouncementEvent build ()
        {
            return new StaticAnnouncementEvent(new Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), announcement, success, serviceId, remoteAddress);
        }
        
        public StaticAnnouncementEvent post ()
        {
            StaticAnnouncementEvent event = build();
            eventClient.post(event);
            return event;
        }
    }

    private final Duration duration;
    private final StaticAnnouncement announcement;
    private final boolean success;
    private final Id<Service> id;
    private final String remoteAddress;

    StaticAnnouncementEvent(Duration duration, StaticAnnouncement announcement, boolean success, Id<Service> id, String remoteAddress)
    {
        this.duration = duration;
        this.announcement = announcement;
        this.success = success;
        this.id = id;
        this.remoteAddress = remoteAddress;
    }

    @EventField
    public double getDuration()
    {
        return duration.toMillis();
    }
    
    @EventField
    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    @EventField
    public String getEnvironment()
    {
        return announcement.getEnvironment();
    }

    @EventField
    public String getLocation()
    {
        return announcement.getLocation();
    }

    @EventField
    public String getType()
    {
        return announcement.getType();
    }

    @EventField
    public String getPool()
    {
        return announcement.getPool();
    }

    @EventField
    public String getProperties()
    {
        Map<String, String> p = announcement.getProperties();
        return (p != null) ? p.toString() : null;
    }

    @EventField
    public boolean isSuccess()
    {
        return success;
    }

    @EventField
    public String getId()
    {
        return String.valueOf(id);
    }
}
