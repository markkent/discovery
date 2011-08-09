package com.proofpoint.discovery;

import com.proofpoint.cassandra.testing.CassandraServerSetup;
import com.proofpoint.node.NodeInfo;
import me.prettyprint.hector.api.Cluster;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import javax.inject.Provider;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;

public class TestCassandraDynamicStore
        extends TestDynamicStore
{
    private final static AtomicLong counter = new AtomicLong(0);
    private CassandraDynamicStore cassandraStore;

    @Override
    protected DynamicStore initializeStore(DiscoveryConfig config, Provider<DateTime> timeProvider)
    {
        CassandraStoreConfig storeConfig = new CassandraStoreConfig()
                .setKeyspace("test_cassandra_dynamic_store" + counter.incrementAndGet());

        Cluster cluster = new DiscoveryModule().getCluster(CassandraServerSetup.getServerInfo(), new NodeInfo("testing"));
        cassandraStore = new CassandraDynamicStore(storeConfig, config, timeProvider, cluster);
        Assert.assertTrue(new CassandraSchemaInitialization(cluster, storeConfig).waitForInit());
        cassandraStore.initialize();
        //somehow the first reload on the initialize kicks in delayed and so the tests fail since looks like the reload gets called in along with the explicit reload
        // resulting in unexpected stats and hence test failures
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            //do nothing
        }

        return new DynamicStore()
        {
            @Override
            public boolean put(Id<Node> nodeId, DynamicAnnouncement announcement)
            {
                return cassandraStore.put(nodeId, announcement);
            }

            @Override
            public boolean delete(Id<Node> nodeId)
            {
                return cassandraStore.delete(nodeId);
            }

            @Override
            public Set<Service> getAll()
            {
                cassandraStore.reload();
                return cassandraStore.getAll();
            }

            @Override
            public Set<Service> get(String type)
            {
                cassandraStore.reload();
                return cassandraStore.get(type);
            }

            @Override
            public Set<Service> get(String type, String pool)
            {
                cassandraStore.reload();
                return cassandraStore.get(type, pool);
            }
        };
    }

    @BeforeSuite
    public void setupCassandra()
            throws IOException, TTransportException, ConfigurationException, InterruptedException
    {
        CassandraServerSetup.tryInitialize();
    }

    @AfterSuite
    public void teardownCassandra()
            throws IOException
    {
        CassandraServerSetup.tryShutdown();
    }

    @AfterMethod
    public void teardown()
    {
        cassandraStore.shutdown();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Already initialized")
    public void testCannotBeInitalizedTwice()
    {
        cassandraStore.initialize();
    }

    @Override
    @Test
    public void testEmpty()
    {
        long count = cassandraStore.getDynamicStoreLoadAllStats().getCount();
        super.testEmpty();
        assertEquals(cassandraStore.getDynamicStoreLoadAllStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testPutSingle()
    {
        long count = cassandraStore.getDynamicStorePutStats().getCount();
        super.testPutSingle();
        assertEquals(cassandraStore.getDynamicStorePutStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testExpires()
    {
        super.testExpires();
    }

    @Override
    @Test
    public void testPutMultipleForSameNode()
    {
        long count = cassandraStore.getDynamicStorePutStats().getCount();
        super.testPutMultipleForSameNode();
        assertEquals(cassandraStore.getDynamicStorePutStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testReplace()
    {
        long count = cassandraStore.getDynamicStorePutStats().getCount();
        super.testReplace();
        assertEquals(cassandraStore.getDynamicStorePutStats().getCount() - count, 2);
    }

    @Override
    @Test
    public void testReplaceExpired()
    {
        long count = cassandraStore.getDynamicStorePutStats().getCount();
        super.testReplaceExpired();
        assertEquals(cassandraStore.getDynamicStorePutStats().getCount() - count, 2);
    }

    @Override
    @Test
    public void testPutMultipleForDifferentNodes()
    {
        long count = cassandraStore.getDynamicStorePutStats().getCount();
        super.testPutMultipleForDifferentNodes();
        assertEquals(cassandraStore.getDynamicStorePutStats().getCount() - count, 3);
    }

    @Override
    @Test
    public void testGetByType()
    {
        long count = cassandraStore.getDynamicStoreLoadAllStats().getCount();
        super.testGetByType();
        assertEquals(cassandraStore.getDynamicStoreLoadAllStats().getCount() - count, 2);
    }

    @Override
    @Test
    public void testGetByTypeAndPool()
    {
        long count = cassandraStore.getDynamicStoreLoadAllStats().getCount();
        super.testGetByTypeAndPool();
        assertEquals(cassandraStore.getDynamicStoreLoadAllStats().getCount() - count, 3);
    }

    @Override
    @Test
    public void testDelete()
    {
        long count = cassandraStore.getDynamicStoreDeleteStats().getCount();
        super.testDelete();
        assertEquals(cassandraStore.getDynamicStoreDeleteStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testDeleteThenReInsert()
    {
        super.testDeleteThenReInsert();
    }

    @Override
    @Test
    public void testCanHandleLotsOfAnnouncements()
    {
        super.testCanHandleLotsOfAnnouncements();
    }

}
