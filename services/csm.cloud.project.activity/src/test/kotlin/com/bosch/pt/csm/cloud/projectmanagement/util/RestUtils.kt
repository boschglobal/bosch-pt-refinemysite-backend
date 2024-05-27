/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

fun requestBuilder(request: MockHttpServletRequestBuilder, objectMapper: ObjectMapper) =
    requestBuilder(request, null, objectMapper)

fun requestBuilder(
    request: MockHttpServletRequestBuilder,
    content: Any?,
    objectMapper: ObjectMapper
): MockHttpServletRequestBuilder {
  request
      .locale(LocaleContextHolder.getLocale())
      .header(ACCEPT, HAL_JSON_VALUE)
      .header(ACCEPT_LANGUAGE, LocaleContextHolder.getLocale().language)
      .accept(MediaType.parseMediaType(HAL_JSON_VALUE))

  // there is no other way to get the private RequestBuilder Method
  if ("get" != ReflectionTestUtils.getField(request, "method")) {
    request.contentType(MediaType.APPLICATION_JSON_VALUE)
  }

  content?.let {
    try {
      request.content(objectMapper.writeValueAsString(content))
    } catch (e: JsonProcessingException) {
      throw IllegalStateException("Object could not be mapped to json", e)
    }
  }
  return request
}
