/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.CLOSED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchApiDocumentationTest.Companion.buildSearchMilestonesRequestFields
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobContext
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType.PROJECT_RESCHEDULE
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.RescheduleController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request.RescheduleResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.request.RescheduleResource.CriteriaResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest.Companion.buildSearchTasksRequestFieldDescriptorsV2
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import org.apache.avro.specific.SpecificRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class RescheduleApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val project by lazy { repositories.findProject(projectIdentifier)!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "closedTask") {
          it.assignee = getByReference("participant")
          it.status = CLOSED
        }
        .submitTaskSchedule(asReference = "closedTaskSchedule")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
    commandSendingService.clearRecords()
  }

  @Test
  fun `verify and document validate reschedule`() {
    val milestones = FilterMilestoneListResource()
    val tasks = FilterTaskListResource()
    val resource =
        RescheduleResource(
            shiftDays = 2L,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            criteria = CriteriaResource(milestones, tasks))

    this.mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/{projectId}/reschedule/validate"),
                    projectIdentifier),
                resource))
        .andExpectAll(
            status().isOk,
            jsonPath("$.successful").exists(),
            jsonPath("$.successful.milestones.length()").value(1),
            jsonPath("$.successful.tasks.length()").value(1),
            jsonPath("$.successful.tasks[0]").value(getIdentifier("task").toString()),
            jsonPath("$.successful.milestones[0]").value(getIdentifier("milestone").toString()),
            jsonPath("$.failed").exists(),
            jsonPath("$.failed.milestones.length()").value(0),
            jsonPath("$.failed.tasks.length()").value(1),
            jsonPath("$.failed.tasks[0]").value(getIdentifier("closedTask").toString()),
        )
        .andDo(
            document(
                "reschedule/document-validation-reschedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(
                    RESCHEDULE_REQUEST_FIELD_DESCRIPTORS +
                        buildSearchMilestonesRequestFields("criteria.milestones") +
                        buildSearchTasksRequestFieldDescriptorsV2("criteria.tasks")),
                responseFields(RESCHEDULE_RESPONSE_FIELD_DESCRIPTORS)))

    // assert that no event was sent
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document reschedule`() {
    val milestones = FilterMilestoneListResource()
    val tasks = FilterTaskListResource()
    val resource =
        RescheduleResource(
            shiftDays = 2L,
            useTaskCriteria = true,
            useMilestoneCriteria = true,
            criteria = CriteriaResource(milestones, tasks))

    this.mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/{projectId}/reschedule"), projectIdentifier),
                resource))
        .andExpectAll(status().isAccepted, jsonPath("$.id").exists())
        .andDo(
            document(
                "reschedule/document-job-reschedule",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(
                    RESCHEDULE_REQUEST_FIELD_DESCRIPTORS +
                        buildSearchMilestonesRequestFields("criteria.milestones") +
                        buildSearchTasksRequestFieldDescriptorsV2("criteria.tasks")),
                responseFields(RESCHEDULE_JOB_RESPONSE_FIELD_DESCRIPTORS)))

    // check that no event is sent
    projectEventStoreUtils.verifyEmpty()

    // check that correct job command is sent
    with(commandSendingService.capturedRecords) {
      assertThat(this).hasSize(1)
      with(this[0].value) {
        assertJobTypeAndIdentifier(this)
        assertContext(this)
        assertRescheduleCommand(this)
      }
    }
  }

  private fun assertJobTypeAndIdentifier(message: SpecificRecord) {
    assertThat(message).isInstanceOf(EnqueueJobCommandAvro::class.java)
    (message as EnqueueJobCommandAvro).run {
      assertThat(jobType).isEqualTo(PROJECT_RESCHEDULE.name)
      assertThat(aggregateIdentifier.type).isEqualTo("JOB")
    }
  }

  private fun assertContext(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedContext.run {
      RescheduleJobContext(ResourceReference.from(project))
          .let { jobJsonSerializer.serialize(it) }
          .run {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun assertRescheduleCommand(message: SpecificRecord) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.run {
      RescheduleCommand(
              shiftDays = 2L,
              useTaskCriteria = true,
              useMilestoneCriteria = true,
              taskCriteria = SearchTasksDto(projectIdentifier = projectIdentifier),
              milestoneCriteria = SearchMilestonesDto(projectIdentifier = projectIdentifier),
              projectIdentifier = projectIdentifier)
          .let { jobJsonSerializer.serialize(it) }
          .run {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  companion object {

    private val PROJECT_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"))

    private var RESCHEDULE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(RescheduleResource::class.java)
                .withPath("shiftDays")
                .description("The number of days to move the tasks and milestones")
                .type(NUMBER),
            ConstrainedFields(RescheduleResource::class.java)
                .withPath("useTaskCriteria")
                .description("Boolean indicating if the task filter criteria should be applied")
                .type(BOOLEAN),
            ConstrainedFields(RescheduleResource::class.java)
                .withPath("useMilestoneCriteria")
                .description(
                    "Boolean indicating if the milestone filter criteria should be applied")
                .type(BOOLEAN),
            ConstrainedFields(RescheduleResource::class.java)
                .withPath("criteria")
                .description(
                    "The criteria used for filtering the elements that are going to be moved")
                .type(OBJECT),
            ConstrainedFields(CriteriaResource::class.java)
                .withPath("criteria.milestones")
                .description("The criteria used for filtering the milestones.")
                .type(OBJECT),
            ConstrainedFields(CriteriaResource::class.java)
                .withPath("criteria.tasks")
                .description("The criteria used for filtering the tasks.")
                .type(OBJECT))

    private val RESCHEDULE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("successful.milestones")
                .description("List of milestone identifiers that are moved")
                .type(ARRAY),
            fieldWithPath("successful.tasks")
                .description("List of task identifiers that are moved")
                .type(ARRAY),
            fieldWithPath("failed.milestones")
                .description("List of milestone identifiers that are not moved")
                .type(ARRAY),
            fieldWithPath("failed.tasks")
                .description("List of task identifiers that are not moved")
                .type(ARRAY))

    private val RESCHEDULE_JOB_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id")
                .description("The ID of the async import job")
                .type(JsonFieldType.STRING))
  }
}
