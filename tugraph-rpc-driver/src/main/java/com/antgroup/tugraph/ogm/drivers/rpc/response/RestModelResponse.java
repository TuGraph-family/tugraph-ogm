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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import com.antgroup.tugraph.ogm.response.model.DefaultRestModel;
import com.antgroup.tugraph.ogm.response.model.QueryStatisticsModel;
import com.antgroup.tugraph.ogm.model.QueryStatistics;
import com.antgroup.tugraph.ogm.model.RestModel;

/**
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
public class RestModelResponse extends RpcResponse<RestModel> {

    private final QueryStatisticsModel statisticsModel;

    public RestModelResponse(String result) {
        super(result);
        this.statisticsModel = new StatisticsModelAdapter().adapt(result);
    }

    @Override
    protected ArrayList<RestModel> mappingResultToModel(String mappingResult) {
        JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.getMask();
        ArrayList<RestModel> models = new ArrayList<>();
        if (mappingResult.equals("null")) {
            return models;
        }
        if (mappingResult.contains("[")) {
            JSONArray arrayResult = JSONArray.parseArray(mappingResult);
            for (int i = 0; i < arrayResult.size(); i++) {
                JSONObject obj = arrayResult.getJSONObject(i);

                Map<String, Object> row = new HashMap<>();
                for (String key : obj.keySet()) {
                    row.put(key, obj.get(key));
                }
                DefaultRestModel model = DefaultRestModel.basedOn(row).orElse(null);
                models.add(model);
            }
        } else {
            JSONObject objectResult = JSONObject.parseObject(mappingResult);
            Map<String, Object> row = new HashMap<>();
            for (String key : objectResult.keySet()) {
                row.put(key, objectResult.get(key));
            }
            DefaultRestModel model = DefaultRestModel.basedOn(row).orElse(null);
            models.add(model);
        }
        return models;
    }

    @Override
    public Optional<QueryStatistics> getStatistics() {
        return Optional.of(statisticsModel);
    }
}
