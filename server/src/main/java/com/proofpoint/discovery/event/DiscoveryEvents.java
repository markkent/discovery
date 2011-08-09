package com.proofpoint.discovery.event;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Node;
import com.proofpoint.discovery.Service;
import com.proofpoint.event.client.EventClient;

public class DiscoveryEvents
{
    private final EventClient eventClient;
    private final DiscoveryEventConfig config;
    
    /** For EventBinder registration in Modules */
    public static final Class<?>[] getAllEventClasses ()
    {
        return new Class<?>[] {StaticAnnouncementEvent.class, StaticListEvent.class, StaticDeleteEvent.class, DynamicAnnouncementEvent.class,
                DynamicDeleteEvent.class, QueryEvent.class};
    }
    
    
    @Inject
    public DiscoveryEvents (EventClient eventClient, DiscoveryEventConfig config)
    {
        this.eventClient = eventClient;
        this.config = config;
    }
    
    public StaticAnnouncementEvent.Builder getStaticAnnouncementEventBuilder ()
    {
        return new StaticAnnouncementEvent.Builder (eventClient, config);
    }
    
    public StaticListEvent.Builder getStaticListEventBuilder ()
    {
        return new StaticListEvent.Builder (eventClient, config);
    }

    public StaticDeleteEvent.Builder getStaticDeleteEventBuilder (Id<Service> id)
    {
        return new StaticDeleteEvent.Builder (id, eventClient, config);
    }
    
    public DynamicAnnouncementEvent.Builder getDynamicAnnouncementEventBuilder ()
    {
        return new DynamicAnnouncementEvent.Builder(eventClient, config);
    }
    
    public DynamicDeleteEvent.Builder getDynamicDeleteEventBuilder (Id<Node> id)
    {
        return new DynamicDeleteEvent.Builder (id, eventClient, config);
    }
    
    public QueryEvent.Builder getQueryEventBuilder (String type, String pool)
    {
        Preconditions.checkNotNull(type, "null type");              //Don't expose null capability.  May change.
        Preconditions.checkNotNull(pool, "null pool");
        return new QueryEvent.Builder (eventClient, type, pool, config);
    }
    
    public QueryEvent.Builder getQueryEventBuilder (String pool)
    {
        Preconditions.checkNotNull(pool, "null pool");              //Don't expose null capability.  May change.
        return new QueryEvent.Builder (eventClient, null, pool, config);
    }
    
    public QueryEvent.Builder getQueryEventBuilder ()
    {
        return new QueryEvent.Builder (eventClient, null, null, config);    //Don't expose null capability.  May change.
    }
}
