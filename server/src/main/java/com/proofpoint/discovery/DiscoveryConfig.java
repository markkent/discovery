package com.proofpoint.discovery;

import com.proofpoint.configuration.Config;
import com.proofpoint.units.Duration;
import com.proofpoint.units.MinDuration;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

public class DiscoveryConfig
{
    private Duration maxAge = new Duration(30, TimeUnit.SECONDS);
    private int statsWindowSize = 5000;
    private Duration dynamicServiceCacheRefresh = new Duration(1, TimeUnit.SECONDS);
    private Duration staticServiceCacheRefresh = new Duration(5, TimeUnit.SECONDS);


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

    @MinDuration(value="1s", message="must be greater than or equal to 1s")
    public Duration getDynamicServiceCacheRefresh()
    {
        return dynamicServiceCacheRefresh;
    }

    @Config("discovery.dynamic-services-cache-refresh-duration")
    public DiscoveryConfig setDynamicServiceCacheRefresh(Duration dynamicServiceCacheRefresh)
    {
        this.dynamicServiceCacheRefresh = dynamicServiceCacheRefresh;
        return this;
    }

    @MinDuration(value="5s", message="must be greater than or equal to 5s")
    public Duration getStaticServiceCacheRefresh()
    {
        return staticServiceCacheRefresh;
    }

    @Config("discovery.static-services-cache-refresh-duration")
    public DiscoveryConfig setStaticServiceCacheRefresh(Duration staticServiceCacheRefresh)
    {
        this.staticServiceCacheRefresh = staticServiceCacheRefresh;
        return this;
    }


}
