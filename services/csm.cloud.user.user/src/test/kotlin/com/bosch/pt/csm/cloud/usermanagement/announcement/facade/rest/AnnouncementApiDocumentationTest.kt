/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.AnnouncementController.Companion.ANNOUNCEMENT_RESOURCE_PATH
import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.CreateAnnouncementResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.Announcement
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementBuilder
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementPermissionBuilder
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum.NEUTRAL
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.CreateTranslationResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.google.common.collect.Sets.newHashSet
import java.util.Locale.CHINESE
import java.util.Locale.ENGLISH
import java.util.Locale.FRANCE
import java.util.Locale.GERMAN
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Attributes
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Test and document current Announcement API")
class AnnouncementApiDocumentationTest : AbstractApiDocumentationTest() {

  private val field = ConstrainedFields(CreateAnnouncementResource::class.java)

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUser("user")

    eventStreamGenerator
        .getIdentifier("user")
        .let { repositories.userRepository.findOneByIdentifier(UserId(it))!! }
        .let { AnnouncementPermissionBuilder.announcementPermission().withUser(it).build() }
        .let { repositories.announcementPermissionRepository.save(it) }

    setAuthentication("user")
  }

  @Test
  fun `verify and document get announcement`() {
    val announcement =
        repositories.announcementRepository.save(AnnouncementBuilder.announcement().build())

    mockMvc
        .perform(
            requestBuilder(
                request = get(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)),
                locale = GERMANY))
        .andExpect(status().isOk)
        .andExpectAll(
            jsonPath("$.items.length()").value(1),
            jsonPath("$.items[0].id").value(announcement.identifier.toString()),
            jsonPath("$.items[0].type").value(announcement.type.toString()),
            jsonPath("$.items[0].message").value(announcement.translations[1].value))
        .andDo(
            document(
                "announcements/document-get-announcements",
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("items[].id").description("ID of this announcement"),
                    fieldWithPath("items[].type").description("Type of this announcement"),
                    fieldWithPath("items[].message").description("Message of this announcement"))))
  }

  @Test
  fun `verify and document create announcement`() {
    val englishResource = CreateTranslationResource(ENGLISH.toString(), randomUUID().toString())
    val germanResource = CreateTranslationResource(GERMAN.toString(), randomUUID().toString())
    val announcement =
        CreateAnnouncementResource(NEUTRAL, newHashSet(englishResource, germanResource))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)), announcement))
        .andExpect(status().isCreated)
        .andExpectAll(
            jsonPath("$.id").isNotEmpty,
            jsonPath("$.type").value(announcement.type.toString()),
            jsonPath("$.message").value(englishResource.value))
        .andDo(
            document(
                "announcements/document-create-announcement",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    field
                        .withPath("type")
                        .description("Type of this announcement")
                        .attributes(
                            Attributes.key("description")
                                .value("Valid values are: SUCCESS, NEUTRAL, WARNING, ERROR")),
                    field
                        .withPath("translations[].locale")
                        .description("Language of the translation")
                        .type(JsonFieldType.STRING)
                        .attributes(
                            Attributes.key("constraints")
                                .value("Mandatory field. Size must be between 2 and 3 inclusive.")),
                    field
                        .withPath("translations[].value")
                        .description("Actual translation of the announcement message.")
                        .type(JsonFieldType.STRING)
                        .attributes(
                            Attributes.key("constraints")
                                .value(
                                    "Mandatory field. Size must be between 1 and 128 inclusive."))),
                responseFields(
                    fieldWithPath("id").description("ID of this announcement"),
                    fieldWithPath("type").description("Type of this announcement"),
                    fieldWithPath("message")
                        .description("Message of this announcement for default locale"))))
  }

  @Test
  fun `verify and document delete announcement`() {
    val announcement =
        repositories.announcementRepository.save(AnnouncementBuilder.announcement().build())

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH) + "/{identifier}",
                    announcement.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "announcements/document-delete-announcement",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("identifier").description("ID of the announcement"))))
  }

  @Test
  fun `verify create announcement fails when missing translation for default locale`() {
    val announcement =
        CreateAnnouncementResource(
            NEUTRAL, newHashSet(CreateTranslationResource(GERMAN.toString(), "What a message")))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)), announcement))
        .andExpect(status().isBadRequest)
  }

  @Nested
  inner class VerifyLanguageFallbacks {

    private lateinit var announcement: Announcement

    @BeforeEach
    fun setup() {
      announcement =
          repositories.announcementRepository.save(AnnouncementBuilder.announcement().build())
    }

    @Test
    fun `verify get announcement returns default language when requested language is not supported`() {
      mockMvc
          .perform(
              requestBuilder(
                  request = get(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)),
                  locale = CHINESE))
          .andExpect(status().isOk)
          .andExpectAll(
              jsonPath("$.items.length()").value(1),
              jsonPath("$.items[0].id").value(announcement.identifier.toString()),
              jsonPath("$.items[0].type").value(announcement.type.toString()),
              jsonPath("$.items[0].message").value(announcement.translations[0].value))
    }

    @Test
    fun `verify get announcement returns default language when translation for requested language is missing`() {
      mockMvc
          .perform(
              requestBuilder(
                  request = get(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)),
                  locale = FRANCE))
          .andExpect(status().isOk)
          .andExpectAll(
              jsonPath("$.items.length()").value(1),
              jsonPath("$.items[0].id").value(announcement.identifier.toString()),
              jsonPath("$.items[0].type").value(announcement.type.toString()),
              jsonPath("$.items[0].message").value(announcement.translations[0].value))
    }
  }

  @Test
  fun verifyEmptyResult() {
    mockMvc
        .perform(
            requestBuilder(
                request = get(latestVersionOfAnnouncementApi(ANNOUNCEMENT_RESOURCE_PATH)),
                locale = GERMANY))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.items.length()").value(0))
  }
}
