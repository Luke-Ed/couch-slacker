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

import com.github.luke_ed.couchdb.slacker.utils.ThrowingBiConsumer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowingBiConsumerTest {

    @Test
    void test() {
        assertThrows(Exception.class, () -> process(this::processString), "Exception produces in the function must be passed outside");
    }

    private void processString(String s, String s2) throws IOException {
        throw new IOException(s + s2);
    }

    private void process(ThrowingBiConsumer<String, String, Exception> f) throws Exception {
        f.accept("test", "test2");
    }

}
