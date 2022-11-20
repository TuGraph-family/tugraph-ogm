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
package org.neo4j.ogm.drivers.rpc.transaction;

import com.alipay.tugraph.TuGraphRpcClient;

import org.neo4j.ogm.transaction.AbstractTransaction;
import org.neo4j.ogm.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Bickers
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class RpcTransaction extends AbstractTransaction {

    private final TuGraphRpcClient rpcClient;
    private final Logger LOGGER = LoggerFactory.getLogger(RpcTransaction.class);

    public RpcTransaction(TransactionManager transactionManager, TuGraphRpcClient rpcClient, Type type) {
        super(transactionManager);
        this.rpcClient = rpcClient;
        this.type = type;
    }

    @Override
    public void rollback() {
        // TuGraph rpc do not use transaction
    }

    @Override
    public void commit() {
        // TuGraph rpc do not use transaction
    }

}
