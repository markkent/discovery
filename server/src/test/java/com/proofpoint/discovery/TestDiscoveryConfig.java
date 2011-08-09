package com.proofpoint.discovery;

import com.google.common.collect.ImmutableMap;
import com.proofpoint.configuration.testing.ConfigAssertions;
import com.proofpoint.units.Duration;
import com.proofpoint.units.MinDuration;
import org.testng.annotations.Test;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.proofpoint.experimental.testing.ValidationAssertions.assertFailsValidation;

public class TestDiscoveryConfig
{
    @Test
    public void testDefaults()
    {
        ConfigAssertions.assertRecordedDefaults(ConfigAssertions.recordDefaults(DiscoveryConfig.class)
                .setMaxAge(new Duration(30, TimeUnit.SECONDS))
                .setStatsWindowSize(5000)
                .setDynamicServiceCacheRefresh(new Duration(1,TimeUnit.SECONDS))
                .setStaticServiceCacheRefresh(new Duration(5,TimeUnit.SECONDS)));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = ImmutableMap.<String, String>builder()
                .put("discovery.max-age", "1m")
                .put("discovery.timed-statistics-window-size", "1000")
                .put("discovery.static-services-cache-refresh-duration","10s")
                .put("discovery.dynamic-services-cache-refresh-duration","11s")
                .build();

        DiscoveryConfig expected = new DiscoveryConfig()
                .setMaxAge(new Duration(1, TimeUnit.MINUTES))
                .setStatsWindowSize(1000)
                .setDynamicServiceCacheRefresh(new Duration(11, TimeUnit.SECONDS))
                .setStaticServiceCacheRefresh(new Duration(10, TimeUnit.SECONDS));

        ConfigAssertions.assertFullMapping(properties, expected);
    }


    @Test
    public void testValidatesNotNullDuration()
    {
        DiscoveryConfig config = new DiscoveryConfig().setMaxAge(null);

        assertFailsValidation(config, "maxAge", "may not be null", NotNull.class);
    }

    @Test
    public void testValidatesMinimumStatsWindow()
    {
        DiscoveryConfig config = new DiscoveryConfig().setStatsWindowSize(50);
        assertFailsValidation(config, "statsWindowSize", "must be greater than or equal to 100", Min.class);
    }

    @Test
    public void testValidatesMinimumStaticRefreshDuration()
    {
        DiscoveryConfig config = new DiscoveryConfig().setStaticServiceCacheRefresh(new Duration(1,TimeUnit.SECONDS));
        assertFailsValidation(config, "staticServiceCacheRefresh", "must be greater than or equal to 5s", MinDuration.class);
    }

    @Test
    public void testValidatesMinimumDynamicRefreshDuration()
    {
        DiscoveryConfig config = new DiscoveryConfig().setDynamicServiceCacheRefresh(new Duration(1, TimeUnit.MILLISECONDS));
        assertFailsValidation(config, "dynamicServiceCacheRefresh", "must be greater than or equal to 1s", MinDuration.class);
    }
}
