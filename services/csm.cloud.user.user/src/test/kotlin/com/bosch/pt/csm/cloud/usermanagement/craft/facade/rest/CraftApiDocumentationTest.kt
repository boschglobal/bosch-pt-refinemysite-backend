/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.facade.rest

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.ID_AND_VERSION_FIELD_DESCRIPTORS
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.CreateTranslationResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFTS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.CraftController.Companion.CRAFT_BY_CRAFT_ID_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CreateCraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.util.Locale.ENGLISH
import java.util.Locale.GERMAN
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.IanaLinkRelations.PREV
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@CodeExample
class CraftApiDocumentationTest : AbstractApiDocumentationTest() {

  private lateinit var craft1: Craft
  private lateinit var craft2: Craft

  /** Initializes required test data. */
  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitCraft("craft1") {
          it.defaultName = CRAFT1_NAME_EN
          it.translations =
              listOf(
                  CraftTranslationAvro.newBuilder()
                      .setLocale(GERMAN.toString())
                      .setValue(CRAFT1_NAME_DE)
                      .build(),
                  CraftTranslationAvro.newBuilder()
                      .setLocale(ENGLISH.toString())
                      .setValue(CRAFT1_NAME_EN)
                      .build())
        }
        .submitCraft("craft2") {
          it.defaultName = CRAFT2_NAME_EN
          it.translations =
              listOf(
                  CraftTranslationAvro.newBuilder()
                      .setLocale(GERMAN.toString())
                      .setValue(CRAFT2_NAME_DE)
                      .build(),
                  CraftTranslationAvro.newBuilder()
                      .setLocale(ENGLISH.toString())
                      .setValue(CRAFT2_NAME_EN)
                      .build())
        }
        .repeat(2)
        .submitUser("user")

