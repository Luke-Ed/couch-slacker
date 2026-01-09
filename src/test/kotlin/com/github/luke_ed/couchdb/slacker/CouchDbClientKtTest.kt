package com.github.luke_ed.couchdb.slacker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.luke_ed.couchdb.slacker.integration.TestDocument
import java.io.InputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

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

//  @Disabled("Not fully implemented, need to figure out the mocking")
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
}
