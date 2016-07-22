/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.jdbc;

import herddb.client.ClientConfiguration;
import herddb.client.HDBClient;
import herddb.server.Server;
import herddb.server.ServerConfiguration;
import herddb.server.StaticClientSideMetadataProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * test switch tablespace using sql esplicit schema references
 *
 * @author enrico.olivelli
 */
public class SwitchTableSpaceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test() throws Exception {
        try (Server server = new Server(new ServerConfiguration(folder.newFolder().toPath()))) {
            server.start();
            try (HDBClient client = new HDBClient(new ClientConfiguration(folder.newFolder().toPath()));) {
                client.setClientSideMetadataProvider(new StaticClientSideMetadataProvider(server));
                try (AbstractHerdDBDataSource dataSource = new AbstractHerdDBDataSource(client);
                        Connection con = dataSource.getConnection();
                        Statement statement = con.createStatement();) {
                    statement.execute("CREATE TABLESPACE 'ts1'");
                    statement.execute("CREATE TABLESPACE 'ts2'");
                    server.waitForTableSpaceBoot("ts1", true);
                    server.waitForTableSpaceBoot("ts2", true);

                    statement.execute("CREATE TABLE ts1.mytable (key string primary key, name string)");
                    statement.execute("CREATE TABLE ts2.mytable2 (key string primary key, name string)");
                    con.setAutoCommit(true);
                    assertEquals(1, statement.executeUpdate("INSERT INTO ts1.mytable (key,name) values('k1','name1')"));
                    assertEquals("ts1", con.getSchema());

                    assertEquals(1, statement.executeUpdate("INSERT INTO ts2.mytable2 (key,name) values('k1','name1')"));
                    assertEquals("ts2", con.getSchema());

                }
            }
        }
    }
}