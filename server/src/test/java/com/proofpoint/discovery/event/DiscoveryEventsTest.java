package com.proofpoint.discovery.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.proofpoint.discovery.DynamicAnnouncement;
import com.proofpoint.discovery.DynamicServiceAnnouncement;
import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Service;
import com.proofpoint.event.client.InMemoryEventClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DiscoveryEventsTest
{

    DiscoveryEvents discoveryEvents;
    InMemoryEventClient eventClient;

    @BeforeMethod
    public void setup()
    {
        eventClient = new InMemoryEventClient();
        discoveryEvents = new DiscoveryEvents(eventClient);
    }

    @Test
    public void getAllEventClasses()
    {
        @SuppressWarnings("unchecked")
        HashSet<Class<?>> expected = new HashSet<Class<?>>(Arrays.asList(StaticAnnouncementEvent.class, StaticListEvent.class, StaticDeleteEvent.class, DynamicAnnouncementEvent.class, DynamicDeleteEvent.class, QueryEvent.class));
        HashSet<Class<?>> actual = new HashSet<Class<?>>(Arrays.asList(DiscoveryEvents.getAllEventClasses()));
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void getDynamicAnnouncementEventBuilder()
    {
        DynamicAnnouncementEvent.Builder builder = discoveryEvents.getDynamicAnnouncementEventBuilder();
        builder.setAnnouncement(new DynamicAnnouncement("env", "pool", "loc", Collections.singleton(new DynamicServiceAnnouncement(Id.<Service>random(), "type", new HashMap<String, String>()))));
        builder.post();
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Assert.assertEquals(eventClient.getEvents().get(0).getClass(), DynamicAnnouncementEvent.class);

    }

//    @Test
//    public void getDynamicDeleteEventBuilder()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getQueryEventBuilderStringString()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getQueryEventBuilderString()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getQueryEventBuilder()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getStaticAnnouncementEventBuilder()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getStaticDeleteEventBuilder()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getStaticListEventBuilder()
//    {
//        throw new RuntimeException("Test not implemented");
//    }
}
