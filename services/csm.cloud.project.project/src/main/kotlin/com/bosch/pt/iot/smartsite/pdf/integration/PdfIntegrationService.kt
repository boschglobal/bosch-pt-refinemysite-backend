/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.pdf.integration

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.Trace
import java.net.URI
import java.time.Duration
import java.util.Locale
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.ResourceHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Service
open class PdfIntegrationService(
    private val objectMapper: ObjectMapper,
    private val restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${csm.pdf.url}") private val pdfServiceUrl: String,
    @param:Value("\${csm.pdf.connect-timeout-ms:3000}") private val connectTimeoutMillis: Long,
    @param:Value("\${csm.pdf.read-timeout-ms:125000}") private val readTimeoutMillis: Long
) {

  private val restTemplate: RestTemplate by lazy {
    restTemplateBuilder
        .messageConverters(
            ByteArrayHttpMessageConverter(),
            ResourceHttpMessageConverter(),
            FormHttpMessageConverter())
        .setConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
        .setReadTimeout(Duration.ofMillis(readTimeoutMillis))
        .requestFactory { ->
          OkHttp3ClientHttpRequestFactory(
              OkHttpClient().newBuilder().retryOnConnectionFailure(false).build())
        }
        .build()
  }

  @Trace
  @NoPreAuthorize
  @ExcludeFromCodeCoverage
  @Transactional(readOnly = true)
  open fun convertToPdf(uri: URI, postData: Any?, token: String, locale: Locale): Resource {
    val headers = HttpHeaders().apply { contentType = APPLICATION_FORM_URLENCODED }

    val body =
        LinkedMultiValueMap<String, String>().apply {
          add("uri", uri.toString())
          add("locale", locale.toLanguageTag())
          add("token", token)
        }

    try {
      body.add("data", objectMapper.writeValueAsString(postData))
    } catch (e: JsonProcessingException) {
      throw IllegalStateException(e)
    }

    val entity = HttpEntity<MultiValueMap<String, String>>(body, headers)
    val responseEntity =
        restTemplate.postForEntity("$pdfServiceUrl/convert", entity, Resource::class.java)

    return requireNotNull(responseEntity.body)
  }
}
