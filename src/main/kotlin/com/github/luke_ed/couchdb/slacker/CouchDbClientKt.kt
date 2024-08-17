package com.github.luke_ed.couchdb.slacker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.luke_ed.couchdb.slacker.utils.ThrowingFunction
import com.github.luke_ed.couchdb.slacker.utils.ViewedDocumentSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URI
import java.net.http.HttpResponse
import java.util.function.Consumer

class CouchDbClientKt internal constructor(
    okHttpClient: OkHttpClient,
    address: Address,
    idGenerators: Iterable<IdGenerator<Any>>,
    defaultShards: Int,
    defaultReplicas: Int,
    defaultPartitioned: Boolean,
    bulkMaxSize: Int,
    queryStrategy: QueryStrategy,
    objectMapper: ObjectMapper,
    couchDbContext: CouchDbContext,
    httpUrl: HttpUrl
) {
    private val okHttpClient: OkHttpClient
    private val address: Address
    private val idGenerators: MutableMap<Class<*>, IdGenerator<*>>
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
    private val jsonMediaType: MediaType = "application/json".toMediaType()
    private val baseHttpUrl: HttpUrl

    private val logger = KotlinLogging.logger {}

    init {
        this.okHttpClient = okHttpClient
        this.address = address
        this.idGenerators = HashMap()
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
        this.baseHttpUrl = httpUrl
    }


    fun <T> getEntityMetadata(clazz: Class<T>): EntityMetadata {
        return couchDbContext[clazz]
    }

    private fun <EntityT : Any> generateId(entity: EntityT, clazz: Class<EntityT>): String {
        val idGenerator = idGenerators.computeIfAbsent(clazz) { defaultIdGenerator }
                as? IdGenerator<EntityT>
            ?: throw IllegalStateException(
                "Expected IdGenerator for type ${clazz.simpleName}, but found ${idGenerators[clazz]?.javaClass?.simpleName}"
            )

        return idGenerator.generate(entity)
    }

    fun getDataBaseName(clazz: Class<*>): String {
        return getEntityMetadata(clazz).databaseName
    }

    private fun getHttpUrl(base: URI, vararg pathSegments: String): HttpUrl {
        val baseUrl = base.toHttpUrlOrNull()
        requireNotNull(baseUrl) { "URI: $base, had a protocol other than http, or https" }
        val builder = baseUrl.newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private fun getHttpUrl(base: HttpUrl, vararg pathSegments: String): HttpUrl {
        val builder = base.newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private fun getHttpUrl(base: URI, pathSegments: List<String>, parameters: Map<String, String>): HttpUrl {
        val baseUrl = base.toHttpUrlOrNull()
        requireNotNull(baseUrl) { "URI: $base, had a protocol other than http, or https" }
        val builder = baseUrl.newBuilder()
        pathSegments.forEach { builder.addPathSegment(it) }
        parameters.forEach { builder.addQueryParameter(it.key, it.value) }
        return builder.build()
    }

    private fun resolveMapper(entityMetadata: EntityMetadata, clazz: Class<*>): ObjectMapper {
        if (entityMetadata.isViewed) {
            val localMapper = objectMapper.copy();
            val module = SimpleModule()
            module.addSerializer(ViewedDocumentSerializer(clazz, entityMetadata.typeField, entityMetadata.type))
            localMapper.registerModule(module)
            return localMapper
        }
        return objectMapper
    }

    private fun <DataT> put(httpUrl: HttpUrl, json: String, responseProcessor: ThrowingFunction<Response, DataT, IOException>): DataT {
        val request = Request.Builder().url(httpUrl).post(json.toRequestBody(jsonMediaType)).build()
        val response = okHttpClient.newCall(request).execute()
        return responseProcessor.apply(response)
    }

    private fun <EntityT : Any> save(entity: EntityT): EntityT {
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

        val response = put(getHttpUrl(baseHttpUrl, entityMetadata.databaseName, id), localMapper.writeValueAsString(entity))
    }
}