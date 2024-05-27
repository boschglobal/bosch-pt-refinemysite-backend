/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Locale
import java.util.Locale.UK
import jakarta.annotation.Nonnull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.parseMediaType
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

/** Base class for testing and documenting server side api's. */
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SmartSiteSpringBootTest
@EnableAllKafkaListeners
abstract class AbstractApiDocumentationTest : AbstractIntegrationTest() {

  @Autowired protected lateinit var mockMvc: MockMvc

  @Autowired protected lateinit var apiVersionProperties: ApiVersionProperties

  @Autowired protected lateinit var objectMapper: ObjectMapper

  protected fun requestBuilder(
      @Nonnull request: MockHttpServletRequestBuilder,
      content: Any? = null,
      version: Long? = null,
      user: User? = null,
      locale: Locale = UK
  ): MockHttpServletRequestBuilder {
    request.locale(locale).accept(parseMediaType(HAL_JSON_VALUE))

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
      authorizeWithUser(user, false)
    }

    return request
  }

  protected fun latestVersionOf(path: String): String =
      "/v" + apiVersionProperties.version.max + path

  protected fun version1of(path: String): String = "/v1$path"

  protected fun version2of(path: String): String = "/v2$path"

  protected fun latestVersionOfAnnouncementApi(path: String): String = "/v1$path"

  protected fun latestVersionOfCraftApi(path: String): String = "/v1$path"

  companion object {
    fun buildSortingAndPagingParameterDescriptors(allowedSortingProperties: Set<String>) =
        listOf(
            parameterWithName("sort")
                .description("Allowed values are: " + printSortProperties(allowedSortingProperties))
                .optional(),
            parameterWithName("size").description("see overview of API documentation").optional(),
            parameterWithName("page").description("see overview of API documentation").optional())

    private fun printSortProperties(propertyTranslationMap: Set<String>) =
        propertyTranslationMap.sorted().joinToString(",")

    fun buildPagedItemsListResponseFields(itemsFieldDescriptors: List<FieldDescriptor>) =
        buildPagedListResponseFields("items", itemsFieldDescriptors)

    fun buildPagedListResponseFields(
        itemsFieldName: String,
        itemsFieldDescriptors: List<FieldDescriptor>
    ) =
        responseFields(
                fieldWithPath("$itemsFieldName[]").description("List of page items").type(ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                fieldWithPath("totalPages").description("Total number of available pages"),
                fieldWithPath("totalElements").description("Total number of items available"),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("$itemsFieldName[].", itemsFieldDescriptors)
  }
}
