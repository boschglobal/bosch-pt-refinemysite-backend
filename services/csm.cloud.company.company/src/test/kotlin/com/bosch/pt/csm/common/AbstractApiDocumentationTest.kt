/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common

import com.bosch.pt.csm.application.security.AuthorizationTestUtils
import com.bosch.pt.csm.user.user.query.UserProjection
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Locale
import java.util.stream.Collectors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.QueryParametersSnippet
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@AutoConfigureMockMvc
@AutoConfigureRestDocs
abstract class AbstractApiDocumentationTest : AbstractApiIntegrationTest() {

  @Autowired protected lateinit var mockMvc: MockMvc

  @Autowired protected lateinit var objectMapper: ObjectMapper

  protected fun requestBuilder(
      request: MockHttpServletRequestBuilder
  ): MockHttpServletRequestBuilder = requestBuilder(request, null, null, null)

  protected fun requestBuilder(
      request: MockHttpServletRequestBuilder,
      content: Any?
  ): MockHttpServletRequestBuilder = requestBuilder(request, content, null, null)

  protected fun requestBuilder(
      request: MockHttpServletRequestBuilder,
      content: Any?,
      version: Long?
  ): MockHttpServletRequestBuilder = requestBuilder(request, content, version, null)

  protected fun requestBuilder(
      request: MockHttpServletRequestBuilder,
      content: Any?,
      version: Long?,
      user: UserProjection?
  ): MockHttpServletRequestBuilder {
    request
        .locale(Locale.ENGLISH)
        .header(ACCEPT, HAL_JSON_VALUE)
        .header(ACCEPT_LANGUAGE, "en")
        .accept(MediaType.parseMediaType(HAL_JSON_VALUE))

    // there is no other way to get the private RequestBuilder Method
    if ("get" != ReflectionTestUtils.getField(request, "method")) {
      request.contentType(APPLICATION_JSON_VALUE)
    }
    if (version != null) {
      request.header(IF_MATCH, version)
    }
    if (content != null) {
      try {
        request.content(objectMapper.writeValueAsString(content))
      } catch (e: JsonProcessingException) {
        throw IllegalStateException("Object could not be mapped to json", e)
      }
    }
    if (user != null) {
      AuthorizationTestUtils.authorizeWithUser(user, false)
    }
    return request
  }

  protected fun latestVersionOf(path: String): String =
      "/v" + apiVersionProperties.version.max + path

  companion object {

    fun sortingAndPagingRequestParameter(
        allowedSortingProperties: Set<String>
    ): QueryParametersSnippet? =
        queryParameters(
            parameterWithName("sort")
                .description("Allowed values are: " + printSortProperties(allowedSortingProperties))
                .optional(),
            parameterWithName("size").description("see overview of API documentation").optional(),
            parameterWithName("page").description("see overview of API documentation").optional())

    private fun printSortProperties(propertyTranslationMap: Set<String>): String {
      return propertyTranslationMap.stream().sorted().collect(Collectors.joining(","))
    }

    fun buildPagedItemsListResponseFields(
        itemsFieldDescriptors: List<FieldDescriptor>
    ): ResponseFieldsSnippet =
        responseFields(
                fieldWithPath("items[]")
                    .description("List of page items")
                    .type(JsonFieldType.ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                fieldWithPath("totalPages").description("Total number of available pages"),
                fieldWithPath("totalElements").description("Total number of items available"),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("items[].", itemsFieldDescriptors)!!
  }
}
