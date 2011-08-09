package com.proofpoint.discovery.event;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDiscoveryEventConfig
{
    @Test
    public void testDefaults()
    {
        DiscoveryEventConfig config = new DiscoveryEventConfig();
        for (DiscoveryEventType e : DiscoveryEventType.values()) {
            Assert.assertTrue(config.isEventEnabled(e), e.name());
        }
    }
    
    @Test
    public void testAllOn()
    {
        DiscoveryEventConfig config = new DiscoveryEventConfig().setEnabledEvents("*");
        for (DiscoveryEventType e : DiscoveryEventType.values()) {
            Assert.assertTrue(config.isEventEnabled(e), e.name());
        }
    }
    
    @Test
    public void testPermutations()
    {
        DiscoveryEventConfig config = new DiscoveryEventConfig().setEnabledEvents("*");

        final DiscoveryEventType all[] = DiscoveryEventType.values();
        Assert.assertTrue(all.length <= 30, "Too many event types defined for a full test");
        Assert.assertTrue(all.length > 0, "No event types defined");
        
        final int testEnd = 1 << all.length;
        int bits;
        for (int i = 0; i < testEnd; ++i)
        {
            StringBuilder buf = new StringBuilder (1024);
            bits = i;
            for (DiscoveryEventType e : all) {
                if ((bits & 1) == 1) {
                    buf.append(e.name()).append(',');
                }
                bits = bits >>> 1;
            }
            
            config.setEnabledEvents(buf.toString());
            
            bits = i;
            for (DiscoveryEventType e : all) {
                Assert.assertEquals(config.isEventEnabled(e), (bits & 1) == 1, e.name());
                bits = bits >>> 1;
            }
        }
    }
}
