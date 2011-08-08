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
    
    /** For EventBinder registration in Modules */
    public static final Class<?>[] getAllEventClasses ()
    {
        return new Class<?>[] {StaticAnnouncementEvent.class, StaticListEvent.class, StaticDeleteEvent.class, DynamicAnnouncementEvent.class,
                DynamicDeleteEvent.class, QueryEvent.class};
    }
    
    
    @Inject
    public DiscoveryEvents (EventClient eventClient)
    {
        this.eventClient = eventClient;
    }
    
    public StaticAnnouncementEvent.Builder getStaticAnnouncementEventBuilder ()
    {
        return new StaticAnnouncementEvent.Builder (eventClient);
    }
    
    public StaticListEvent.Builder getStaticListEventBuilder ()
    {
        return new StaticListEvent.Builder (eventClient);
    }

    public StaticDeleteEvent.Builder getStaticDeleteEventBuilder (Id<Service> id)
    {
        return new StaticDeleteEvent.Builder (id, eventClient);
    }
    
    public DynamicAnnouncementEvent.Builder getDynamicAnnouncementEventBuilder ()
    {
        return new DynamicAnnouncementEvent.Builder(eventClient);
    }
    
    public DynamicDeleteEvent.Builder getDynamicDeleteEventBuilder (Id<Node> id)
    {
        return new DynamicDeleteEvent.Builder (id, eventClient);
    }
    
    public QueryEvent.Builder getQueryEventBuilder (String type, String pool)
    {
        Preconditions.checkNotNull(type, "null type");              //Don't expose null capability.  May change.
        Preconditions.checkNotNull(pool, "null pool");
        return new QueryEvent.Builder (eventClient, type, pool);
    }
    
    public QueryEvent.Builder getQueryEventBuilder (String pool)
    {
        Preconditions.checkNotNull(pool, "null pool");              //Don't expose null capability.  May change.
        return new QueryEvent.Builder (eventClient, null, pool);
    }
    
    public QueryEvent.Builder getQueryEventBuilder ()
    {
        return new QueryEvent.Builder (eventClient, null, null);    //Don't expose null capability.  May change.
    }
}
