package com.github.luke_ed.couchdb.slacker; /*
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

import com.github.luke_ed.couchdb.slacker.annotation.EnableCouchDbRepositories;
import java.util.Map;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {
      SpringTestConfiguration.class,
      TestDocumentRepository.class,
      CouchDbInitializer.class
    },
    initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("test-mango-strategy")
@EnableCouchDbRepositories
@DirtiesContext
public class TestDocumentRepositoryMangoStrategyIntegrationTest extends TestDocumentRepositoryBase {

  static {
    singleCouchDbContainer =
        new GenericContainer<>(DockerImageName.parse("couchdb:latest"))
            .withEnv(
                Map.of(
                    "COUCHDB_USER", "admin",
                    "COUCHDB_PASSWORD",
                        "-pbkdf2-2a4bb055c2a66b28158523b86afcd7f63bc67ef1,WnMIf5ph/d+jVJDA4WjXdw==,10",
                    "COUCHDB_SECRET", "0123456789abcdef0123456789abcdef"))
            .withExposedPorts(5984, 5986)
            .waitingFor(Wait.forListeningPort().forPorts(5984, 5986))
            .waitingFor(Wait.forHttp("/_up").forPort(5984).forStatusCode(200));

    singleCouchDbContainer.start();
  }

  @DynamicPropertySource
  static void mangoProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "couchdb.client.url",
        () ->
            "http://%s:%d"
                .formatted(
                    singleCouchDbContainer.getHost(), singleCouchDbContainer.getFirstMappedPort()));
  }

  @Autowired
  public TestDocumentRepositoryMangoStrategyIntegrationTest(
      CouchDbClient couchDbClient, TestDocumentRepository testDocumentRepository) {
    super(couchDbClient, testDocumentRepository);
  }
}
