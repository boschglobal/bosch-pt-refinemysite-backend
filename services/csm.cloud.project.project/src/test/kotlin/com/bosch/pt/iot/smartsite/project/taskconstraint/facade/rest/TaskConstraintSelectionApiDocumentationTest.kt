/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController.Companion.CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.EQUIPMENT
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.EXTERNAL_FACTORS
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.INFORMATION
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.MATERIAL
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.hypermedia.LinksSnippet
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.payload.RequestFieldsSnippet
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.PathParametersSnippet
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class TaskConstraintSelectionApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val projectIdTaskIdParameterSnippet: PathParametersSnippet =
      pathParameters(
          parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"),
          parameterWithName(PATH_VARIABLE_TASK_ID)
              .description("ID of the task the selection belongs to"))

  private val taskConstraintResponseFields =
      listOf(
          fieldWithPath("key").description("The technical key of the constraint"),
          fieldWithPath("name").description("Display name of the constraint"))

  private val taskConstraintSelectionResponseFields =
      listOf(
          fieldWithPath("version").description("Version of the constraint selection"),
          fieldWithPath("taskId")
              .description("The identifier of the task the constraints belong to"),
          fieldWithPath("items").description("The list of selected constraints"))

  private val taskConstraintSelectionLinksSnippet: LinksSnippet =
      links(
          linkWithRel(TaskConstraintSelectionResource.LINK_CONSTRAINTS_UPDATE)
              .optional()
              .description("Link to update a constraint selection"))

  private val saveTaskConstraintSelectionRequestFieldsSnippet: RequestFieldsSnippet =
      requestFields(
          ConstrainedFields(UpdateTaskConstraintSelectionResource::class.java)
              .withPath("constraints")
              .description("Selection of constraints for a task")
              .type(JsonFieldType.ARRAY))

  private val taskConstraintSelectionResponseFieldsSnippet: ResponseFieldsSnippet =
      responseFields()
          .and(taskConstraintSelectionResponseFields)
          .andWithPrefix("items[].", taskConstraintResponseFields)
          .and(subsectionWithPath("_links").ignored())

  private val taskConstraintSelectionsResponseFieldsSnippet: ResponseFieldsSnippet =
      responseFields()
          .and(fieldWithPath("selections[]").description("List of constraint selections"))
          .andWithPrefix("selections[].", taskConstraintSelectionResponseFields)
          .andWithPrefix("selections[].items[].", taskConstraintResponseFields)
          .and(subsectionWithPath("_links").ignored().optional())
          .and(subsectionWithPath("selections[]._links").ignored())

  private val userTest by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }

  @BeforeEach
  fun setUp() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(userTest.identifier!!)
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document updating a constraint selection of a task`() {
    createSelection("task", randomUUID(), INFORMATION, EQUIPMENT)
    projectEventStoreUtils.reset()

    val updateTaskConstraintSelectionResource: UpdateTaskConstraintSelectionResource =
        buildSaveTaskSelectionResource(COMMON_UNDERSTANDING, EQUIPMENT, EXTERNAL_FACTORS)

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID),
                    task.project.identifier,
                    task.identifier),
                updateTaskConstraintSelectionResource,
                0L))
        .andExpectAll(
            status().isOk,
            jsonPath("$.version").value(1),
            jsonPath("$.items.length()").value(3),
            jsonPath("$.items[0].key").value(EQUIPMENT.name),
            jsonPath("$.items[1].key").value(EXTERNAL_FACTORS.name),
            jsonPath("$.items[2].key").value(COMMON_UNDERSTANDING.name),
        )
        .andDo(
            document(
                "task-constraints/document-update-selection-of-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectIdTaskIdParameterSnippet,
                saveTaskConstraintSelectionRequestFieldsSnippet,
                taskConstraintSelectionResponseFieldsSnippet,
                taskConstraintSelectionLinksSnippet,
            ))

    projectEventStoreUtils
        .verifyContainsAndGet(TaskActionSelectionEventAvro::class.java, UPDATED, 1)
        .first()
        .aggregate
        .also { aggregate ->
          validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate,
              repositories.findTaskConstraintSelectionByIdentifier(
                  getIdentifier("task-selection"))!!)
          assertThat(aggregate.task.identifier).isEqualTo(task.identifier.toString())
          assertThat(aggregate.actions)
              .hasSize(updateTaskConstraintSelectionResource.constraints.size)
          assertThat(aggregate.actions)
              .containsExactlyInAnyOrder(
                  *updateTaskConstraintSelectionResource.constraints
                      .map { TaskActionEnumAvro.valueOf(it.name) }
                      .sorted()
                      .toTypedArray())
        }
  }

  @Test
  fun `verify and document getting a constraint selection of a single task`() {
    createSelection("task", randomUUID(), COMMON_UNDERSTANDING, MATERIAL)
    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/{projectId}/tasks/{taskId}/constraints"),
                    task.project.identifier,
                    task.identifier)))
        .andExpectAll(
            status().isOk,
            jsonPath("$.items.length()").value(2),
            jsonPath("$.items[0].key").value(MATERIAL.name),
            jsonPath("$.items[1].key").value(COMMON_UNDERSTANDING.name),
        )
        .andDo(
            document(
                "task-constraints/document-get-constraint-selection-of-a-task",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                projectIdTaskIdParameterSnippet,
                taskConstraintSelectionResponseFieldsSnippet,
                taskConstraintSelectionLinksSnippet,
            ))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document getting constraint selections of multiple tasks`() {
    eventStreamGenerator.submitTask(asReference = "task2")
    createSelection(
        "task", "a7faec0c-2f89-4b60-8c8e-d4159564bfa4".toUUID(), COMMON_UNDERSTANDING, MATERIAL)
    createSelection("task2", "b7faec0c-2f89-4b60-8c8e-d4159564bfa4".toUUID(), EQUIPMENT)
    projectEventStoreUtils.reset()

    val batchRequestResource =
        BatchRequestResource(setOf(getIdentifier("task"), getIdentifier("task2")))

    mockMvc
        .perform(
            requestBuilder(
                post(
                        latestVersionOf("/projects/{projectId}/tasks/constraints"),
                        getIdentifier("project"))
                    .locale(Locale.ENGLISH)
                    .header(HttpHeaders.ACCEPT, MediaTypes.HAL_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(ObjectMapper().writeValueAsString(batchRequestResource))
                    .accept(MediaType.parseMediaType(MediaTypes.HAL_JSON_VALUE))
                    .param("identifierType", BatchRequestIdentifierType.TASK)))
        .andExpectAll(
            status().isOk,
            jsonPath("$.selections.length()").value(2),
            jsonPath("$.selections[0].items.length()").value(2),
            jsonPath("$.selections[0].items[0].key").value(MATERIAL.name),
            jsonPath("$.selections[0].items[1].key").value(COMMON_UNDERSTANDING.name),
            jsonPath("$.selections[1].items.length()").value(1),
            jsonPath("$.selections[1].items[0].key").value(EQUIPMENT.name),
        )
        .andDo(
            document(
                "task-constraints/document-get-constraint-selections-of-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeRequestParameter(
                    BatchRequestIdentifierType.TASK, BatchRequestIdentifierType.TASK),
                taskConstraintSelectionsResponseFieldsSnippet,
            ))

    projectEventStoreUtils.verifyEmpty()
  }

  private fun buildSaveTaskSelectionResource(
      vararg constraints: TaskConstraintEnum
  ): UpdateTaskConstraintSelectionResource =
      UpdateTaskConstraintSelectionResource(constraints.toSet())

  /** @param identifier needed if a reliable, fixed sorting is required, as it's sorted by id */
  private fun createSelection(
      taskReference: String,
      identifier: UUID,
      vararg constraints: TaskConstraintEnum,
  ): TaskConstraintSelection {
    eventStreamGenerator.submitTaskAction(asReference = "$taskReference-selection") {
      it.aggregateIdentifierBuilder.identifier = identifier.toString()
      it.task = getByReference(taskReference)
      it.actions = constraints.map { constraint -> TaskActionEnumAvro.valueOf(constraint.name) }
    }

    return repositories.findTaskConstraintSelectionByIdentifier(
        getIdentifier("$taskReference-selection"))!!
  }
}
