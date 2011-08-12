package com.proofpoint.discovery;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;
import com.proofpoint.bootstrap.Bootstrap;
import com.proofpoint.cassandra.CassandraModule;
import com.proofpoint.discovery.client.Announcer;
import com.proofpoint.discovery.client.DiscoveryClient;
import com.proofpoint.http.server.HttpServerModule;
import com.proofpoint.jaxrs.JaxrsModule;
import com.proofpoint.jmx.JmxModule;
import com.proofpoint.json.JsonModule;
import com.proofpoint.log.Logger;
import com.proofpoint.node.NodeModule;

public class Main
{
    private final static Logger log = Logger.get(Main.class);

    public static void main(String[] args)
            throws Exception
    {
        try {
            Bootstrap app = new Bootstrap(new NodeModule(),
                    new HttpServerModule(),
                    new JaxrsModule(),
                    new JsonModule(),
                    new JmxModule(),
                    new DiscoveryModule(),
                    new CassandraModule(),
                    Modules.override(new com.proofpoint.discovery.client.DiscoveryModule()).with(new Module()
                    {
                        @Override
                        public void configure(Binder binder)
                        {
                            binder.bind(DiscoveryClient.class).to(LocalDiscoveryClient.class).in(Scopes.SINGLETON);
                        }
                    }));

            Injector injector = app.initialize();
            Announcer announcer = injector.getInstance(Announcer.class);
            announcer.start();

//            HttpServerInfo serverInfo = injector.getInstance(HttpServerInfo.class);
//            StaticStore store = injector.getInstance(StaticStore.class);
//            NodeInfo nodeInfo = injector.getInstance(NodeInfo.class);
//
//            store.put(
//                    new Service(
//                            Id.<Service>valueOf(nodeInfo.getInstanceId()),
//                            Id.<Node>valueOf(nodeInfo.getNodeId()),
//                            "discovery",
//                            nodeInfo.getPool(),
//                            nodeInfo.getLocation(),
//                            ImmutableMap.<String, String>of(
//                                    "http", serverInfo.getHttpUri().toString(),
//                                    "https", serverInfo.getHttpsUri().toString())));
        }
        catch (Exception e) {
            log.error(e);
            // Cassandra prevents the vm from shutting down on its own
            System.exit(1);
        }
    }
}