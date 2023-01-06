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
package com.antgroup.tugraph.ogm.drivers.rpc.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.alibaba.fastjson.parser.Feature;

import com.antgroup.tugraph.ogm.response.model.DefaultGraphModel;
import com.antgroup.tugraph.ogm.response.model.NodeModel;
import com.antgroup.tugraph.ogm.model.GraphModel;
import com.antgroup.tugraph.ogm.response.model.RelationshipModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class GraphModelResponse extends RpcResponse<GraphModel> {

    public GraphModelResponse(String result) {
        super(result);
    }

    @Override
    public ArrayList<GraphModel> mappingResultToModel(String mappingResult) {
        JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.getMask();
        JSONArray arrayResults = null;
        // construct a list<GraphModel>
        ArrayList<GraphModel> models = new ArrayList<>();
        if (mappingResult.equals("null")) {
            return models;
        }
        // mapping from string to json
        if (mappingResult.charAt(0) == '[') {
            arrayResults = JSONArray.parseArray(mappingResult);
        } else {
            mappingResult = "[" + mappingResult + "]";
            arrayResults = JSON.parseArray(mappingResult);
        }

        for (int i = 0; i < arrayResults.size(); i++) {
            JSONObject objectResult = arrayResults.getJSONObject(i);
            DefaultGraphModel graph = new DefaultGraphModel();
            for (String keys : objectResult.keySet()) {
                JSONObject value = objectResult.getJSONObject(keys);
                if (value.containsKey("start")) {
                    RelationshipModel relationship = new RelationshipModel();
                    for (String key : value.keySet()) {
                        switch (key) {
                            case "identity":
                                relationship.setId((long) value.getInteger(key));
                                break;
                            case "start":
                                relationship.setStartNode((long) value.getInteger(key));
                                break;
                            case "end":
                                relationship.setEndNode((long) value.getInteger(key));
                                break;
                            case "label":
                                relationship.setType(value.getString(key));
                                break;
                            case "properties":
                                JSONObject properties = value.getJSONObject(key);
                                Map<String, Object> mapProperties = new HashMap<>();
                                for (String propertyKey : properties.keySet()) {
                                    mapProperties.put(propertyKey, properties.get(propertyKey));
                                }
                                relationship.setProperties(mapProperties);
                                break;
                            default:
                                break;
                        }
                    }
                    graph.addRelationship(relationship);
                } else {
                    NodeModel node = new NodeModel((long) value.getInteger("identity"));
                    for (String key : value.keySet()) {
                        switch (key) {
                            case "label":
                                String label = value.getString(key);
                                String[] labels = { label };
                                node.setLabels(labels);
                                break;
                            case "properties":
                                JSONObject properties = value.getJSONObject(key);
                                Map<String, Object> mapProperties = new HashMap<>();
                                for (String propertyKey : properties.keySet()) {
                                    mapProperties.put(propertyKey, properties.get(propertyKey));
                                }
                                node.setProperties(mapProperties);
                                break;
                            default:
                                break;
                        }
                    }
                    graph.addNode(node);
                }
            }
            models.add(graph);
        }
        return models;
    }
}
