/*
 * Modifications Copyright 2022 "Ant Group"
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
package com.antgroup.tugraph.ogm.drivers.rpc.request;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.antgroup.tugraph.ogm.drivers.rpc.response.GraphModelResponse;
import com.antgroup.tugraph.ogm.drivers.rpc.response.GraphRowModelResponse;
import com.antgroup.tugraph.ogm.drivers.rpc.response.RestModelResponse;
import com.antgroup.tugraph.ogm.drivers.rpc.response.RowModelResponse;
import com.antgroup.tugraph.TuGraphRpcClient;

import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.exceptions.TransientException;
import com.antgroup.tugraph.ogm.driver.ParameterConversion;
import com.antgroup.tugraph.ogm.exception.CypherException;
import com.antgroup.tugraph.ogm.model.GraphModel;
import com.antgroup.tugraph.ogm.model.GraphRowListModel;
import com.antgroup.tugraph.ogm.model.RestModel;
import com.antgroup.tugraph.ogm.model.RowModel;
import com.antgroup.tugraph.ogm.request.DefaultRequest;
import com.antgroup.tugraph.ogm.request.GraphModelRequest;
import com.antgroup.tugraph.ogm.request.GraphRowListModelRequest;
import com.antgroup.tugraph.ogm.request.Request;
import com.antgroup.tugraph.ogm.request.RestModelRequest;
import com.antgroup.tugraph.ogm.request.RowModelRequest;
import com.antgroup.tugraph.ogm.request.Statement;
import com.antgroup.tugraph.ogm.response.EmptyResponse;
import com.antgroup.tugraph.ogm.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class RpcRequest implements Request {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcRequest.class);

    private final TuGraphRpcClient rpcClient;

    private final ParameterConversion parameterConversion;

    private final Function<String, String> cypherModification;

    public RpcRequest(TuGraphRpcClient rpcClient, ParameterConversion parameterConversion,
        Function<String, String> cypherModification) {
        this.rpcClient = rpcClient;
        this.parameterConversion = parameterConversion;
        this.cypherModification = cypherModification;
    }

    @Override
    public Response<GraphModel> execute(GraphModelRequest request) {
        if (request.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new GraphModelResponse(executeRequest(request));
    }

    @Override
    public Response<RowModel> execute(RowModelRequest request) {
        if (request.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new RowModelResponse(executeRequest(request));
    }

    @Override
    public Response<RowModel> execute(DefaultRequest query) {
        final List<RowModel> rowModels = new ArrayList<>();
        String[] columns = null;
        int i = 0;
        for (Statement statement : query.getStatements()) {
            columns = new String[query.getStatements().size()];
            String result = executeRequest(statement);
            try (RowModelResponse rowModelResponse = new RowModelResponse(result)) {
                if (columns[i] == null) {
                    columns[i++] = result;
                }
                RowModel model;
                while ((model = rowModelResponse.next()) != null) {
                    rowModels.add(model);
                }
            } catch (ClientException e) {
                throw new CypherException(e.code(), e.getMessage(), e);
            }
        }
        return new MultiStatementBasedResponse(columns, rowModels);
    }

    private static class MultiStatementBasedResponse implements Response<RowModel> {
        // This implementation is not good, but it preserved the current behaviour while fixing another bug.
        // While the statements executed in org.neo4j.ogm.drivers.bolt.request.BoltRequest.execute(org.neo4j.ogm.request.DefaultRequest)
        // might return different columns, only the ones of the first result are used. :(
        private final String[] columns;
        private final List<RowModel> rowModels;

        private int currentRow = 0;

        MultiStatementBasedResponse(String[] columns, List<RowModel> rowModels) {
            this.columns = columns;
            this.rowModels = rowModels;
        }

        @Override
        public RowModel next() {
            if (currentRow < rowModels.size()) {
                return rowModels.get(currentRow++);
            }
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public String[] columns() {
            return this.columns;
        }
    }

    @Override
    public Response<GraphRowListModel> execute(GraphRowListModelRequest request) {
        if (request.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new GraphRowModelResponse(executeRequest(request));
    }

    @Override
    public Response<RestModel> execute(RestModelRequest request) {
        if (request.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new RestModelResponse(executeRequest(request));
    }

    public String getLabel(String cypher) {
        Pattern labelPattern1 = Pattern.compile(":`(\\w+)`(\\s*)((\\)|\\])|\\{)");
        Pattern labelPattern2 = Pattern.compile(":(\\w+)(\\s*)((\\)|\\])|\\{)");
        Matcher labelMatch1 = labelPattern1.matcher(cypher);
        Matcher labelMatch2 = labelPattern2.matcher(cypher);
        String label = "";
        if (labelMatch1.find()) {
            label = labelMatch1.group(1);
        } else if (labelMatch2.find()) {
            label = labelMatch2.group(1);
        } else {
            throw new IllegalArgumentException("CREATE without label");
        }
        return label;
    }

    public String getProperties(LinkedHashMap mapProps) {
        String props = "{";
        Set<String> keySet = mapProps.keySet();
        for (String key : keySet) {
            if (mapProps.get(key) == null) {
                continue;
            }
            if (mapProps.get(key) instanceof String) {
                props += key + ":\"" + mapProps.get(key) + "\",";
            } else {
                props += key + ":" + mapProps.get(key).toString() + ",";
            }
        }
        props = props.substring(0, props.length() - 1);
        if (!props.equals("")) {
            props += "}";
        }
        return props;
    }

    private String mergeRequest(String cypher, Map<String, Object> parameterMap) {
        // merge paramters into cypher
        if (cypher.contains("CREATE") || cypher.contains("MERGE")) {
            if (parameterMap.size() == 0) {
                return cypher;
            }
            String label = getLabel(cypher);
            String createCypher = "";
            if (!cypher.contains("-[")) {
                // CREATE NODE
                String type = (String) parameterMap.get("type");
                ArrayList<Object> rowsValue = (ArrayList<Object>) parameterMap.get("rows");
                Long nodeRef = Long.valueOf(-1);
                String nodeIdStr = "";
                String refStr = "";
                for (int i = 0; i < rowsValue.size(); i++) {
                    LinkedHashMap map = (LinkedHashMap) rowsValue.get(i);
                    String props = getProperties((LinkedHashMap) map.get("props"));
                    nodeRef = (Long) map.get("nodeRef");
                    createCypher += "CREATE (n" + i + ":" + label + props + ")\n";
                    nodeIdStr += "id(n" + i + ") AS id" + i + ",";
                    refStr += nodeRef + " AS ref" + i + ",";
                }
                createCypher += "RETURN " + refStr + nodeIdStr + "\"" + type + "\" AS type";
            } else {
                // CREATE RELATIONSHIP
                ArrayList<Object> rowsValue = (ArrayList<Object>) parameterMap.get("rows");
                String matchStr = "MATCH ";
                String whereStr = " WHERE ";
                String mergeStr = "";
                for (int i = 0; i < rowsValue.size(); i++) {
                    LinkedHashMap map = (LinkedHashMap) rowsValue.get(i);
                    Long startId = (Long) map.get("startNodeId");
                    Long endId = (Long) map.get("endNodeId");
                    Long relRef = (Long) map.get("relRef");
                    if (startId < 0 || endId < 0) {
                        throw new IndexOutOfBoundsException("Get wrong id");
                    }
                    String props = getProperties((LinkedHashMap) map.get("props"));
                    matchStr += "(startNode" + i + "),(endNode" + i + "),";
                    whereStr += "id(startNode" + i + ") = " + Long.toString(startId) +
                        " AND id(endNode" + i + ") = " + Long.toString(endId) + " AND ";
                    mergeStr += " MERGE (startNode" + i + ")-[rel" + i + ":" + label + props + "]->(endNode" + i + ") \n ";
                }
                if (matchStr.charAt(matchStr.length() - 1) == ',') {
                    matchStr = matchStr.substring(0, matchStr.length() - 1);
                }
                if (whereStr.substring(whereStr.length() - 4, whereStr.length() - 1).equals("AND")) {
                    whereStr = whereStr.substring(0, whereStr.length() - 4);
                }
                createCypher = matchStr + whereStr + mergeStr;
            }
            return createCypher;
        } else if (cypher.contains("DELETE")) {
            // Modify the statement that specifies label
            String deleteCypher = cypher.replace('`', ' ')
                .replace("OPTIONAL MATCH", "WITH n OPTIONAL MATCH");
            if (parameterMap.size() == 0) {
                return deleteCypher;
            }
            Long id = (Long) parameterMap.get("id");
            if (id < 0) {
                throw new IndexOutOfBoundsException("Get wrong id");
            }
            // Modifies the statement with the specified ID
            deleteCypher = deleteCypher.replace("ID(n) = $id", "id(n) = " + String.valueOf(id));
            return deleteCypher;
        } else if (cypher.contains("SET")) {
            if (parameterMap.size() == 0) {
                return cypher;
            }
            String type = (String) parameterMap.get("type");
            String updateStr = "";
            ArrayList<Object> rowsValue = (ArrayList<Object>) parameterMap.get("rows");
            for (int i = 0; i < rowsValue.size(); i++) {
                LinkedHashMap map = (LinkedHashMap) rowsValue.get(i);
                LinkedHashMap mapProps = (LinkedHashMap) map.get("props");
                String props = "";
                Set<String> keySet = mapProps.keySet();
                for (String key : keySet) {
                    if (mapProps.get(key) == null) {
                        continue;
                    }
                    if (mapProps.get(key) instanceof String) {
                        props += " SET n." + key + " = \"" + mapProps.get(key) + "\"";
                    } else {
                        props += " SET n." + key + " = " + mapProps.get(key);
                    }
                }
                if (!cypher.contains("[r]")) {
                    // UPDATE NODE
                    Long nodeId = (Long) map.get("nodeId");
                    updateStr += "MATCH (n) WHERE id(n) = " + nodeId + props + " RETURN " + nodeId + " AS ref,"
                        + "id(n) AS id, \"" + type + "\" AS type\n";
                } else {
                    // UPDATE RELATIONSHIP
                    Long relId = (Long) map.get("relId");
                    updateStr += "MATCH ()-[n]->() WHERE id(n) = " + relId + props + " RETURN " + relId + " AS ref,"
                        + "id(n) AS id, \"" + type + "\" AS type\n";
                }
            }
            return updateStr;
        } else {
            // MATCH
            String matchStr = cypher;
            matchStr = matchStr.replace("`", "");
            matchStr = matchStr.replace("ID(", "id(");
            for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
                if (entry.getValue() instanceof String) {
                    matchStr = matchStr.replaceAll("\\$" + entry.getKey() + "|\\{\\s*" + entry.getKey() + "\\s*\\}",
                        "\"" + entry.getValue().toString() + "\"");
                } else {
                    matchStr = matchStr.replace("$" + entry.getKey(), entry.getValue().toString());
                }
            }
            // when query returns "n,[ [ (n)-[r_h1: HAS_ALBUM ]->(a1: Album ) | ..."
            if (matchStr.contains("RETURN n,[")) {
                if (matchStr.substring(matchStr.length() - 7, matchStr.length()).equals(", id(n)")) {
                    matchStr = matchStr.substring(0, matchStr.indexOf("RETURN n,[") + 8) + ", id(n)";
                } else {
                    matchStr = matchStr.substring(0, matchStr.indexOf("RETURN n,[") + 8);
                }
            }
            return matchStr;
        }
    }

    private String executeRequest(Statement request) {
        try {
            Map<String, Object> parameterMap = this.parameterConversion.convertParameters(request.getParameters());
            String cypher = cypherModification.apply(request.getStatement());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request: {} with params {}", cypher, parameterMap);
            }
            String result = rpcClient.callCypher(mergeRequest(cypher, parameterMap), "default", 10);
            return result;
        } catch (ClientException | DatabaseException | TransientException ce) {
            throw new CypherException(ce.code(), ce.getMessage(), ce);
        }
    }
}
