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

package com.github.luke_ed.couchdb.slacker.initialization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.luke_ed.couchdb.slacker.CouchDbClient;
import com.github.luke_ed.couchdb.slacker.CouchDbInitializer;
import com.github.luke_ed.couchdb.slacker.annotation.EnableCouchDbRepositories;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {CusterInitializationConfiguration.class, CouchDbInitializer.class},
    initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("initialization")
@EnableCouchDbRepositories
class ClusterInitializationIntegrationTest {

  private static final Map<@NotNull String, @NotNull String> sharedEnvironment =
      Map.of(
          "COUCHDB_USER", "admin",
          "COUCHDB_PASSWORD",
              "-pbkdf2-2a4bb055c2a66b28158523b86afcd7f63bc67ef1,WnMIf5ph/d+jVJDA4WjXdw==,10",
          "COUCHDB_SECRET", "0123456789abcdef0123456789abcdef");

  static Network clusterNetwork = Network.newNetwork();

  @Container
  static GenericContainer<?> couchDbContainer0 =
      new GenericContainer<>(DockerImageName.parse("couchdb:latest"))
          .withEnv(sharedEnvironment)
          .withEnv("NODE_NAME", "couchdb-0")
          .withNetwork(clusterNetwork)
          .withNetworkAliases("couchdb-0")
          .withExposedPorts(5984)
          .waitingFor(Wait.forListeningPort())
          .waitingFor(Wait.forHttp("/_up").forStatusCode(200));

  @Container
  static GenericContainer<?> couchDbContainer1 =
      new GenericContainer<>(DockerImageName.parse("couchdb:latest"))
          .withEnv(sharedEnvironment)
          .withEnv("NODE_NAME", "couchdb-1")
          .withNetwork(clusterNetwork)
          .withNetworkAliases("couchdb-1")
          .withExposedPorts(5984)
          .waitingFor(Wait.forListeningPort())
          .waitingFor(Wait.forHttp("/_up").forStatusCode(200));

  @Container
  static GenericContainer<?> couchDbContainer2 =
      new GenericContainer<>(DockerImageName.parse("couchdb:latest"))
          .withEnv(sharedEnvironment)
          .withEnv("NODE_NAME", "couchdb-2")
          .withNetwork(clusterNetwork)
          .withNetworkAliases("couchdb-2")
          .withExposedPorts(5984)
          .waitingFor(Wait.forListeningPort())
          .waitingFor(Wait.forHttp("/_up").forStatusCode(200));

  static {
    couchDbContainer0.start();
    couchDbContainer1.start();
    couchDbContainer2.start();
  }

  @AfterAll
  static void afterAll() {
    if (couchDbContainer0 != null && couchDbContainer0.isRunning()) {
      couchDbContainer0.stop();
    }
    if (couchDbContainer1 != null && couchDbContainer1.isRunning()) {
      couchDbContainer1.stop();
    }
    if (couchDbContainer2 != null && couchDbContainer2.isRunning()) {
      couchDbContainer2.stop();
    }
  }

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "couchdb.client.url",
        () ->
            "http://%s:%d"
                .formatted(couchDbContainer0.getHost(), couchDbContainer0.getFirstMappedPort()));

    registry.add(
        "couchdb.client.cluster.nodes",
        () -> List.of("couchdb-0:5984", "couchdb-1:5984", "couchdb-2:5984"));
  }

  @Autowired CouchDbClient client;

  @Test
  void initializedTest() {
    assertDoesNotThrow(() -> client.verifyCluster());
  }
}
