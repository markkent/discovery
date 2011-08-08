package com.proofpoint.discovery;

import javax.annotation.PostConstruct;

import com.google.inject.Inject;
import com.proofpoint.log.Logger;

import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import static com.proofpoint.discovery.ColumnFamilies.named;
import static com.google.common.collect.Iterables.find;

/**
 * There are so many things wrong with this class. Please delete ASAP.
 * 
 * EmbeddedCassandraServer starts up asynchronously so Cassandra column family initialization code
 * in CassandraDynamicStore and CassandraStaticStore usually failed to run.
 * 
 * CassandraSchemaInitialization is the initialization code extracted from CassandraDynamicStore and CassandraStaticStore,
 * then wrapped in a retry loop.
 */
public class CassandraSchemaInitialization
{
    private static final int GC_GRACE_SECONDS = 0; // don't care about columns being brought back from the dead
    private final static String DYNAMIC_COLUMN_FAMILY = CassandraDynamicStore.COLUMN_FAMILY;
    private final static String STATIC_COLUMN_FAMILY = CassandraStaticStore.COLUMN_FAMILY;

    final Cluster cluster;
    final CassandraStoreConfig config;
    private boolean startingInit = false;
    private boolean initialized = false;
    private boolean done = false;
    

    @Inject
    public CassandraSchemaInitialization(Cluster cluster, CassandraStoreConfig config)
    {
        this.cluster = cluster;
        this.config = config;
    }
    
    public synchronized boolean waitForInit ()
    {
        init();
        while (!done) {
            try {
                wait();
            }
            catch (InterruptedException e) {
            }
        }
        return initialized;
    }

    @PostConstruct
    public synchronized void init()
    {
        if (startingInit) {
            return;
        }
        final Logger log = Logger.get(CassandraSchemaInitialization.class);

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    for (int i = 0; i < 30; ++i) {
                        try {
                            String keyspaceName = config.getKeyspace();
                            KeyspaceDefinition definition = cluster.describeKeyspace(keyspaceName);
                            if (definition == null) {
                                cluster.addKeyspace(new ThriftKsDef(keyspaceName));
                            }
                            
                            HFactory.createKeyspace(keyspaceName, cluster);

                            // Dynamic announcements
                            ColumnFamilyDefinition existing = find(cluster.describeKeyspace(keyspaceName).getCfDefs(), named(DYNAMIC_COLUMN_FAMILY), null);
                            if (existing == null) {
                                cluster.addColumnFamily(new ThriftCfDef(keyspaceName, DYNAMIC_COLUMN_FAMILY));
                                cluster.addColumnFamily(withDefaults(new ThriftCfDef(keyspaceName, DYNAMIC_COLUMN_FAMILY)));
                            }
                            else if (needsUpdate(existing)) {
                                cluster.updateColumnFamily(withDefaults(existing));
                            }

                            // Static announcements
                            if (find(cluster.describeKeyspace(keyspaceName).getCfDefs(), named(STATIC_COLUMN_FAMILY), null) == null) {
                                cluster.addColumnFamily(new ThriftCfDef(keyspaceName, STATIC_COLUMN_FAMILY));
                            }

                            initialized = true;
                            return;
                        }
                        catch (HectorException err) {
                            log.warn(err, "Error initializing Cassandra");
                            try {
                                Thread.sleep(1000L);
                            }
                            catch (InterruptedException e) {
                                // Don't care
                            }
                        }
                    }
                    log.error("Could not initialize Cassandra");
                }
                finally {
                    synchronized (CassandraSchemaInitialization.this) {
                        done = true;
                        CassandraSchemaInitialization.this.notifyAll();
                    }
                }
            }
        }).start();
        
        startingInit = true;
    }

    private static boolean needsUpdate(ColumnFamilyDefinition definition)
    {
        return definition.getGcGraceSeconds() != GC_GRACE_SECONDS;
    }

    private static ColumnFamilyDefinition withDefaults(ColumnFamilyDefinition original)
    {
        ThriftCfDef updated = new ThriftCfDef(original);
        updated.setGcGraceSeconds(GC_GRACE_SECONDS);
        return updated;
    }
}
