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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.alibaba.fastjson.parser.Feature;

import org.neo4j.ogm.model.RowModel;
import org.neo4j.ogm.response.model.DefaultRowModel;

/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class RowModelResponse extends RpcResponse<RowModel> {

    public RowModelResponse(String result) {
        super(result);
    }

    @Override
    protected ArrayList<RowModel> mappingResultToModel(String mappingResult) {
        JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.getMask();
        ArrayList<RowModel> models = new ArrayList<>();
        if (mappingResult.equals("null")) {
            return models;
        }
        // Judge result format
        if (mappingResult.charAt(0) == '[') {
            JSONArray arrayResult = JSONArray.parseArray(mappingResult);
            for (int i = 0; i < arrayResult.size(); i++) {
                JSONObject obj = arrayResult.getJSONObject(i);
                String[] variables = new String[obj.size()];
                Object[] values = new Object[obj.size()];
                int j = 0;
                for (String key : obj.keySet()) {
                    variables[j] = key;
                    values[j++] = obj.get(key);
                }
                DefaultRowModel model = new DefaultRowModel(values, variables);
                models.add(model);
            }
        } else {
            JSONObject objectResult = JSONObject.parseObject(mappingResult);
            if (objectResult.containsKey("ref0") &&
                objectResult.containsKey("type")) {
                // Get CREATE result
                Object type = objectResult.get("type");
                int i = 0;
                while (objectResult.containsKey("ref" + i)) {
                    String[] variables = new String[3];
                    Object[] values = new Object[3];
                    variables[0] = "id";
                    values[0] = objectResult.get("id" + i);
                    variables[1] = "ref";
                    values[1] = objectResult.get("ref" + i);
                    variables[2] = "type";
                    values[2] = type;
                    DefaultRowModel model = new DefaultRowModel(values, variables);
                    models.add(model);
                    i++;
                }
            } else {
                String[] variables = new String[objectResult.size()];
                Object[] values = new Object[objectResult.size()];
                int i = 0;
                for (String key : objectResult.keySet()) {
                    variables[i] = key;
                    values[i++] = objectResult.get(key);
                }
                DefaultRowModel model = new DefaultRowModel(values, variables);
                models.add(model);
            }
        }
        return models;
    }
}
