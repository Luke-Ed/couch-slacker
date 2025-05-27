/*
 * Copyright 2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.luke_ed.couchdb.slacker.structure.BulkGetRequest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BulkGetSerializerTest {

  private final String docsJson = """
        {"docs":[{"id":"a"},{"id":"b"}]}
        """.trim();

  @Test
  void test() throws JsonProcessingException {
    ObjectMapper localMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    localMapper.registerModule(module);
    assertEquals(
        docsJson, localMapper.writeValueAsString(new BulkGetRequest(Arrays.asList("a", "b"))));
  }
}
