/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import java.io.InputStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [HandlerMethodArgumentResolver] to resolve If-Match headers for optimistic locking handling
 * [org.springframework.http.HttpHeaders.IF_MATCH].
 */
class ETagArgumentResolver : HandlerMethodArgumentResolver {

  /*
   * (non-Javadoc)
   * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(
   * org.springframework.core.MethodParameter)
   */
  override fun supportsParameter(parameter: MethodParameter): Boolean =
      parameter.parameterType == ETag::class.java

  /*
   * (non-Javadoc)
   * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(
   * org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer,
   * org.springframework.web.context.request.NativeWebRequest,
   * org.springframework.web.bind.support.WebDataBinderFactory)
   */
  override fun resolveArgument(
      parameter: MethodParameter,
      mavContainer: ModelAndViewContainer?,
      webRequest: NativeWebRequest,
      binderFactory: WebDataBinderFactory?
  ): ETag {
    val ifMatchHeader = webRequest.getHeader(HttpHeaders.IF_MATCH)
    if (StringUtils.isBlank(ifMatchHeader)) {
      throw HttpMessageNotReadableException(
          "Value for request header field '" + HttpHeaders.IF_MATCH + "' is " + "missing.",
          MyHttpInputMessage())
    }
    return ETag.from(ifMatchHeader!!)
  }

  private class MyHttpInputMessage : HttpInputMessage {

    override fun getBody(): InputStream = IOUtils.toInputStream("", "UTF-8")

    override fun getHeaders(): HttpHeaders = HttpHeaders()
  }
}
