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
package org.neo4j.ogm.drivers.rpc.driver;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.alipay.tugraph.TuGraphRpcClient;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.Credentials;
import org.neo4j.ogm.config.UsernamePasswordCredentials;
import org.neo4j.ogm.driver.AbstractConfigurableDriver;
import org.neo4j.ogm.driver.ExceptionTranslator;
import org.neo4j.ogm.drivers.rpc.request.RpcRequest;
import org.neo4j.ogm.drivers.rpc.transaction.RpcTransaction;
import org.neo4j.ogm.exception.ConnectionException;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.transaction.Transaction;
import org.neo4j.ogm.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Michael J. Simons
 */
public class RpcDriver extends AbstractConfigurableDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcDriver.class);
    public static final String CONFIG_PARAMETER_RPC_LOGGING = "Rpc_Logging";

    private final ExceptionTranslator exceptionTranslator = new RpcDriverExceptionTranslator();

    private TuGraphRpcClient rpcClient;
    private Credentials credentials;
    private Config driverConfig;
    /**
     * The database to use, defaults to {@literal null} (Use Neo4j default).
     */
    private String database = null;

    // required for service loader mechanism
    public RpcDriver() {
    }

    public RpcDriver(TuGraphRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public void configure(Configuration newConfiguration) {

        close();

        super.configure(newConfiguration);

        this.driverConfig = buildDriverConfig();
        this.credentials = this.configuration.getCredentials();
        this.database = this.configuration.getDatabase();

        if (this.configuration.getVerifyConnection()) {
            checkClientInitialized();
        }
    }

    @Override
    protected String getTypeSystemName() {
        return "org.neo4j.ogm.drivers.rpc.types.RpcNativeTypes";
    }

    public Function<TransactionManager, BiFunction<Transaction.Type, Iterable<String>, Transaction>> getTransactionFactorySupplier() {
        return transactionManager -> (type, bookmarks) -> {
            checkClientInitialized();
            return new RpcTransaction(transactionManager, rpcClient, type);
        };
    }

    @Override
    public void close() {
        try {
            LOGGER.info("Shutting down rpc client {} ", this);
            if (rpcClient != null) {
                rpcClient.stopClient();
            }
        } catch (Exception e) {
            LOGGER.warn("Unexpected Exception when closing tugraph client rpcClient: ", e);
        }
    }

    private void checkClientInitialized() {
        TuGraphRpcClient client = rpcClient;
        if (client == null) {
            synchronized (this) {
                client = rpcClient;
                if (client == null) {
                    createRpcClient();
                }
            }
        }
    }

    static boolean isCorrectScheme(String scheme) {
        String lowerCaseScheme = scheme.toLowerCase(Locale.ENGLISH);
        return  "list".equals(lowerCaseScheme);
    }

    private void createRpcClient() {

        final String serviceUnavailableMessage = "Could not create Rpc client instance";

        try {
            if (credentials != null) {
                UsernamePasswordCredentials usernameAndPassword = (UsernamePasswordCredentials) this.credentials;
                TuGraphRpcClient client = new TuGraphRpcClient(configuration.getURI(), usernameAndPassword.getUsername(), usernameAndPassword.getPassword());
                rpcClient = client;
            } else {
                LOGGER.debug("Rpc Driver credentials not supplied");
            }
        } catch (ServiceUnavailableException e) {
            throw new ConnectionException(serviceUnavailableMessage, e);
        }
    }

    private URI getSingleURI(String singleUri) {
        return URI.create(singleUri);
    }

    @Override
    public ExceptionTranslator getExceptionTranslator() {
        return this.exceptionTranslator;
    }

    @Override
    public Request request(Transaction transaction) {
        return new RpcRequest(rpcClient, this.parameterConversion, getCypherModification());
    }

    public <T> T unwrap(Class<T> clazz) {

        if (clazz == Driver.class) {
            return (T) rpcClient;
        } else {
            return super.unwrap(clazz);
        }
    }

    private Optional<Logging> getRpcLogging() {

        Object possibleLogging = customPropertiesSupplier.get().get(CONFIG_PARAMETER_RPC_LOGGING);
        if (possibleLogging != null && !(possibleLogging instanceof Logging)) {
            LOGGER.warn("Invalid object of type {} for {}, not changing log.", possibleLogging.getClass(),
                CONFIG_PARAMETER_RPC_LOGGING);
            possibleLogging = null;
        }

        LOGGER.debug("Using {} for rpc logging.", possibleLogging == null ? "default" : possibleLogging.getClass());

        return Optional.ofNullable((Logging) possibleLogging);
    }

    private Config buildDriverConfig() {

        // Done outside the try/catch and explicity catch the illegalargument exception of singleURI
        // so that exception semantics are not changed since we introduced that feature.

        URI singleUri = getSingleURI(configuration.getURI());
        if (!isCorrectScheme(singleUri.getScheme())) {
            throw new IllegalArgumentException(
                "Rpc uri is incorrect!");
        }

        try {
            Config.ConfigBuilder configBuilder = Config.builder();
            configBuilder.withMaxConnectionPoolSize(configuration.getConnectionPoolSize());

            if (configuration.getConnectionLivenessCheckTimeout() != null) {
                configBuilder.withConnectionLivenessCheckTimeout(configuration.getConnectionLivenessCheckTimeout(),
                    TimeUnit.MILLISECONDS);
            }

            getRpcLogging().ifPresent(configBuilder::withLogging);

            return configBuilder.build();
        } catch (Exception e) {
            throw new ConnectionException("Unable to build driver configuration", e);
        }
    }

}
