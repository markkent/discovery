package com.proofpoint.discovery;

import com.proofpoint.cassandra.testing.CassandraServerSetup;
import com.proofpoint.node.NodeInfo;
import me.prettyprint.hector.api.Cluster;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;

public class TestCassandraStaticStore
    extends TestStaticStore
{
    private final static AtomicLong counter = new AtomicLong(0);
    private CassandraStaticStore staticStore;

    @Override
    protected StaticStore initializeStore()
    {
        CassandraStoreConfig storeConfig = new CassandraStoreConfig()
                .setKeyspace("test_cassandra_static_store" + counter.incrementAndGet());

        Cluster cluster = new DiscoveryModule().getCluster(CassandraServerSetup.getServerInfo(), new NodeInfo("testing"));

        Assert.assertTrue(new CassandraSchemaInitialization(cluster, storeConfig).waitForInit());
        staticStore = new CassandraStaticStore(storeConfig, cluster, new TestingTimeProvider(), new DiscoveryConfig());
 
        return new StaticStore()
        {
            @Override
            public void put(Service service)
            {
                staticStore.put(service);
            }

            @Override
            public void delete(Id<Service> nodeId)
            {
                staticStore.delete(nodeId);
            }

            @Override
            public Set<Service> getAll()
            {
                staticStore.reload();
                return staticStore.getAll();
            }

            @Override
            public Set<Service> get(String type)
            {
                staticStore.reload();
                return staticStore.get(type);
            }

            @Override
            public Set<Service> get(String type, String pool)
            {
                staticStore.reload();
                return staticStore.get(type, pool);
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

    @Override
    @Test
    public void testEmpty()
    {
        long count = staticStore.getStoreLoadAllStats().getCount();
        super.testEmpty();
        assertEquals(staticStore.getStoreLoadAllStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testPutSingle()
    {
        long count = staticStore.getStorePutStats().getCount();
        super.testPutSingle();
        assertEquals(staticStore.getStorePutStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testPutMultiple()
    {
        long count = staticStore.getStorePutStats().getCount();
        super.testPutMultiple();
        assertEquals(staticStore.getStorePutStats().getCount() - count, 3);
    }

    @Override
    @Test
    public void testGetByType()
    {
        long count = staticStore.getStoreLoadAllStats().getCount();
        super.testGetByType();
        assertEquals(staticStore.getStoreLoadAllStats().getCount() - count, 2);

    }

    @Override
    @Test
    public void testGetByTypeAndPool()
    {
        long count = staticStore.getStoreLoadAllStats().getCount();
        super.testGetByTypeAndPool();
        assertEquals(staticStore.getStoreLoadAllStats().getCount() - count, 3);
    }

    @Override
    @Test
    public void testDelete()
    {
        long count = staticStore.getStoreDeleteStats().getCount();
        super.testDelete();
        assertEquals(staticStore.getStoreDeleteStats().getCount() - count, 1);
    }

    @Override
    @Test
    public void testCanHandleLotsOfServices()
    {
        super.testCanHandleLotsOfServices();
    }
}
