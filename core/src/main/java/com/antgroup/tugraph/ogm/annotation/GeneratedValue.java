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
package com.antgroup.tugraph.ogm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.antgroup.tugraph.ogm.id.IdStrategy;
import com.antgroup.tugraph.ogm.id.InternalIdStrategy;
import com.antgroup.tugraph.ogm.id.UuidStrategy;

/**
 * Used to generate an ID. Must be used with the @{@link Id} annotation, otherwise it will be ignored.
 * Two strategies are provided {@link UuidStrategy} and {@link InternalIdStrategy}.
 * Custom strategies may be implemented using {@link IdStrategy}
 *
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface GeneratedValue {

    /**
     * (Optional) The primary key generation strategy
     * that the persistence provider must use to
     * generate the annotated entity id.
     */
    Class<? extends IdStrategy> strategy() default InternalIdStrategy.class;
}
