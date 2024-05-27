/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import java.util.Locale.ENGLISH

@Suppress("MemberVisibilityCanBePrivate")
object RestUtils {

    fun requestBuilder(request: MockHttpServletRequestBuilder, objectMapper: ObjectMapper) =
        requestBuilder(request, null, objectMapper)

    fun requestBuilder(
        request: MockHttpServletRequestBuilder,
        content: Any?,
        objectMapper: ObjectMapper
    ): MockHttpServletRequestBuilder {
        request
            .locale(ENGLISH)
            .header(ACCEPT, HAL_JSON_VALUE)
            .header(ACCEPT_LANGUAGE, "en")
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
}
