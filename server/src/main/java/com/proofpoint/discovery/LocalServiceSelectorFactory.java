package com.proofpoint.discovery;

import static com.google.common.collect.Sets.union;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.proofpoint.discovery.client.ServiceDescriptor;
import com.proofpoint.discovery.client.ServiceSelector;
import com.proofpoint.discovery.client.ServiceSelectorConfig;
import com.proofpoint.discovery.client.ServiceSelectorFactory;

public class LocalServiceSelectorFactory implements ServiceSelectorFactory
{
    final DynamicStore dynamicStore;
    final StaticStore staticStore;
    private final ConcurrentHashMap<List<String>, ServiceSelector> serviceSelectorCache = new ConcurrentHashMap<List<String>, ServiceSelector>();
    
    @Inject
    public LocalServiceSelectorFactory (DynamicStore dynamicStore, StaticStore staticStore)
    {
        this.dynamicStore = dynamicStore;
        this.staticStore = staticStore;
    }
    
    @Override
    public ServiceSelector createServiceSelector(final String type, ServiceSelectorConfig selectorConfig)
    {
        List<String> key = Arrays.asList(type, selectorConfig.getPool());
        ServiceSelector cachedSelector;
        while ((cachedSelector = serviceSelectorCache.get(key)) == null) {
            serviceSelectorCache.putIfAbsent(key, new LocalServiceSelector (type, selectorConfig.getPool()));
        }
        return cachedSelector;
    }
    
    class LocalServiceSelector implements ServiceSelector
    {
        private final String type, pool;

        //There are race conditions where multiple threads may perform cache refreshes at once:
        // - The cache doesn't exist yet
        // - The cache is older than tooOldMillis
        // - The cache is refreshing slower than refreshIntervalMillis
        // These races must be harmless.  Make sure refreshes are idempotent and there's CPU time to spare.
        private final long refreshIntervalMillis = 5000L;  //One thread will try a cache refresh while others may get older data
        private final long tooOldMillis = 30000L;          //Must refresh cache at this age
        private final AtomicLong lastUpdate = new AtomicLong(0);
        private List<ServiceDescriptor> serviceDescriptorCache = null;  //Pointer swap of immutable data.
        
        LocalServiceSelector (String type, String pool)
        {
            this.type = type;
            this.pool = pool;
        }
        
        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getPool()
        {
            return pool;
        }

        /**
         * Debugging finds that this method is called at extremely high rates.  It must be very fast.
         * @see com.proofpoint.discovery.client.ServiceSelector#selectAllServices()
         */
        @Override
        public List<ServiceDescriptor> selectAllServices()
        {
            final long now = System.currentTimeMillis();
            List<ServiceDescriptor> cachedList = serviceDescriptorCache;
            final long lastCacheUpdate = lastUpdate.get();
            final long cacheAge = now - lastCacheUpdate;
            if ((cachedList == null) || (cacheAge > refreshIntervalMillis)) {
                boolean updatedClock = lastUpdate.compareAndSet(lastCacheUpdate, now);
                if (updatedClock || (cachedList == null) || (cacheAge > tooOldMillis)) {
                    Set<Service> serviceSet = union(dynamicStore.get(type, pool), staticStore.get(type, pool));
                    ArrayList<ServiceDescriptor> serviceList = new ArrayList<ServiceDescriptor>(serviceSet.size());
                    for (Service service : serviceSet) {
                        serviceList.add(new ServiceDescriptor(service.getId().get(), service.getNodeId().get().toString(), service.getType(), service.getPool(), service.getLocation(), service
                                .getProperties()));
                    }
                    serviceDescriptorCache = cachedList = Collections.unmodifiableList(serviceList);
                }
            }
            return cachedList;
        }
    }
}