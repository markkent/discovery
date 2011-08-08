package com.proofpoint.discovery;

import com.google.common.collect.ImmutableMap;
import com.proofpoint.configuration.testing.ConfigAssertions;
import com.proofpoint.units.Duration;
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
                .setStatsWindowSize(5000));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = ImmutableMap.<String, String>builder()
                .put("discovery.max-age", "1m")
                .put("discovery.timed-statistics-window-size", "1000")
                .build();

        DiscoveryConfig expected = new DiscoveryConfig()
                .setMaxAge(new Duration(1, TimeUnit.MINUTES))
                .setStatsWindowSize(1000);

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
}
