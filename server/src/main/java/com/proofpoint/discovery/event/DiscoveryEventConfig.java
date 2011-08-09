package com.proofpoint.discovery.event;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.base.Preconditions;
import com.proofpoint.configuration.Config;

import org.weakref.jmx.Managed;

/**
 * Events can be helpful or noisy. Configure which are recorded.
 * Configure with a list of enums or an asterisk for all.
 */
public class DiscoveryEventConfig
{
    private Set<DiscoveryEventType> enabledEvents = Collections.emptySet();

    public DiscoveryEventConfig()
    {
        setEnabledEvents("*");
    }

    @Managed
    @Config("discovery.events.enabledlist")
    public DiscoveryEventConfig setEnabledEvents(String events)
    {
        Preconditions.checkNotNull(events, "events is null");
        enabledEvents = enumListToEnumMap(events);
        return this;
    }

    @Managed
    public String getEnabledEvents()
    {
        return enumMaptoEnumList(enabledEvents);
    }

    private static Set<DiscoveryEventType> enumListToEnumMap(final String list)
    {
        Preconditions.checkNotNull(list, "list is null");
        EnumSet<DiscoveryEventType> newMap;

        if (list.trim().equals("*")) {
            newMap = EnumSet.allOf(DiscoveryEventType.class);
        }
        else {
            newMap = EnumSet.noneOf(DiscoveryEventType.class);

            StringTokenizer tok = new StringTokenizer(list, ", \t\n\r\f", false);
            while (tok.hasMoreElements()) {
                newMap.add(DiscoveryEventType.valueOf(tok.nextToken()));
            }
        }

        return Collections.unmodifiableSet(newMap);
    }

    private static String enumMaptoEnumList(Set<DiscoveryEventType> map)
    {
        StringBuilder buf = new StringBuilder();
        for (DiscoveryEventType e : map) {
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(e.name());
        }
        return buf.toString();
    }

    public boolean isEventEnabled(DiscoveryEventType event)
    {
        return enabledEvents.contains(event);
    }
}
