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

package com.github.luke_ed.couchdb.slacker.schema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.luke_ed.couchdb.slacker.CouchDbClient;
import com.github.luke_ed.couchdb.slacker.CouchDbInitializer;
import com.github.luke_ed.couchdb.slacker.DocumentDescriptor;
import com.github.luke_ed.couchdb.slacker.EntityMetadata;
import com.github.luke_ed.couchdb.slacker.SchemaOperation;
import com.github.luke_ed.couchdb.slacker.annotation.EnableCouchDbRepositories;
import com.github.luke_ed.couchdb.slacker.repository.CouchDBSchemaProcessor;
import com.github.luke_ed.couchdb.slacker.structure.DesignDocument;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {SchemaOperationTestConfiguration.class, CouchDbInitializer.class},
    initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("schema-test")
@EntityScan({"com.github.luke_ed.couchdb.slacker.schema"})
@EnableCouchDbRepositories
@SpringBootTest
class SchemaOperationIntegrationTest {

  @Autowired CouchDbClient client;

  @Autowired CouchDBSchemaProcessor processor;

  @Container private static final GenericContainer<?> singleCouchDbContainer;

  static {
    singleCouchDbContainer =
        new GenericContainer<>(DockerImageName.parse("couchdb:latest"))
            .withEnv(
                Map.of(
                    "COUCHDB_USER", "admin",
                    "COUCHDB_PASSWORD",
                        "-pbkdf2-2a4bb055c2a66b28158523b86afcd7f63bc67ef1,WnMIf5ph/d+jVJDA4WjXdw==,10",
                    "COUCHDB_SECRET", "0123456789abcdef0123456789abcdef"))
            .withExposedPorts(5984)
            .waitingFor(Wait.forListeningPort())
            .waitingFor(Wait.forHttp("/_up").forStatusCode(200));

    singleCouchDbContainer.start();
  }

  @DynamicPropertySource
  static void viewStrategyProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "couchdb.client.url",
        () ->
            "http://%s:%d"
                .formatted(
                    singleCouchDbContainer.getHost(), singleCouchDbContainer.getFirstMappedPort()));
  }

  @AfterAll
  void afterAll() {
    if (singleCouchDbContainer.isRunning()) {
      singleCouchDbContainer.stop();
    }
  }

  @Test
  void testDatabaseExists() throws IOException {
    assertTrue(
        client.databaseExists("schema-test"),
        "Database schema-test does not exist, but schema operation did not fail");
    DesignDocument design =
        assertDoesNotThrow(
            () -> client.readDesign("byType", "schema-test"),
            "It looks like schema processing " + "did not create design document byType");
    assertTrue(
        design.getViews().keySet().stream().anyMatch("schema"::equals),
        "It looks like schema processing did not create view 'schema' in "
            + "design document or overriding other views");
    assertTrue(
        design.getViews().keySet().stream().anyMatch("schema2"::equals),
        "It looks like cschema processing did not create view 'schema2' in "
            + "design document or overriding other views");
    DesignDocument all =
        assertDoesNotThrow(
            () -> client.readDesign("all", "schema-test"),
            "It looks like schema processing " + "did not create design document all");
    assertTrue(
        all.getViews().keySet().stream().anyMatch("data"::equals),
        "It looks like schema processing did not create view 'data' in "
            + "design document or overriding other views");
  }

  @Test
  void testRuntimeAdd() throws Exception {
    processor.processSchema(
        Arrays.asList(
            new EntityMetadata(DocumentDescriptor.of(SchemaTestDocument.class, "schema-test-2")),
            new EntityMetadata(
                DocumentDescriptor.of(OtherSchemaTestDocument.class, "schema-test-2"))),
        SchemaOperation.CREATE);

    assertTrue(
        client.databaseExists("schema-test-2"),
        "Database schema-test-2 do not exists, but schema operation did not failed");
    DesignDocument design =
        assertDoesNotThrow(
            () -> client.readDesign("byType", "schema-test-2"),
            "It looks like schema processing " + "did not create design document byType");
    assertTrue(
        design.getViews().keySet().stream().anyMatch("schema"::equals),
        "It looks like schema processing did not create view 'schema' in "
            + "design document or overriding other views");
    assertTrue(
        design.getViews().keySet().stream().anyMatch("schema2"::equals),
        "It looks like schema processing did not create view 'schema2' in "
            + "design document or overriding other views");
    DesignDocument all =
        assertDoesNotThrow(
            () -> client.readDesign("all", "schema-test"),
            "It looks like schema processing " + "did not create design document all");
    assertTrue(
        all.getViews().keySet().stream().anyMatch("data"::equals),
        "It looks like schema processing did not create view 'data' in "
            + "design document or overriding other views");
  }
}