    craft1 =
        repositories.craftRepository.findOneWithUserAndTranslationsByIdentifier(
            CraftId(eventStreamGenerator.getIdentifier("craft1"))
        )!!
    craft2 =
        repositories.craftRepository.findOneWithUserAndTranslationsByIdentifier(
            CraftId(eventStreamGenerator.getIdentifier("craft2"))
        )!!
  }

  @Test
  fun `verify and document create craft`() {
    val createTranslationResourceEnglish = CreateTranslationResource("en", CRAFT1_NAME_EN)
    val createTranslationResourceGerman = CreateTranslationResource("de", CRAFT1_NAME_DE)
    val createCraftResource = CreateCraftResource()
    createCraftResource.translations =
        setOf(createTranslationResourceEnglish, createTranslationResourceGerman)

    setAuthentication("admin")

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOfCraftApi(CRAFTS_ENDPOINT_PATH)), createCraftResource)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.translations[0].locale").exists())
        .andExpect(jsonPath("$.translations[0].value").exists())
        .andExpect(jsonPath("$.translations[1].locale").exists())
        .andExpect(jsonPath("$.translations[1].value").exists())
        .andDo(
            document(
                "crafts/document-create-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(CREATE_CRAFT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                responseFields(CREATE_CRAFT_RESPONSE_FIELD_DESCRIPTORS),
                links(linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION))))
  }

  @Test
  fun `verify and document create craft with identifier`() {
    val id = randomUUID()
    val createTranslationResourceEnglish = CreateTranslationResource("en", CRAFT1_NAME_EN)
    val createTranslationResourceGerman = CreateTranslationResource("de", CRAFT1_NAME_DE)
    val createCraftResource = CreateCraftResource()
    createCraftResource.translations =
        setOf(createTranslationResourceEnglish, createTranslationResourceGerman)

    setAuthentication("admin")

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOfCraftApi(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH), id.toString()),
                createCraftResource))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.translations[0].locale").exists())
        .andExpect(jsonPath("$.translations[0].value").exists())
        .andExpect(jsonPath("$.translations[1].locale").exists())
        .andExpect(jsonPath("$.translations[1].value").exists())
        .andDo(
            document(
                "crafts/document-create-craft-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PATH_PARAMETER_CRAFT_ID),
                requestFields(CREATE_CRAFT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                responseFields(CREATE_CRAFT_RESPONSE_FIELD_DESCRIPTORS),
                links(linkWithRel(SELF.value()).description(LINK_SELF_DESCRIPTION))))
  }

  @Test
  fun `verify and document list crafts`() {

    setAuthentication("user")

    mockMvc
        .perform(
            requestBuilder(
                request = get(latestVersionOfCraftApi(CRAFTS_ENDPOINT_PATH)), locale = GERMANY))
        .andExpect(status().isOk)
        .andExpect(jsonPath(PATH_CRAFTS_LENGTH).value(2))
        .andExpect(jsonPath("$.crafts[0].id").value(craft1.getIdentifierUuid().toString()))
        .andExpect(jsonPath("$.crafts[1].id").value(craft2.getIdentifierUuid().toString()))
        .andExpect(jsonPath("$.crafts[0].name").value(CRAFT1_NAME_DE))
        .andExpect(jsonPath("$.crafts[1].name").value(CRAFT2_NAME_DE))
        .andDo(
            document(
                "crafts/document-get-crafts",
                preprocessResponse(prettyPrint()),
                CRAFT_LIST_RESPONSE_FIELD_DESCRIPTORS,
                links(
                    linkWithRel(SELF.value()).description("Links to the crafts resource itself"),
                    linkWithRel(NEXT.value())
                        .optional()
                        .description("Links to the next page of the craft list."),
                    linkWithRel(PREV.value())
                        .optional()
                        .description("Links to the previous page of the craft list."))))
  }

  @Test
  fun `verify and document get single craft`() {

    setAuthentication("user")

    mockMvc
        .perform(
            requestBuilder(
                request =
                    get(
                        latestVersionOfCraftApi(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH),
                        craft1.getIdentifierUuid().toString()),
                locale = GERMANY))
        .andExpect(status().isOk)
        .andDo(
            document(
                "crafts/document-get-single-craft",
                preprocessResponse(prettyPrint()),
                pathParameters(PATH_PARAMETER_CRAFT_ID),
                responseFields(CRAFT_RESPONSE_FIELD_DESCRIPTORS),
                links(linkWithRel(SELF.value()).description("Link to this very craft"))))
  }

  @Test
  fun `verify page links first page`() {

    setAuthentication("user")

    mockMvc
        .perform(
            requestBuilder(get(latestVersionOfCraftApi(CRAFTS_ENDPOINT_PATH)).param("size", "1")))
        .andExpect(jsonPath("$._links.next").exists())
        .andExpect(jsonPath("$._link.prev").doesNotExist())
  }

  @Test
  fun `verify page links second page`() {

    setAuthentication("user")

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOfCraftApi(CRAFTS_ENDPOINT_PATH))
                    .param("size", "1")
                    .param("page", "1")))
        .andExpect(jsonPath("$._links.next").doesNotExist())
        .andExpect(jsonPath("$._links.prev").exists())
  }

  companion object {
    private const val CRAFT1_NAME_DE = "Vorbereitende Maßnahmen"
    private const val CRAFT2_NAME_DE = "Stahlbauarbeiten"
    private const val CRAFT1_NAME_EN = "preparatory steps"
    private const val CRAFT2_NAME_EN = "constructional steelwork"
    private const val PATH_CRAFTS_LENGTH = "$.crafts.length()"
    private const val LINK_SELF_DESCRIPTION = "Link to the craft resource itself"

    private val PATH_PARAMETER_CRAFT_ID =
        parameterWithName("craftId").description("ID of the craft")

    private val CREATE_CRAFT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ID_AND_VERSION_FIELD_DESCRIPTORS,
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("defaultName").description("Default name of craft"),
            fieldWithPath("translations[].locale").description("Language of the name of the craft"),
            fieldWithPath("translations[].value").description("Actual translation of the craft"),
            subsectionWithPath("_links").ignored())

    private val CRAFT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of this craft"),
            fieldWithPath("name").description("Name of the craft"),
            subsectionWithPath("_links").ignored())

    private val CRAFT_LIST_RESPONSE_FIELD_DESCRIPTORS =
        buildPagedListResponseFields(
            "crafts",
            listOf(
                fieldWithPath(".id").description("ID of this craft"),
                fieldWithPath(".name").description("Name of the craft"),
                subsectionWithPath("._links").ignored()))

    private val CREATE_CRAFT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateCraftResource::class.java)
                .withPath("translations[].locale")
                .description("Language of the translation")
                .type(STRING)
                .attributes(
                    key("constraints")
                        .value("Mandatory field. Size must be between 2 and 3 inclusive.")),
            ConstrainedFields(CreateCraftResource::class.java)
                .withPath("translations[].value")
                .description("Actual translation of the craft")
                .type(STRING)
                .attributes(
                    key("constraints")
                        .value("Mandatory field. Size must be between 1 and 128 inclusive.")))
  }
}
