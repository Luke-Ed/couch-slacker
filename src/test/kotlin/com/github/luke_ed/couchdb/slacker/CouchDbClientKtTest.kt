package com.github.luke_ed.couchdb.slacker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.luke_ed.couchdb.slacker.integration.TestDocument
import okhttp3.Address
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CouchDbClientKtTest {
  @Mock private val httpClient: OkHttpClient = mock()
  @Mock private val address: Address = mock()
  @Mock private val dbContext: CouchDbContext = mock()

  private val baseUrl: HttpUrl = "http://localhost:5984".toHttpUrl()
  private lateinit var couchDbClientKt: CouchDbClientKt

  @BeforeEach
  fun beforeEach() {
    couchDbClientKt =
      CouchDbClientKt(
        httpClient,
        address,
        listOf(),
        8,
        3,
        false,
        10000,
        QueryStrategy.MANGO,
        ObjectMapper().registerModule(KotlinModule.Builder().build()),
        dbContext,
        baseUrl
      )
  }

  @Test
  fun testGetDataBaseName() {
    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    val databaseName: String = couchDbClientKt.getDataBaseName(TestDocument::class.java)
    assertThat(databaseName, equalTo("test"))
  }

  @Disabled("Not fully implemented, need to figure out the mocking")
  @Test
  fun testSaveNewItem() {
    val mResponseBody: ResponseBody =
      """{"id": "unique", "rev": "revision", "ok": "true"}"""
        .toResponseBody("application/json".toMediaType())
    val mResponse: Response = mock { on { body } doReturn mResponseBody }

    whenever(dbContext[TestDocument::class.java])
      .doReturn(EntityMetadata(DocumentDescriptor.of(TestDocument::class.java)))

    whenever(httpClient.newCall(any()).execute()).thenReturn(mResponse)

    val savedDocument = couchDbClientKt.save(TestDocument("test"))
    assertNotNull(savedDocument)
  }
}
