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

package com.groocraft.couchdb.slacker.repository;

import com.github.luke_ed.couchdb.slacker.annotation.EnableCouchDbRepositories;
import com.github.luke_ed.couchdb.slacker.repository.CouchDbRepositoriesRegistrar;
import com.github.luke_ed.couchdb.slacker.repository.CouchDbRepositoryConfigurationExtension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouchDbRepositoriesRegistrarTest {

    @Test
    void test() {
        CouchDbRepositoriesRegistrar registrar = new CouchDbRepositoriesRegistrar();
        assertEquals(EnableCouchDbRepositories.class, registrar.getAnnotation(), "Registrar has to process " + EnableCouchDbRepositories.class);
        assertEquals(CouchDbRepositoryConfigurationExtension.class, registrar.getExtension().getClass(),
                "Registrar must return " + CouchDbRepositoryConfigurationExtension.class + " as the extension");
    }

}
