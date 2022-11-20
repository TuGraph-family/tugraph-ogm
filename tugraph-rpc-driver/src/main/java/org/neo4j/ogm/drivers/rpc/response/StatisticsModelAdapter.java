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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.neo4j.ogm.config.ObjectMapperFactory;
import org.neo4j.ogm.response.model.QueryStatisticsModel;
import org.neo4j.ogm.result.adapter.ResultAdapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticsModelAdapter implements ResultAdapter<String, QueryStatisticsModel> {
    protected static final ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    public Integer[] mappingStats(Integer[] stats, String result) {
        for (int i = 0; i < stats.length; i++) {
            stats[i] = 0;
        }
        Pattern deleteVPattern = Pattern.compile("deleted\\s([0-9]*)\\svertices");
        Pattern deleteEPattern = Pattern.compile("deleted\\s([0-9]*)\\sedges");
        Pattern createVPattern = Pattern.compile("created\\s([0-9]*)\\svertices");
        Pattern createEPattern = Pattern.compile("created\\s([0-9]*)\\sedges");
        Pattern setPPattern = Pattern.compile("set\\s([0-9]*)\\sproperties");
        Matcher deleteVMatch = deleteVPattern.matcher(result);
        Matcher deleteEMatch = deleteEPattern.matcher(result);
        Matcher createVMatch = createVPattern.matcher(result);
        Matcher createEMatch = createEPattern.matcher(result);
        Matcher setPMatch = setPPattern.matcher(result);
        if (deleteVMatch.find()) {
            stats[1] = Integer.valueOf(deleteVMatch.group(1));
        }
        if (deleteEMatch.find()) {
            stats[4] = Integer.valueOf(deleteEMatch.group(1));
        }
        if (createVMatch.find()) {
            stats[0] = Integer.valueOf(createVMatch.group(1));
        }
        if (createEMatch.find()) {
            stats[3] = Integer.valueOf(createEMatch.group(1));
        }
        if (setPMatch.find()) {
            stats[2] = Integer.valueOf(setPMatch.group(1));
        }
        return stats;
    }

    @Override
    public QueryStatisticsModel adapt(String result) {
        QueryStatisticsModel queryStatisticsModel = new QueryStatisticsModel();
        // Parse the result from string
        Integer[] stats = new Integer[11];
        mappingStats(stats, result);
        queryStatisticsModel.setContains_updates(false);
        queryStatisticsModel.setNodes_created(stats[0]);
        queryStatisticsModel.setNodes_deleted(stats[1]);
        queryStatisticsModel.setProperties_set(stats[2]);
        queryStatisticsModel.setRelationships_created(stats[3]);
        queryStatisticsModel.setRelationship_deleted(stats[4]);
        queryStatisticsModel.setLabels_added(stats[5]);
        queryStatisticsModel.setLabels_removed(stats[6]);
        queryStatisticsModel.setIndexes_added(stats[7]);
        queryStatisticsModel.setIndexes_removed(stats[8]);
        queryStatisticsModel.setConstraints_added(stats[9]);
        queryStatisticsModel.setConstraints_removed(stats[10]);
        return queryStatisticsModel;
    }
}


