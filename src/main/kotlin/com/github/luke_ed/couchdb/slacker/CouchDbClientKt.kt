package com.github.luke_ed.couchdb.slacker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.luke_ed.couchdb.slacker.structure.BulkRequest
import com.github.luke_ed.couchdb.slacker.structure.DocumentPutResponse
import com.github.luke_ed.couchdb.slacker.utils.ThrowingFunction
import com.github.luke_ed.couchdb.slacker.utils.ViewedDocumentSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import java.io.IOException
import java.net.URI
import java.util.function.Consumer
import java.util.stream.Collectors
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInitializer
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

class CouchDbClientKt
internal constructor(
  private val requestFactory: ClientHttpRequestFactory,
  private val baseUrl: String,
  idGenerators: Iterable<IdGenerator<Any>>,
  defaultShards: Int,
  defaultReplicas: Int,
  defaultPartitioned: Boolean,
  bulkMaxSize: Int,
  queryStrategy: QueryStrategy,
  objectMapper: ObjectMapper,
  couchDbContext: CouchDbContext
) {
  private val idGenerators: MutableMap<Class<*>, IdGenerator<*>> = HashMap()
  private val defaultShards: Int
  private val defaultReplicas: Int
  private val defaultPartitioned: Boolean
  private val bulkMaxSize: Int
  private val queryStrategy: QueryStrategy
  private val objectMapper: ObjectMapper
  private val couchDbContext: CouchDbContext
  private val knownIndices: Set<String>
  private val knownSortedViews: Set<String>
  private val defaultIdGenerator: IdGenerator<*>
  private val jsonMediaType: MediaType = MediaType.APPLICATION_JSON

  private val logger = KotlinLogging.logger {}

  init {
    idGenerators.forEach(
      Consumer { generator: IdGenerator<*> -> this.idGenerators[generator.entityClass] = generator }
    )
    this.defaultShards = defaultShards
    this.defaultReplicas = defaultReplicas
    this.defaultPartitioned = defaultPartitioned
    this.bulkMaxSize = bulkMaxSize
    this.queryStrategy = queryStrategy
    this.objectMapper = objectMapper
    this.couchDbContext = couchDbContext
    if (!this.objectMapper.registeredModuleIds.contains(KotlinModule::class.qualifiedName)) {
      this.objectMapper.registerModule(KotlinModule.Builder().build())
    }
    this.knownIndices = HashSet()
    this.knownSortedViews = HashSet()
    this.defaultIdGenerator = IdGeneratorUUID()
  }

  fun <T> getEntityMetadata(clazz: Class<T>): EntityMetadata {
    return couchDbContext[clazz]
  }

  private fun <EntityT : Any> generateId(entity: EntityT, clazz: Class<EntityT>): String {
    val idGenerator =
      idGenerators.computeIfAbsent(clazz) { defaultIdGenerator } as? IdGenerator<EntityT>
        ?: throw IllegalStateException(
          "Expected IdGenerator for type ${clazz.simpleName}, but found ${idGenerators[clazz]?.javaClass?.simpleName}"
        )

    return idGenerator.generate(entity)
  }

  fun getDataBaseName(clazz: Class<*>): String {
    return getEntityMetadata(clazz).databaseName
  }

  private fun getHttpUrl(base: URI, vararg pathSegments: String): UriComponents {
    return UriComponentsBuilder.fromUri(base).pathSegment(*pathSegments).build()
  }

  private fun getHttpUrl(base: String, vararg pathSegments: String): UriComponents {
    return UriComponentsBuilder.fromHttpUrl(base).pathSegment(*pathSegments).build()
  }

  private fun getHttpUrl(
    base: URI,
    pathSegments: List<String>,
    parameters: Map<String, String>
  ): UriComponents {
    val builder = UriComponentsBuilder.fromUri(base).pathSegment(*pathSegments.toTypedArray())
    parameters.forEach { (k, v) -> builder.queryParam(k, v) }
    return builder.build()
  }

  private fun resolveMapper(entityMetadata: EntityMetadata, clazz: Class<*>): ObjectMapper {
    if (entityMetadata.isViewed) {
      val localMapper = objectMapper.copy()
      val module = SimpleModule()
      module.addSerializer(
        ViewedDocumentSerializer(clazz, entityMetadata.typeField, entityMetadata.type)
      )
      localMapper.registerModule(module)
      return localMapper
    }
    return objectMapper
  }

  private fun <DataT> put(
    uriComponents: UriComponents,
    json: String,
    responseProcessor: ThrowingFunction<ClientHttpResponse, DataT, IOException>
  ): DataT {
    val request = requestFactory.createRequest(uriComponents.toUri(), HttpMethod.PUT)
    val headers = request.headers
    headers[HttpHeaders.CONTENT_TYPE] =  jsonMediaType.toString()
    request.body.use { body -> body.write(json.toByteArray()) }

    request.execute().use { response ->
      return responseProcessor.apply(response)
    }
  }

  private fun <DataT> post(
    uriComponents: UriComponents,
    json: String,
    responseProcessor: ThrowingFunction<ClientHttpResponse, DataT, IOException>
  ): DataT {
    val request = requestFactory.createRequest(uriComponents.toUri(), HttpMethod.POST)

    request.headers.contentType = jsonMediaType
    request.body.use { body -> body.write(json.toByteArray()) }

    request.execute().use { response ->
      return responseProcessor.apply(response)
    }
  }

  fun <EntityT : Any> save(entity: EntityT): EntityT {
    val entityMetadata = getEntityMetadata(entity::class.java)
    var id = entityMetadata.idReader.read(entity)
    logger.debug {
      "Saving document $entity with id $id and revision ${entityMetadata.revisionReader.read(entity)} to database ${entityMetadata.databaseName}"
    }

    if (id.isNullOrBlank()) {
      id = generateId(entity, entity.javaClass)
      logger.debug { "New Id $id generated for saved document" }
    }
    val localMapper = resolveMapper(entityMetadata, entity::class.java)

    val response: DocumentPutResponse =
      put(
        getHttpUrl(baseUrl, entityMetadata.databaseName, id),
        localMapper.writeValueAsString(entity),
      ) { response ->
        objectMapper.readValue<DocumentPutResponse>(response.body)
      }

    entityMetadata.revisionWriter.write(entity, response.rev)
    entityMetadata.idWriter.write(entity, response.id)
    logger.debug { "Saved document $entity with id ${response.id} and revision ${response.rev}" }
    return entity
  }

  fun <EntityT : Any> saveAll(entities: Iterable<EntityT>, clazz: Class<*>): Iterable<EntityT> {
    val entityMetadata = getEntityMetadata(clazz)

    logger.debug {
      "Bulk save of ${entities.count()} documents to database ${entityMetadata.databaseName}"
    }

    for (entity in entities) {
      var id = entityMetadata.idReader.read(entity)

      if (id.isNullOrBlank()) {
        id = generateId(entity, entity.javaClass)
        entityMetadata.idWriter.write(entity, id)
        logger.debug { "New Id: $id generated for saved document" }
      }
    }

    val localMapper = resolveMapper(entityMetadata, clazz)

    val responses =
      post(
        getHttpUrl(baseUrl, entityMetadata.databaseName, "_bulk_docs"),
        localMapper.writeValueAsString(BulkRequest(entities))
      ) { response ->
        objectMapper.readValue<List<DocumentPutResponse>>(
          response.body.toString(),
          objectMapper.typeFactory.constructCollectionType(
            List::class.java,
            DocumentPutResponse::class.java
          )
        )
      }
    val indexedResponses: Map<String, DocumentPutResponse> =
      responses.stream().collect(Collectors.toMap(DocumentPutResponse::getId) { it })

    for (entity in entities) {
      val response = indexedResponses[entityMetadata.idReader.read(entity)]

      if (response?.ok == "true") {
        entityMetadata.revisionWriter.write(entity, response.rev)
        entityMetadata.idWriter.write(entity, response.id)
      } else {
        logger.warn {
          "Document $entity with id: ${response?.id} and rev: ${response?.rev} failed to save with error ${response?.error} and reason ${response?.reason}"
        }
      }
    }

    return entities
  }
}
