/*
 * Copyright 2022 "Ant Group"
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

import com.antgroup.tugraph.ogm.config.Configuration;
import com.antgroup.tugraph.ogm.driver.Driver;
import com.antgroup.tugraph.ogm.drivers.rpc.driver.RpcDriver;

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
