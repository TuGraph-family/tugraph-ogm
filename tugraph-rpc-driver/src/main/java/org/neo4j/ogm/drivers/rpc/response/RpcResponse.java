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
package org.neo4j.ogm.drivers.rpc.response;

import java.util.ArrayList;

import org.neo4j.driver.exceptions.ClientException;

import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public abstract class RpcResponse<T> implements Response {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcResponse.class);

    protected final String result;
    protected final ArrayList<T> models;
    protected static int it = 0;

    RpcResponse(String result) {
        it = 0;
        this.result = result;
        this.models = mappingResultToModel(result);
    }

    @Override
    public T next() {
        try {
            if (it < models.size()) {
                return models.get(it++);
            }
            return null;
        } catch (ClientException ce) {
            LOGGER.debug("Error executing Cypher: {}, {}", ce.code(), ce.getMessage());
            throw new CypherException(ce.code(), ce.getMessage(), ce);
        }
    }

    @Override
    public void close() {
        // Consume the rest of the result and thus closing underlying resources.
    }

    @Override
    public String[] columns() {
        return new String[0];
    }

    protected abstract ArrayList<T> mappingResultToModel(String mappingResult);
}
