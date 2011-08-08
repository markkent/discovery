package com.proofpoint.discovery;

import com.proofpoint.configuration.Config;
import com.proofpoint.units.Duration;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

public class DiscoveryConfig
{
    private Duration maxAge = new Duration(30, TimeUnit.SECONDS);
    private int statsWindowSize = 5000;


    @NotNull
    public Duration getMaxAge()
    {
        return maxAge;
    }

    @Config("discovery.max-age")
    public DiscoveryConfig setMaxAge(Duration maxAge)
    {
        this.maxAge = maxAge;
        return this;
    }

    @Min(100)
    public int getStatsWindowSize()
    {
        return statsWindowSize;
    }

    @Config("discovery.timed-statistics-window-size")
    public DiscoveryConfig setStatsWindowSize(int statsWindowSize)
    {
        this.statsWindowSize = statsWindowSize;
        return this;
    }


}
