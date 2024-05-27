/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LINK_CREATE_DESCRIPTION
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LINK_DELETE_DESCRIPTION
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LINK_NEXT_DESCRIPTION
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LINK_PREVIOUS_DESCRIPTION
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LINK_SELF_DESCRIPTION
import com.bosch.pt.iot.smartsite.user.model.User
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.Nonnull
import java.util.Locale.ENGLISH
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.parseMediaType
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.LinkDescriptor
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.RequestFieldsSnippet
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.QueryParametersSnippet
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

/** Base class for testing and documenting server side api's. */
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SmartSiteSpringBootTest
abstract class AbstractApiDocumentationTestV2 : AbstractIntegrationTestV2() {

  @Autowired private lateinit var apiVersionProperties: ApiVersionProperties

  @Autowired protected lateinit var mockMvc: MockMvc

  @Autowired protected lateinit var objectMapper: ObjectMapper

  private fun buildIdentifierTypeParameter(
      validValue: String,
      defaultType: String
  ): ParameterDescriptor =
      parameterWithName("identifierType")
          .description(
              """
              Optional identifier to specify what kind of resource ids are passed.            
              Valid value is $validValue.          
              Default is $defaultType.
              """
                  .trimIndent())
          .optional()

  protected fun buildIdentifierTypeRequestParameter(
      validValue: String,
      defaultType: String
  ): QueryParametersSnippet = queryParameters(buildIdentifierTypeParameter(validValue, defaultType))

  protected fun buildIdentifierTypeAndPagingRequestParameter(
      validValue: String,
      defaultType: String
  ): QueryParametersSnippet =
      queryParameters(
          parameterWithName("size")
              .description("The size for the page returned in the request")
              .optional(),
          parameterWithName("page")
              .description("The number of the page returned in the request")
              .optional(),
          buildIdentifierTypeParameter(validValue, defaultType))

  protected fun requestBuilder(
      @Nonnull request: MockHttpServletRequestBuilder,
      content: Any? = null,
      version: Long? = null,
      user: User? = null
  ): MockHttpServletRequestBuilder {
    request
        .locale(ENGLISH)
        .header(ACCEPT, HAL_JSON_VALUE)
        .header(ACCEPT_LANGUAGE, "en")
        .accept(parseMediaType(HAL_JSON_VALUE))

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
        throw IllegalArgumentException("Object could not be mapped to json", e)
      }
    }
    if (user != null) {
      AuthorizationTestUtils.authorizeWithUser(user, false)
    }
    return request
  }

  protected fun latestVersionOf(path: String) = "/v" + apiVersionProperties.version.max + path

  protected fun version4of(path: String) = "/v4$path"

  protected fun version3of(path: String) = "/v3$path"

  protected fun version2of(path: String) = "/v2$path"

  protected fun version1of(path: String) = "/v1$path"

  companion object {

    private val BATCH_REQUEST_CONSTRAINED_FIELD =
        ConstrainedFields(BatchRequestResource::class.java)

    private val VERSIONED_BATCH_REQUEST_CONSTRAINED_FIELD =
        ConstrainedFields(BatchRequestResource::class.java)

    private val CREATE_BATCH_REQUEST_CONSTRAINED_FIELD =
        ConstrainedFields(CreateBatchRequestResource::class.java)

    private val CREATE_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS =
        listOf(
            CREATE_BATCH_REQUEST_CONSTRAINED_FIELD.withPath("items[]")
                .description("List of items")
                .type(ARRAY))

    val BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS =
        listOf(
            BATCH_REQUEST_CONSTRAINED_FIELD.withPath("ids[]")
                .description("List of IDs of the resources to be retrieved in a batch")
                .type(ARRAY))

    val VERSIONED_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS =
        listOf(
            VERSIONED_BATCH_REQUEST_CONSTRAINED_FIELD.withPath("items[]")
                .description(
                    "List of versioned references of the resource to be updated in a batch")
                .type(ARRAY),
            VERSIONED_BATCH_REQUEST_CONSTRAINED_FIELD.withPath("items[].id")
                .description("ID of the resource to be updated in a batch")
                .type(STRING),
            VERSIONED_BATCH_REQUEST_CONSTRAINED_FIELD.withPath("items[].version")
                .description("Version of the resource to be updated in a batch")
                .type(NUMBER))

    val SELF_LINK_DESCRIPTOR: LinkDescriptor =
        linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION)

    val CREATE_LINK_DESCRIPTOR: LinkDescriptor =
        linkWithRel("create").description(LINK_CREATE_DESCRIPTION)

    val DELETE_LINK_DESCRIPTOR: LinkDescriptor =
        linkWithRel("delete").description(LINK_DELETE_DESCRIPTION)

    val NEXT_LINK_DESCRIPTOR: LinkDescriptor =
        linkWithRel("next").description(LINK_NEXT_DESCRIPTION)

    val PREVIOUS_LINK_DESCRIPTOR: LinkDescriptor =
        linkWithRel("prev").description(LINK_PREVIOUS_DESCRIPTION)

    fun buildSortingAndPagingParameterDescriptors(allowedSortingProperties: Set<String>) =
        listOf(
            parameterWithName("sort")
                .description("Allowed values are: " + printSortProperties(allowedSortingProperties))
                .optional(),
            parameterWithName("size").description("see overview of API documentation").optional(),
            parameterWithName("page").description("see overview of API documentation").optional())

    fun buildBatchItemsListResponseFields(
        itemsFieldDescriptors: List<FieldDescriptor>
    ): ResponseFieldsSnippet =
        responseFields(
                fieldWithPath("items[]").description("List of items").type(ARRAY),
                subsectionWithPath("_links").optional().ignored())
            .andWithPrefix("items[].", itemsFieldDescriptors)

    fun buildCreateBatchRequestFields(
        itemsFieldDescriptors: List<FieldDescriptor>
    ): RequestFieldsSnippet =
        requestFields(CREATE_BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS)
            .andWithPrefix("items[].", itemsFieldDescriptors)

    fun buildPagedItemsListResponseFields(
        itemsFieldDescriptors: List<FieldDescriptor>
    ): ResponseFieldsSnippet =
        responseFields(fieldWithPath("items[]").description("List of page items").type(ARRAY))
            .andWithPrefix("items[].", itemsFieldDescriptors)
            .and(
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                fieldWithPath("totalPages").description("Total number of available pages"),
                fieldWithPath("totalElements").description("Total number of items available"),
                subsectionWithPath("_links").ignored())

    private fun printSortProperties(propertyTranslationMap: Set<String>) =
        propertyTranslationMap.sorted().joinToString(",")
  }
}
