/*
 * Copyright 2020 the original author or authors.
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

package com.groocraft.couchdb.slacker.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.groocraft.couchdb.slacker.data.Reader;

import java.io.IOException;

public class BulkGetIdSerializer<EntityT> extends JsonSerializer<EntityT> {

    private final Class<EntityT> clazz;
    private final Reader<String> idReader;

    public BulkGetIdSerializer(Class<EntityT> clazz, Reader<String> idReader) {
        this.clazz = clazz;
        this.idReader = idReader;
    }

    @Override
    public Class<EntityT> handledType() {
        return clazz;
    }

    @Override
    public void serialize(EntityT value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("id", idReader.read(value));
        gen.writeEndObject();
    }
}
