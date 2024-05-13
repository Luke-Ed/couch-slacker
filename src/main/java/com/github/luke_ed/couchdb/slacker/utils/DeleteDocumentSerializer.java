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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @param <EntityT> Type of entity which should be marked as deleted in a final request
 * @author Majlanky
 */
public class DeleteDocumentSerializer<EntityT> extends WrappingSerializer<EntityT> {

    /**
     * @param clazz of deleted entity. Must not be {@literal null}
     */
    public DeleteDocumentSerializer(@NotNull Class<EntityT> clazz) {
        super(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void serializedAdded(EntityT value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObjectField("_deleted", true);
    }
}
