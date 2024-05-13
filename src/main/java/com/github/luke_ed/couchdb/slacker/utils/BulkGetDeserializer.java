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

package com.github.luke_ed.couchdb.slacker.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Majlanky
 */
public class BulkGetDeserializer<EntityT> extends JsonDeserializer<List<EntityT>> {

    private final Class<EntityT> clazz;
    private final ObjectMapper mapper;

    /**
     * @param clazz  of entities in bulk get. Must not be {@literal null}
     * @param mapper must not be {@literal null}
     */
    public BulkGetDeserializer(@NotNull Class<EntityT> clazz, @NotNull ObjectMapper mapper) {
        Assert.notNull(clazz, "Clazz must not be null");
        Assert.notNull(mapper, "Object mapper must not be null");
        this.clazz = clazz;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EntityT> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        List<EntityT> data = new LinkedList<>();
        JsonNode root = p.getCodec().readTree(p);
        for (int i = 0; i < root.size(); i++) {
            JsonNode object = root.get(i).get("docs").get(0).get("ok");
            if (object != null) {
                data.add(mapper.readValue(object.toString(), clazz));
            }
        }
        return data;
    }
}
