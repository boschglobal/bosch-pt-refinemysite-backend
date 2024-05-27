/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CUSTOM2
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.request.UpdateRfvResource
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_ACTIVATE
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_DEACTIVATE
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_UPDATE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class RfvApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val userTest by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(userTest.identifier!!)
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document find rfvs`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/{projectId}/rfvs"), getIdentifier("project")),
            ))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(14))
        .andDo(
            document(
                "rfvs/document-find-rfvs",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                PATH_PARAMETER_PROJECT,
                responseFields(
                    listOf(
                        subsectionWithPath("items[]")
                            .description("A list with reasons for variance.")
                            .type(ARRAY),
                        subsectionWithPath("_links").ignored()))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update rfv`() {
    val updateRfvResource = UpdateRfvResource(CUSTOM2, true, "This is a custom RFV")

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/{projectId}/rfvs"), getIdentifier("project")),
                updateRfvResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.key").value(updateRfvResource.key.toString()),
            jsonPath("$.active").value(updateRfvResource.active.toString()),
            jsonPath("$.name").value(updateRfvResource.name),
            jsonPath("$._links.deactivate").exists())
        .andDo(
            document(
                "rfvs/document-update-rfv",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                PATH_PARAMETER_PROJECT,
                requestFields(RFV_REQUEST_FIELD_DESCRIPTORS),
                responseFields(RFV_RESPONSE_FIELD_DESCRIPTORS),
                links(RFV_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(RfvCustomizationEventAvro::class.java, CREATED, 1)
        .first()
        .aggregate
        .also { aggregate ->
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, RFVCUSTOMIZATION, userTest)
          assertThat(aggregate.active).isEqualTo(updateRfvResource.active)
          assertThat(aggregate.key)
              .isEqualTo(DayCardReasonNotDoneEnumAvro.valueOf(updateRfvResource.key.name))
          assertThat(aggregate.name).isEqualTo(updateRfvResource.name)
          assertThat(aggregate.project.identifier.toUUID())
              .isEqualTo(getIdentifier("project"))
        }
  }

  companion object {

    const val LINK_UPDATE_RFVS_DESCRIPTION = "Link to update RFVs."
    private const val LINK_ACTIVATE_RFV = "Link to activate a RFV."
    private const val LINK_DEACTIVATE_RFV = "Link to deactivate a RFV."
    private const val LINK_UPDATE_RFV = "Link to update a RFV (rename, etc.)."

    private val RFV_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_RFV_ACTIVATE).description(LINK_ACTIVATE_RFV).optional(),
            linkWithRel(LINK_RFV_DEACTIVATE).description(LINK_DEACTIVATE_RFV).optional(),
            linkWithRel(LINK_RFV_UPDATE).description(LINK_UPDATE_RFV))

    private val PATH_PARAMETER_PROJECT =
        pathParameters(
            parameterWithName(RfvController.PATH_VARIABLE_PROJECT_ID)
                .description("ID of the project"))

    private val field = ConstrainedFields(UpdateRfvResource::class.java)

    private val RFV_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            field.withPath("key").description("The reason for variance").type(STRING),
            field.withPath("active").description("The RFV is active or not").type(BOOLEAN),
            field.withPath("name").description("Name of the custom RFV").type(STRING))

    private val RFV_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("key").description("The reason for variance").type(STRING),
            fieldWithPath("active").description("The RFV is active or not").type(BOOLEAN),
            fieldWithPath("name").description("Name of the custom RFV").type(STRING),
            subsectionWithPath("_links").ignored())
  }
}
