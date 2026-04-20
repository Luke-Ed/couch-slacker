package com.github.luke_ed.couchdb.slacker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.luke_ed.couchdb.slacker.integration.TestDocument
import com.github.luke_ed.couchdb.slacker.repository.CouchDbEntityInformation
import com.github.luke_ed.couchdb.slacker.test_utils.SpyClientHttpRequest
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory

@ExtendWith(MockitoExtension::class)
class CouchDbClientKtTest {
  @Mock private val requestFactory: ClientHttpRequestFactory = mock()
  @Mock private val dbContext: CouchDbContext = mock()

  private val baseUrl: String = "http://localhost:5984"
  private lateinit var couchDbClientKt: CouchDbClientKt

  @BeforeEach
  fun beforeEach() {
    couchDbClientKt =
      CouchDbClientKt(
        requestFactory,
        baseUrl,
        listOf(),
        8,
        3,
        false,
        10000,
        QueryStrategy.MANGO,
        ObjectMapper().registerModule(KotlinModule.Builder().build()),
        dbContext,
      )
  }

  @Test
  fun testGetDataBaseName() {
    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    val databaseName: String = couchDbClientKt.getDataBaseName(TestDocument::class.java)
    assertEquals(databaseName, "test")
  }

  @Test
  fun testGetEntityInformation() {

    whenever(dbContext.get(TestDocument::class.java))
      .thenReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))
    val entityInformation: CouchDbEntityInformation<TestDocument, String> =
      couchDbClientKt.getEntityInformation(TestDocument::class.java)

    assertNotNull(entityInformation)
    assertEquals(
      TestDocument::class.java,
      entityInformation.javaType,
      "Returned entity information must match entity class"
    )
  }

  @Test
  fun testSaveNewItem() {
    val mResponseBody: InputStream =
      """{"id": "unique", "rev": "revision", "ok": "true"}""".byteInputStream()
    val mResponse: ClientHttpResponse = mock { on { body } doReturn mResponseBody }

    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    val mRequest = mock<ClientHttpRequest>()
    val httpRequestMethodCaptor = argumentCaptor<HttpMethod>()
    whenever(requestFactory.createRequest(any(), httpRequestMethodCaptor.capture()))
      .thenReturn(mRequest)

    whenever(mRequest.headers).thenReturn(HttpHeaders())
    whenever(mRequest.body).thenReturn(ByteArrayOutputStream())

    whenever(mRequest.execute()).thenReturn(mResponse)

    val savedDocument = couchDbClientKt.save(TestDocument("test"))

    assertEquals(HttpMethod.PUT, httpRequestMethodCaptor.firstValue, "Save must use HTTP PUT")
    assertNotNull(savedDocument)
  }

  @Test
  fun testSave() {
    val thrownException = IOException("error")
    val testDocument = TestDocument("test")
    val uuidString = UUID.randomUUID().toString()
    testDocument.id = uuidString
    val content = """{"id": "$uuidString", "rev": "revision","ok": "true"}""".byteInputStream()

    val mResponse: ClientHttpResponse = mock { on { body } doReturn content }

    var capturedRequest: ClientHttpRequest? = null

    whenever(requestFactory.createRequest(any(), any()))
      .thenAnswer { invocationOnMock ->
        val uri = invocationOnMock.arguments[0] as URI
        val method = invocationOnMock.arguments[1] as HttpMethod
        val realRequest = SimpleClientHttpRequestFactory().createRequest(uri, method)
        SpyClientHttpRequest(realRequest, mResponse).also { capturedRequest = it }
      }
      .thenAnswer { invocationOnMock ->
        val uri = invocationOnMock.arguments[0] as URI
        val method = invocationOnMock.arguments[1] as HttpMethod
        val realRequest = SimpleClientHttpRequestFactory().createRequest(uri, method)
        SpyClientHttpRequest(realRequest, executeThrows = thrownException).also {
          capturedRequest = it
        }
      }

    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    val saved = couchDbClientKt.save(testDocument)

    assertNotNull(capturedRequest, "Request must be captured to continue testing")

    val request: SpyClientHttpRequest = capturedRequest!! as SpyClientHttpRequest

    assertEquals(HttpMethod.PUT, request.method, "Save must use HTTP PUT")

    assertEquals(
      "$baseUrl/test/$uuidString",
      request.uri.toString(),
      "URI must be baseURI + databaseName + id"
    )
    assertEquals(
      MediaType.APPLICATION_JSON,
      request.headers.contentType,
      "Content-Type must be json"
    )
    assertEquals(uuidString, saved.id, "If document id is set, it must not be changed")
    assertEquals("revision", saved.revision, "Revision must be updated by given revision")
    assertEquals(
      """{"_id":"$uuidString","value":"test","value2":null,"value3":null,"value4":null,"value5":false}""",
      request.getCapturedBody(),
      "Body of save request was not properly created"
    )

    verify(mResponse, times(1)).close()

    assertEquals(
      thrownException,
      assertThrows<IOException> { couchDbClientKt.save(TestDocument("test")) },
      "Client should not modify original exception when thrown"
    )
  }

  @Test
  fun testSaveAll() {
    val thrownException = IOException("error")
    val content =
      """[{"id": "a", "ok": true, "rev": "rev1"},{"id": "b", "ok": true, "rev": "rev1"}]"""
        .byteInputStream()

    val mResponse: ClientHttpResponse = mock { on { body } doReturn content }

    var capturedRequest: ClientHttpRequest? = null

    whenever(requestFactory.createRequest(any(), any()))
      .thenAnswer { invocationOnMock ->
        val uri = invocationOnMock.arguments[0] as URI
        val method = invocationOnMock.arguments[1] as HttpMethod
        val realRequest = SimpleClientHttpRequestFactory().createRequest(uri, method)
        SpyClientHttpRequest(realRequest, mResponse).also { capturedRequest = it }
      }
      .thenAnswer { invocationOnMock ->
        val uri = invocationOnMock.arguments[0] as URI
        val method = invocationOnMock.arguments[1] as HttpMethod
        val realRequest = SimpleClientHttpRequestFactory().createRequest(uri, method)
        SpyClientHttpRequest(realRequest, executeThrows = thrownException).also {
          capturedRequest = it
        }
      }

    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    val documentA = TestDocument("a", null, "a", "a")
    val documentB = TestDocument("b", null, "b", "b")

    val savedDocuments =
      couchDbClientKt.saveAll(listOf(documentA, documentB), TestDocument::class.java)

    assertNotNull(capturedRequest, "Request must be captured to continue testing")

    val request: SpyClientHttpRequest = capturedRequest!! as SpyClientHttpRequest

    assertEquals(HttpMethod.POST, request.method, "saveAll must use HTTP POST")
    assertEquals(
      "$baseUrl/test/_bulk_docs",
      request.uri.toString(),
      "URI must be baseURI + databaseName + _bulk_docs"
    )
    assertEquals(
      MediaType.APPLICATION_JSON,
      request.headers.contentType,
      "Content-Type must be json"
    )
    assertEquals(
      """{"docs":[{"_id":"a","value":"a","value2":"a","value3":null,"value4":null,"value5":false},{"_id":"b","value":"b","value2":"b","value3":null,"value4":null,"value5":false}]}""",
      request.getCapturedBody(),
      "Body of save request was not properly created"
    )
    assertEquals(2, savedDocuments.count())

    verify(mResponse, times(1)).close()

    assertEquals(
      "aaa",
      "${documentA.id}${documentA.value}${documentA.value2}",
      "Document id nor values cannot be changed"
    )
    assertEquals(
      "bbb",
      "${documentB.id}${documentB.value}${documentB.value2}",
      "Document id nor values cannot be changed"
    )

    assertEquals("rev1", documentA.revision, "Revision must be updated by given revision")
    assertEquals("rev1", documentB.revision, "Revision must be updated by given revision")

    assertEquals(
      thrownException,
      assertThrows<IOException> {
        couchDbClientKt.saveAll(
          listOf(TestDocument("a", "b"), TestDocument("b", "b")),
          TestDocument::class.java
        )
      },
      "Client should not modify original exception when thrown"
    )
  }
}
