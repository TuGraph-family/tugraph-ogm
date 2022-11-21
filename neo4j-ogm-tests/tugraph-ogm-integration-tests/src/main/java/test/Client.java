/*
 * Copyright (c) 2002-2022 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.rpc.driver.RpcDriver;

public class Client {
    private static Configuration.Builder baseConfigurationBuilder;

    protected static Driver getDriver(String[] args) {
        String databaseUri = "list://" + args[0];
        String username = args[1];
        String password = args[2];

        Driver driver = new RpcDriver();
        baseConfigurationBuilder = new Configuration.Builder()
            .uri(databaseUri)
            .verifyConnection(true)
            .credentials(username, password);
        driver.configure(baseConfigurationBuilder.build());
        return driver;
    }
}
