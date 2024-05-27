/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.common.extensions.toUserId
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ApiDocumentationSnippets
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.ConstrainedFields
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatUpdated
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.asPatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler.CreatePatCommandHandler.GeneratedPat
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.request.CreateOrUpdatePatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import java.util.UUID
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.snippet.Attributes
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PatApiDocumentationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUserAndActivate("user")
    setAuthentication("user")
  }

  @Test
  fun `verify and document list PAT`() {

    val userId = get<UserAggregateAvro>("user")!!.aggregateIdentifier.identifier.asUserId()

    eventStreamGenerator //
        .createThreePats(userId)
        .submitPatUpdated("PAT2", "user") {
          it.description = "PAT 2 is thoroughly updated"
          it.scopes = listOf(PatScopeEnumAvro.TIMELINE_API_READ)
        }

    mockMvc
        .perform(
            requestBuilder(
                RestDocumentationRequestBuilders.get(latestVersionOf("/users/current/pats")),
            ),
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.items[*].id").exists())
        .andExpect(jsonPath("$.items[*].version", hasItems(0, 1, 0)))
        .andExpect(
            jsonPath(
                "$.items[*].description",
                hasItems(
                    "A Personal Access Token for some test",
                    "PAT 2 is thoroughly updated",
                    "A Personal Access Token for some test"),
            ))
        .andExpect(
            jsonPath(
                "$.items[*].scopes",
                hasItems(
                    hasItems("GRAPHQL_API_READ"),
                    hasItems("TIMELINE_API_READ"),
                    hasItems("GRAPHQL_API_READ", "TIMELINE_API_READ"),
                )))
        .andDo(
            document(
                "pat/document-list-pat",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(
                        fieldWithPath("items[]").description("List of PAT items").type(ARRAY),
                        subsectionWithPath("_links").ignored())
                    .andWithPrefix("items[].", PAT_ITEM_RESPONSE_FIELD_DESCRIPTORS),
            ))
  }

  @Test
  fun `verify and document create PAT`() {

    val createPatResource =
        CreateOrUpdatePatResource(
            description = "This is my PAT",
            scopes = listOf(PatScopeEnum.GRAPHQL_API_READ, PatScopeEnum.TIMELINE_API_READ),
            validForMinutes = 1440,
        )

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/pats")), createPatResource))
        .andExpect(jsonPath("id").exists())
        .andExpect(jsonPath("version").value(0))
        .andExpect(jsonPath("description").value("This is my PAT"))
        .andExpect(jsonPath("scopes", hasItems("GRAPHQL_API_READ", "TIMELINE_API_READ")))
        .andExpect(jsonPath("type").value("RMSPAT1"))
        .andExpect(status().isCreated)
        .andDo(
            document(
                "pat/document-create-pat",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(CREATE_PAT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR),
                responseFields(CREATE_PAT_RESPONSE_FIELD_DESCRIPTORS),
            ),
        )

    val createdPat = requireNotNull(repositories.patRepository.findAll().first())

    assertEquals(createdPat.description, createPatResource.description)
    assertEquals(createdPat.issuedAt.plusDays(1), createdPat.expiresAt)
    assertEquals(createdPat.scopes.map { it.name }, createPatResource.scopes.map { it.name })
  }

  @Test
  fun `verify and document update PAT`() {
    val user = get<UserAggregateAvro>("user")!!

    val pat =
        eventStreamGenerator
            .submitPatCreated("PAT", "user") { event ->
              val patIdentifier = UUID.randomUUID().toString()
              event.aggregateIdentifierBuilder =
                  AggregateIdentifierAvro.newBuilder()
                      .setIdentifier(patIdentifier)
                      .setType(PatAggregateTypeEnum.PAT.name)
                      .setVersion(0)
              GeneratedPat(
                      type = PatTypeEnum.RMSPAT1,
                      impersonatedUser = user.aggregateIdentifier.identifier.asUserId(),
                      patId = PatId(patIdentifier))
                  .let { event.hash = it.hash }
            }
            .get<PatCreatedEventAvro>("PAT")!!

    val updatePatResource =
        CreateOrUpdatePatResource(
            description = "This is my updated PAT",
            scopes = listOf(PatScopeEnum.GRAPHQL_API_READ),
            validForMinutes = 24 * 60 * 3)

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf("/users/current/pats/{patId}"),
                    pat.aggregateIdentifier.identifier),
                updatePatResource,
                0L))
        .andExpect(status().isOk)
        .andExpect(jsonPath("id").exists())
        .andExpect(jsonPath("version").value(1))
        .andExpect(jsonPath("description").value("This is my updated PAT"))
        .andExpect(jsonPath("scopes", hasItems("GRAPHQL_API_READ")))
        .andExpect(jsonPath("type").value("RMSPAT1"))
        .andDo(
            document(
                "pat/document-update-pat",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(PAT_ITEM_RESPONSE_FIELD_DESCRIPTORS),
            ),
        )

    val updatedPat =
        requireNotNull(
            repositories.patRepository.findByIdentifier(PatId(pat.aggregateIdentifier.identifier)))

    assertEquals(updatedPat.description, updatePatResource.description)
    assertEquals(updatedPat.issuedAt.plusDays(3), updatedPat.expiresAt)
    assertEquals(updatedPat.scopes.map { it.name }, updatePatResource.scopes.map { it.name })
  }

  @Test
  fun `verify and document delete PAT`() {
    val user = get<UserAggregateAvro>("user")!!

    val pat2 =
        eventStreamGenerator
            .createThreePats(user.aggregateIdentifier.identifier.asUserId())
            .get<PatCreatedEventAvro>("PAT2")!!

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf("/users/current/pats/{patId}"),
                    pat2.aggregateIdentifier.identifier),
                null,
                0L))
        .andExpect(status().isOk)
        .andDo(
            document(
                "pat/document-delete-pat",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
            ))

    val remainingPats =
        repositories.patRepository.findByImpersonatedUser(user.toUserId(), Sort.by("issuedAt"))
    assertEquals(2, remainingPats.size)
    val deletedPat =
        repositories.patRepository.findByIdentifier(PatId(pat2.aggregateIdentifier.identifier))
    assertNull(deletedPat)
  }

  fun PatCreatedEventAvro.Builder.withAggregateIdentifier(patId: PatId) =
      this.apply {
        this.aggregateIdentifierBuilder =
            AggregateIdentifierAvro.newBuilder()
                .setIdentifier(patId.toString())
                .setType(PatAggregateTypeEnum.PAT.name)
                .setVersion(0)
      }

  private fun EventStreamGenerator.createThreePats(userId: UserId) =
      this.submitPatCreated("PAT1", "user") { event ->
            val patId = UUID.randomUUID().asPatId()
            event.withAggregateIdentifier(patId)
            GeneratedPat(
                    type = PatTypeEnum.RMSPAT1,
                    impersonatedUser = userId,
                    patId = patId,
                )
                .let {
                  event.hash = it.hash
                  event.scopes = listOf(PatScopeEnumAvro.GRAPHQL_API_READ)
                }
          }
          .submitPatCreated("PAT2", "user") { event ->
            val patId = UUID.randomUUID().asPatId()
            event.withAggregateIdentifier(patId)
            GeneratedPat(
                    type = PatTypeEnum.RMSPAT1,
                    impersonatedUser = userId,
                    patId = patId,
                )
                .let { event.hash = it.hash }
          }
          .submitPatCreated("PAT3", "user") { event ->
            val patId = UUID.randomUUID().asPatId()
            event.withAggregateIdentifier(patId)
            GeneratedPat(
                    type = PatTypeEnum.RMSPAT1,
                    impersonatedUser = userId,
                    patId = patId,
                )
                .let { event.hash = it.hash }
          }

  companion object {

    private val ABSTRACT_AUDITING_RESPONSE_FIELD_DESCRIPTORS =
        arrayOf(
            fieldWithPath("createdBy.displayName")
                .description("Name of the user that created the resource"),
            fieldWithPath("createdBy.id").description("ID of the user that created the resource"),
            fieldWithPath("createdDate").description("Date of resource creation"),
            fieldWithPath("lastModifiedBy.displayName")
                .description("Name of the user that modified the resource last"),
            fieldWithPath("lastModifiedBy.id")
                .description("ID of the user that modified the resource last"),
            fieldWithPath("lastModifiedDate").description("Date of the last modification"),
        )

    private val CREATE_PAT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of this PAT"),
            fieldWithPath("version").description("Version of the PAT"),
            fieldWithPath("description").description("User-defined description of the PAT"),
            fieldWithPath("issuedAt").description("Date and time when the token was issued"),
            fieldWithPath("expiresAt").description("Date and time when the token expires"),
            fieldWithPath("scopes").description("List of PAT-scopes chosen by the user"),
            fieldWithPath("token")
                .description("The actual secret token value. For the user's eyes only"),
            fieldWithPath("type").description("Type of the PAT (i.e. RMSPAT1)"),
            fieldWithPath("impersonatedUser")
                .description("UUID of the user impersonated by this token"),
            *ABSTRACT_AUDITING_RESPONSE_FIELD_DESCRIPTORS,
            subsectionWithPath("_links").ignored(),
        )

    private val PAT_ITEM_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of this PAT"),
            fieldWithPath("version").description("Version of the PAT"),
            fieldWithPath("description").description("User-defined description of the PAT"),
            fieldWithPath("issuedAt").description("Date and time when the token was issued"),
            fieldWithPath("expiresAt").description("Date and time when the token expires"),
            fieldWithPath("scopes").description("List of PAT-scopes chosen by the user"),
            fieldWithPath("type").description("Type of the PAT (i.e. RMSPAT1)"),
            fieldWithPath("impersonatedUser")
                .description("UUID of the user impersonated by this token"),
            *ABSTRACT_AUDITING_RESPONSE_FIELD_DESCRIPTORS,
            subsectionWithPath("_links").ignored(),
        )

    private val CREATE_PAT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateOrUpdatePatResource::class.java)
                .withPath("description")
                .description("User-defined description of the PAT")
                .type(STRING)
                .attributes(Attributes.key("constraints").value("Mandatory field.")),
            ConstrainedFields(CreateOrUpdatePatResource::class.java)
                .withPath("validForMinutes")
                .description(
                    "Validity period of the PAT in minutes from now. " +
                        "Must be between 1440 and 525600 minutes (1 to 365 days).")
                .type(NUMBER)
                .attributes(Attributes.key("constraints").value("Mandatory field.")),
            ConstrainedFields(CreateOrUpdatePatResource::class.java)
                .withPath("scopes")
                .description("")
                .type(ARRAY)
                .attributes(
                    Attributes.key("constraints")
                        .value(
                            "Scope(s) of the PAT. Allowed values are GRAPHQL_API_READ, TIMELINE_API_READ"),
                ),
        )
  }
}
