/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.luke_ed.couchdb.slacker.structure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.luke_ed.couchdb.slacker.utils.IndexSerializer;
import org.springframework.data.domain.Sort;

public class IndexCreateRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSerialize(using = IndexSerializer.class)
    @JsonProperty("index")
    private final Iterable<Sort.Order> fields;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("type")
    private final String type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("name")
    private final String name;

    public IndexCreateRequest(String name, Iterable<Sort.Order> fields) {
        this.name = name;
        this.fields = fields;
        this.type = "json";
    }
}
