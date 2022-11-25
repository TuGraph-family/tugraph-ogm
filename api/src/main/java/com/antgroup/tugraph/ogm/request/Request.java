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
package com.antgroup.tugraph.ogm.request;

import com.antgroup.tugraph.ogm.response.Response;
import com.antgroup.tugraph.ogm.model.GraphModel;
import com.antgroup.tugraph.ogm.model.GraphRowListModel;
import com.antgroup.tugraph.ogm.model.RestModel;
import com.antgroup.tugraph.ogm.model.RowModel;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public interface Request {

    Response<GraphModel> execute(GraphModelRequest query);

    Response<RowModel> execute(RowModelRequest query);

    Response<RowModel> execute(DefaultRequest query);

    Response<GraphRowListModel> execute(GraphRowListModelRequest query);

    Response<RestModel> execute(RestModelRequest query);
}
