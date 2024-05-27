/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.etag

import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.lang.Nullable
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [HandlerMethodArgumentResolver] to resolve If-Match headers for optimistic locking handling
 * [HttpHeaders.IF_MATCH].
 */
class ETagArgumentResolver : HandlerMethodArgumentResolver {

  override fun supportsParameter(parameter: MethodParameter) =
      parameter.parameterType == ETag::class.java

  override fun resolveArgument(
      parameter: MethodParameter,
      mavContainer: ModelAndViewContainer?,
      webRequest: NativeWebRequest,
      binderFactory: WebDataBinderFactory?
  ): ETag? {
    val ifMatchHeader = webRequest.getHeader(IF_MATCH)
    return if (ifMatchHeader.isNullOrBlank()) {
      return if (parameter.hasParameterAnnotation(Nullable::class.java)) {
        null
      } else {
        throw HttpMessageNotReadableException(
            "Value for request header field '$IF_MATCH' is missing.", MyHttpInputMessage())
      }
    } else {
      ETag.from(ifMatchHeader)
    }
  }

  private class MyHttpInputMessage : HttpInputMessage {
    override fun getBody(): InputStream = "".byteInputStream(UTF_8)
    override fun getHeaders() = HttpHeaders()
  }
}
