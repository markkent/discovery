package com.proofpoint.discovery.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.proofpoint.discovery.DynamicAnnouncement;
import com.proofpoint.discovery.DynamicServiceAnnouncement;
import com.proofpoint.discovery.Id;
import com.proofpoint.discovery.Node;
import com.proofpoint.discovery.Service;
import com.proofpoint.discovery.StaticAnnouncement;
import com.proofpoint.event.client.InMemoryEventClient;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestDiscoveryEvents
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
    public void testGetAllEventClasses()
    {
        @SuppressWarnings("unchecked") HashSet<Class<?>> expected = new HashSet<Class<?>>(Arrays.asList(StaticAnnouncementEvent.class, StaticListEvent.class, StaticDeleteEvent.class,
                DynamicAnnouncementEvent.class, DynamicDeleteEvent.class, QueryEvent.class));
        HashSet<Class<?>> actual = new HashSet<Class<?>>(Arrays.asList(DiscoveryEvents.getAllEventClasses()));
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testGetDynamicAnnouncementEventBuilder()
    {
        Id<Service> id = Id.random();
        String env = randomWord();
        String pool = randomWord();
        String loc = randomWord();
        String type = randomWord();
        Map<String, String> props = new HashMap<String, String>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            props.put(randomWord(), randomWord());
        }
        boolean success = RandomUtils.nextBoolean();
        String address = randomWord();
        
        DynamicAnnouncementEvent.Builder builder = discoveryEvents.getDynamicAnnouncementEventBuilder();
        builder.setAnnouncement(new DynamicAnnouncement(env, pool, loc, Collections.singleton(new DynamicServiceAnnouncement(id, type, props))));
        if (success) {
            builder.setSuccess();
        }
        builder.setRemoteAddress(address);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), DynamicAnnouncementEvent.class);
        DynamicAnnouncementEvent postedEvent = (DynamicAnnouncementEvent)postedObject;
        Assert.assertEquals(postedEvent.getEnvironment(), env);
        Assert.assertEquals(postedEvent.getId(), id.toString());
        Assert.assertEquals(postedEvent.getLocation(), loc);
        Assert.assertEquals(postedEvent.getPool(), pool);
        Assert.assertEquals(postedEvent.getProperties(), props.toString());
        Assert.assertEquals(postedEvent.getRemoteAddress(), address);
        Assert.assertEquals(postedEvent.getType(), type);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetDynamicDeleteEventBuilder()
    {
        Id<Node> id = Id.random();
        boolean success = RandomUtils.nextBoolean();
        String address = randomWord();

        DynamicDeleteEvent.Builder builder = discoveryEvents.getDynamicDeleteEventBuilder(id);
        if (success) {
            builder.setSuccess();
        }
        builder.setRemoteAddress(address);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), DynamicDeleteEvent.class);
        DynamicDeleteEvent postedEvent = (DynamicDeleteEvent)postedObject;
        Assert.assertEquals(postedEvent.getId(), id.toString());
        Assert.assertEquals(postedEvent.isSuccess(), success);
        Assert.assertEquals(postedEvent.getRemoteAddress(), address);
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetQueryEventBuilderStringString()
    {
        String type = randomWord();
        String pool = randomWord();
        boolean success = RandomUtils.nextBoolean();
        Set<Service> serviceSet = new HashSet<Service>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            serviceSet.add(new Service(Id.<Service>random(), Id.<Node>random(), randomWord(), randomWord(), randomWord(), Collections.<String, String>emptyMap()));
        }
        
        QueryEvent.Builder builder = discoveryEvents.getQueryEventBuilder(type, pool);
        if (success) {
            builder.setSuccess();
        }
        builder.setServiceSet(serviceSet);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), QueryEvent.class);
        QueryEvent postedEvent = (QueryEvent)postedObject;
        Assert.assertEquals(postedEvent.getType(), type);
        Assert.assertEquals(postedEvent.getPool(), pool);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        Assert.assertEquals(postedEvent.getResultCount(), serviceSet.size());
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetQueryEventBuilderString()
    {
        String pool = randomWord();
        boolean success = RandomUtils.nextBoolean();
        Set<Service> serviceSet = new HashSet<Service>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            serviceSet.add(new Service(Id.<Service>random(), Id.<Node>random(), randomWord(), randomWord(), randomWord(), Collections.<String, String>emptyMap()));
        }
        
        QueryEvent.Builder builder = discoveryEvents.getQueryEventBuilder(pool);
        if (success) {
            builder.setSuccess();
        }
        builder.setServiceSet(serviceSet);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), QueryEvent.class);
        QueryEvent postedEvent = (QueryEvent)postedObject;
        Assert.assertEquals(postedEvent.getType(), null);
        Assert.assertEquals(postedEvent.getPool(), pool);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        Assert.assertEquals(postedEvent.getResultCount(), serviceSet.size());
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetQueryEventBuilder()
    {
        boolean success = RandomUtils.nextBoolean();
        Set<Service> serviceSet = new HashSet<Service>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            serviceSet.add(new Service(Id.<Service>random(), Id.<Node>random(), randomWord(), randomWord(), randomWord(), Collections.<String, String>emptyMap()));
        }
        
        QueryEvent.Builder builder = discoveryEvents.getQueryEventBuilder();
        if (success) {
            builder.setSuccess();
        }
        builder.setServiceSet(serviceSet);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), QueryEvent.class);
        QueryEvent postedEvent = (QueryEvent)postedObject;
        Assert.assertEquals(postedEvent.getType(), null);
        Assert.assertEquals(postedEvent.getPool(), null);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        Assert.assertEquals(postedEvent.getResultCount(), serviceSet.size());
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetStaticAnnouncementEventBuilder()
    {
        boolean success = RandomUtils.nextBoolean();
        Id<Service> id = Id.random();
        String env = randomWord();
        String pool = randomWord();
        String loc = randomWord();
        String type = randomWord();
        String address = randomWord();
        Map<String, String> props = new HashMap<String, String>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            props.put(randomWord(), randomWord());
        }
        
        StaticAnnouncementEvent.Builder builder = discoveryEvents.getStaticAnnouncementEventBuilder();
        StaticAnnouncement announcement = new StaticAnnouncement(env, type, pool, loc, props);
        builder.setAnnouncement(announcement);
        builder.setId(id);
        if (success) {
            builder.setSuccess();
        }
        builder.setRemoteAddress(address);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), StaticAnnouncementEvent.class);
        StaticAnnouncementEvent postedEvent = (StaticAnnouncementEvent)postedObject;
        Assert.assertEquals(postedEvent.getId(), id);
        Assert.assertEquals(postedEvent.getType(), type);
        Assert.assertEquals(postedEvent.getPool(), pool);
        Assert.assertEquals(postedEvent.getRemoteAddress(), address);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        Assert.assertEquals(postedEvent.getProperties(), props.toString());
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetStaticDeleteEventBuilder()
    {
        boolean success = RandomUtils.nextBoolean();
        Id<Service> id = Id.random();
        String address = randomWord();
        Map<String, String> props = new HashMap<String, String>();
        for (int i = RandomUtils.nextInt(300); i > 0; --i) {
            props.put(randomWord(), randomWord());
        }
        
        StaticDeleteEvent.Builder builder = discoveryEvents.getStaticDeleteEventBuilder(id);
        if (success) {
            builder.setSuccess();
        }
        builder.setRemoteAddress(address);
        builder.post();
        
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Object postedObject = eventClient.getEvents().get(0);
        Assert.assertEquals(postedObject.getClass(), StaticDeleteEvent.class);
        StaticDeleteEvent postedEvent = (StaticDeleteEvent)postedObject;
        Assert.assertEquals(postedEvent.getId(), id);
        Assert.assertEquals(postedEvent.getRemoteAddress(), address);
        Assert.assertEquals(postedEvent.isSuccess(), success);
        assertDuration (postedEvent.getDuration());
    }

    @Test
    public void testGetStaticListEventBuilder()
    {
        StaticListEvent.Builder builder = discoveryEvents.getStaticListEventBuilder();
        builder.post();
        Assert.assertEquals(eventClient.getEvents().size(), 1);
        Assert.assertEquals(eventClient.getEvents().get(0).getClass(), StaticListEvent.class);
    }

    private void assertDuration (double d)
    {
        Assert.assertTrue (d < 30000d, String.valueOf(d));
        Assert.assertTrue (d > 0d, String.valueOf(d));
    }
    
    private static final Set<String> s_usedRandomWords = Collections.synchronizedSet(new HashSet<String>(128));
    private static String randomWord()
    {
        String s;
        do {
            s = RandomStringUtils.random(1 + RandomUtils.nextInt(300));
        } while (!s_usedRandomWords.add(s));
        return s;
    }
}
