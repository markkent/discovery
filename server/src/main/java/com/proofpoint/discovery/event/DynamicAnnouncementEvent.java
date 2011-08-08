package com.proofpoint.discovery.event;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.proofpoint.discovery.DynamicAnnouncement;
import com.proofpoint.discovery.DynamicServiceAnnouncement;
import com.proofpoint.event.client.EventClient;
import com.proofpoint.event.client.EventField;
import com.proofpoint.event.client.EventType;
import com.proofpoint.units.Duration;

/**
 * @see DiscoveryEvents
 */
@EventType("appservices:type=discovery,name=DynamicAnnounce")
public class DynamicAnnouncementEvent
{
    public static class Builder
    {
        private final EventClient eventClient;
        private final long startTime = System.nanoTime();
        private DynamicAnnouncement announcement;
        private boolean success = false;
        private String remoteAddress;

        Builder(EventClient eventClient)
        {
            Preconditions.checkNotNull(eventClient, "eventClient is null");
            this.eventClient = eventClient;
        }

        public Builder setAnnouncement(DynamicAnnouncement announcement)
        {
            this.announcement = announcement;
            return this;
        }

        public Builder setRemoteAddress(String address)
        {
            remoteAddress = address;
            return this;
        }

        public Builder setSuccess()
        {
            success = true;
            return this;
        }

        public DynamicAnnouncementEvent[] build()
        {
            if (announcement == null) {
                return null;
            }
            
            Duration duration = new Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            Set<DynamicServiceAnnouncement> services = announcement.getServiceAnnouncements();
            DynamicAnnouncementEvent events[] = new DynamicAnnouncementEvent[services.size()];
            Iterator<DynamicServiceAnnouncement> itr = services.iterator();
            for (int i = 0; i < events.length; ++i) {
                events[i] = new DynamicAnnouncementEvent(duration, itr.next(), announcement, success, remoteAddress);
            }
            return events;
        }

        public DynamicAnnouncementEvent[] post()
        {
            DynamicAnnouncementEvent events[] = build();
            if (events != null) {
                eventClient.<DynamicAnnouncementEvent>post(events); // Generics to keep this from being ambiguous
            }
            return events;
        }
    }

    private final Duration duration;
    private final DynamicServiceAnnouncement service;
    private final DynamicAnnouncement announcement;
    private final boolean success;
    private final String remoteAddress;

    DynamicAnnouncementEvent(Duration duration, DynamicServiceAnnouncement service, DynamicAnnouncement announcement, boolean success, String remoteAddress)
    {
        this.duration = duration;
        this.service = service;
        this.announcement = announcement;
        this.success = success;
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
        return service.getType();
    }

    @EventField
    public String getPool()
    {
        return announcement.getPool();
    }

    @EventField
    public String getProperties()
    {
        Map<String, String> p = service.getProperties();
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
        return String.valueOf(service.getId());
    }
}
