package com.github.luke_ed.couchdb.slacker.test_utils

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpResponse

class SpyClientHttpRequest(
  private val delegate: ClientHttpRequest,
  private val stubbedResponse: ClientHttpResponse? = null,
  private val executeThrows: Throwable? = null
) : ClientHttpRequest by delegate {

  private val bodyCapture = ByteArrayOutputStream()

  override fun execute(): ClientHttpResponse {
    executeThrows?.let { throw it }
    return stubbedResponse!!
  }

  override fun getBody(): OutputStream = bodyCapture

  fun getCapturedBody(): String = bodyCapture.toString("UTF-8")
}
