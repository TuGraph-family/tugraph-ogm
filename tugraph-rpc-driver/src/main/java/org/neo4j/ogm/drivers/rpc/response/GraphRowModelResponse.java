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
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import org.neo4j.ogm.model.GraphRowListModel;
import org.neo4j.ogm.response.model.DefaultGraphModel;
import org.neo4j.ogm.response.model.DefaultGraphRowListModel;
import org.neo4j.ogm.response.model.DefaultGraphRowModel;
import org.neo4j.ogm.response.model.NodeModel;
import org.neo4j.ogm.response.model.RelationshipModel;

/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class GraphRowModelResponse extends RpcResponse<GraphRowListModel> {

    public GraphRowModelResponse(String result) {
        super(result);
    }

    @Override
    protected ArrayList<GraphRowListModel> mappingResultToModel(String mappingResult) {
        JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.getMask();
        // construct a list<GraphRowListModel>
        ArrayList<GraphRowListModel> models = new ArrayList<>();
        if (mappingResult.equals("null")) {
            return models;
        }

        // mapping from string to json
        JSONArray arrayResults = null;
        if (mappingResult.charAt(0) == '[') {
            arrayResults = JSONArray.parseArray(mappingResult);
        } else {
            mappingResult = "[" + mappingResult + "]";
            arrayResults = JSON.parseArray(mappingResult);
        }

        // mapping jsonObject to model
        // create list<GraphRowModel>
        DefaultGraphRowListModel model = new DefaultGraphRowListModel();
        for (int i = 0; i < arrayResults.size(); i++) {
            JSONObject objectResult = arrayResults.getJSONObject(i);
            // Create GraphRowModel
            DefaultGraphRowModel rowGraph;
            DefaultGraphModel graph = new DefaultGraphModel();
            ArrayList<Object> rows = new ArrayList<Object>();
            for (String keys : objectResult.keySet()) {
                Object values = objectResult.get(keys);
                if (values instanceof JSONObject) {
                    // making Graph Model
                    JSONObject value = (JSONObject) values;
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
                } else {
                    // making row model
                    rows.add(values);
                }
            }
            rowGraph = new DefaultGraphRowModel(graph, rows.toArray());
            model.add(rowGraph);
        }
        models.add(model);
        return models;
    }
}
