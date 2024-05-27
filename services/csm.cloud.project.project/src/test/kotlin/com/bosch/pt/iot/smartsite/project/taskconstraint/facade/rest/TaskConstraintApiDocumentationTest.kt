/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKCONSTRAINTCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_ACTIVATE
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_DEACTIVATE
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource.Companion.LINK_CONSTRAINT_UPDATE
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
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
import org.springframework.restdocs.payload.JsonFieldType
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
class TaskConstraintApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val userTest by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val projectIdentifier by lazy { getIdentifier("project") }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(userTest.identifier!!)
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document find constraints`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/{projectId}/constraints"), projectIdentifier),
            ))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(12),
        )
        .andDo(
            document(
                "task-constraints/document-find-constraints",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                PATH_PARAMETER_PROJECT,
                RESPONSE_FIELDS_LIST))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update constraint`() {
    val updateConstraintResource =
        UpdateTaskConstraintResource(
            TaskConstraintEnum.CUSTOM2, true, "This is a custom constraint")

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/{projectId}/constraints"), projectIdentifier),
                updateConstraintResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.key").value(updateConstraintResource.key.toString()),
            jsonPath("$.active").value(updateConstraintResource.active.toString()),
            jsonPath("$.name").value(updateConstraintResource.name),
            jsonPath("$._links.deactivate").exists(),
        )
        .andDo(
            document(
                "task-constraints/document-update-constraint",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                PATH_PARAMETER_PROJECT,
                requestFields(CONSTRAINT_REQUEST_FIELD_DESCRIPTORS),
                responseFields(CONSTRAINT_RESPONSE_FIELD_DESCRIPTORS),
                links(CONSTRAINT_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskConstraintCustomizationEventAvro::class.java, CREATED, 1)
        .first()
        .getAggregate()
        .also { aggregate ->
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, TASKCONSTRAINTCUSTOMIZATION, userTest)
          assertThat(aggregate.getActive()).isEqualTo(updateConstraintResource.active)
          assertThat(aggregate.getKey())
              .isEqualTo(TaskActionEnumAvro.valueOf(updateConstraintResource.key.name))
          assertThat(aggregate.getName()).isEqualTo(updateConstraintResource.name)
          assertThat(aggregate.getProject().getIdentifier().toUUID()).isEqualTo(projectIdentifier)
        }
  }

  companion object {

    const val LINK_UPDATE_TASK_CONSTRAINTS_DESCRIPTION = "Link to update task constraints."
    private const val LINK_ACTIVATE_CONSTRAINT = "Link to activate a constraint."
    private const val LINK_DEACTIVATE_CONSTRAINT = "Link to deactivate a constraint."
    private const val LINK_UPDATE_CONSTRAINT = "Link to update a constraint (rename, etc.)."

    private val CONSTRAINT_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_CONSTRAINT_ACTIVATE).description(LINK_ACTIVATE_CONSTRAINT).optional(),
            linkWithRel(LINK_CONSTRAINT_DEACTIVATE)
                .description(LINK_DEACTIVATE_CONSTRAINT)
                .optional(),
            linkWithRel(LINK_CONSTRAINT_UPDATE).description(LINK_UPDATE_CONSTRAINT))

    private val PATH_PARAMETER_PROJECT =
        pathParameters(parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"))

    private val field = ConstrainedFields(UpdateTaskConstraintResource::class.java)

    private val CONSTRAINT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            field.withPath("key").description("The constraint").type(JsonFieldType.STRING),
            field
                .withPath("active")
                .description("The constraint is active or not")
                .type(JsonFieldType.BOOLEAN),
            field
                .withPath("name")
                .description("Name of the custom constraint")
                .type(JsonFieldType.STRING))

    private val CONSTRAINT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("key").description("The constraint").type(JsonFieldType.STRING),
            fieldWithPath("active")
                .description("The constraint is active or not")
                .type(JsonFieldType.BOOLEAN),
            fieldWithPath("name")
                .description("Name of the custom constraint")
                .type(JsonFieldType.STRING),
            subsectionWithPath("_links").ignored())

    private val RESPONSE_FIELDS_LIST =
        responseFields(
                fieldWithPath("items[]").description("List of items").type(JsonFieldType.ARRAY),
                subsectionWithPath("_links").ignored().optional())
            .andWithPrefix("items[].", CONSTRAINT_RESPONSE_FIELD_DESCRIPTORS)
  }
}
