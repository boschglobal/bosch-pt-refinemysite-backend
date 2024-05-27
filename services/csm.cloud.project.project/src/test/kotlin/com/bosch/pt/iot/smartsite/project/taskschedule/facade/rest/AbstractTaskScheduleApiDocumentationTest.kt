/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleResource
import java.time.LocalDate.now
import java.util.Locale
import java.util.Locale.ENGLISH
import org.junit.jupiter.api.BeforeEach
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath

@EnableAllKafkaListeners
abstract class AbstractTaskScheduleApiDocumentationTest : AbstractApiDocumentationTestV2() {

  protected val saveTaskScheduleConstrainedFields =
      ConstrainedFields(UpdateTaskScheduleResource::class.java)

  protected val saveTaskScheduleRequestFieldsSnippet =
      requestFields(
          saveTaskScheduleConstrainedFields
              .withPath("start")
              .description("The start date of the task")
              .type("LocalDate"),
          saveTaskScheduleConstrainedFields
              .withPath("end")
              .description("The end date of the task")
              .type("LocalDate"),
          saveTaskScheduleConstrainedFields
              .withPath("slots")
              .description("The slots of the task schedule")
              .type(ARRAY)
              .optional())

  protected val updateBatchResourceConstrainedFields =
      ConstrainedFields(UpdateBatchRequestResource::class.java)

  protected val createTaskScheduleBatchResourceConstrainedFields =
      ConstrainedFields(CreateTaskScheduleBatchResource::class.java)

  protected val updateTaskScheduleBatchResourceConstrainedFields =
      ConstrainedFields(UpdateTaskScheduleBatchResource::class.java)

  protected val createTaskScheduleBatchResourcesRequestFieldsSnippet =
      requestFields(
              updateBatchResourceConstrainedFields
                  .withPath("items[]")
                  .description("List of references of the resource to be created in a batch")
                  .type(ARRAY))
          .andWithPrefix(
              "items[].",
              createTaskScheduleBatchResourceConstrainedFields
                  .withPath("id")
                  .description("Same as the taskId field (override)")
                  .type(STRING)
                  .optional(),
              createTaskScheduleBatchResourceConstrainedFields
                  .withPath("taskId")
                  .description("ID of the task the schedule will be created in the batch request")
                  .type(STRING),
              createTaskScheduleBatchResourceConstrainedFields
                  .withPath("start")
                  .description("The start date of the task")
                  .type("LocalDate"),
              createTaskScheduleBatchResourceConstrainedFields
                  .withPath("end")
                  .description("The end date of the task")
                  .type("LocalDate"))

  protected val updateTaskScheduleBatchResourcesRequestFieldsSnippet =
      requestFields(
              updateBatchResourceConstrainedFields
                  .withPath("items[]")
                  .description(
                      "List of versioned references of the resource to be updated in a batch")
                  .type(ARRAY))
          .andWithPrefix(
              "items[].",
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("id")
                  .description("Same as the taskId field (override)")
                  .type(STRING)
                  .optional(),
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("version")
                  .description("Version of the resource to be updated in a batch")
                  .type(NUMBER),
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("taskId")
                  .description("ID of the task the schedule will be created in the batch request")
                  .type(STRING),
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("start")
                  .description("The start date of the task")
                  .type("LocalDate"),
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("end")
                  .description("The end date of the task")
                  .type("LocalDate"),
              updateTaskScheduleBatchResourceConstrainedFields
                  .withPath("slots")
                  .description("The slots of the task schedule")
                  .type(ARRAY))

  protected val taskScheduleWithoutSlotsResponseFields =
      responseFields(
          *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
          fieldWithPath("start").description("The start date of the task"),
          fieldWithPath("end").description("The end date of the task"),
          fieldWithPath("slots[]").description("The slots of the task schedule"),
          fieldWithPath("task.displayName")
              .description("The display name of the task the schedule belongs to"),
          fieldWithPath("task.id").description("ID of the task the schedule belongs to"),
          subsectionWithPath("_links").ignored(),
          subsectionWithPath("_embedded.dayCards")
              .optional()
              .description("Embedded resources")
              .type(OBJECT))

  protected val taskScheduleWithoutSlotsListResponseFields =
      responseFields(
          fieldWithPath("taskSchedules[].version").description("Version of the task schedule"),
          fieldWithPath("taskSchedules[].id").description("ID of the task schedule"),
          fieldWithPath("taskSchedules[].start").description("The start date of the task"),
          fieldWithPath("taskSchedules[].end").description("The end date of the task"),
          fieldWithPath("taskSchedules[].slots[]").description("The slots of the task schedule"),
          fieldWithPath("taskSchedules[].task.displayName")
              .description("The display name of the task the schedule belongs to"),
          fieldWithPath("taskSchedules[].task.id")
              .description("ID of the task the schedule belongs to"),
          fieldWithPath("taskSchedules[].createdBy.displayName")
              .description("Name of the creator of the task schedule"),
          fieldWithPath("taskSchedules[].createdBy.id")
              .description("ID of the creator of the task schedule"),
          fieldWithPath("taskSchedules[].createdDate")
              .description("Date of the task schedule creation"),
          fieldWithPath("taskSchedules[].lastModifiedBy.displayName")
              .description("Name of the user of last modification"),
          fieldWithPath("taskSchedules[].lastModifiedBy.id")
              .description("ID of the user of last modification"),
          fieldWithPath("taskSchedules[].lastModifiedDate")
              .description("Date of the last modification"),
          subsectionWithPath("_links").optional().ignored(),
          subsectionWithPath("taskSchedules[]._links").optional().ignored(),
          subsectionWithPath("taskSchedules[]._embedded.dayCards")
              .optional()
              .description("Embedded resources")
              .type(OBJECT))

  protected val taskScheduleResponseFields =
      responseFields(
          *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
          fieldWithPath("start").description("The start date of the task"),
          fieldWithPath("end").description("The end date of the task"),
          fieldWithPath("slots[]").description("The slots of the task schedule"),
          fieldWithPath("slots[].dayCard.displayName")
              .description("Title of the day card of the slot")
              .type(STRING),
          fieldWithPath("slots[].dayCard.id")
              .description("ID of the day card of the slot")
              .type(STRING),
          fieldWithPath("slots[].date")
              .description("The date of the day card of the slot")
              .type(STRING),
          fieldWithPath("task.displayName")
              .description("The display name of the task the schedule belongs to"),
          fieldWithPath("task.id").description("ID of the task the schedule belongs to"),
          subsectionWithPath("_links").ignored(),
          subsectionWithPath("_embedded.dayCards")
              .optional()
              .description("Embedded resources")
              .type(OBJECT))

  protected val searchTaskSchedulesResponseFields =
      responseFields(
          fieldWithPath("taskSchedules[].version").description("Version of the task schedule"),
          fieldWithPath("taskSchedules[].id").description("ID of the task schedule"),
          fieldWithPath("taskSchedules[].start").description("The start date of the task"),
          fieldWithPath("taskSchedules[].end").description("The end date of the task"),
          fieldWithPath("taskSchedules[].slots[]").description("The slots of the task schedule"),
          fieldWithPath("taskSchedules[].slots[].dayCard.displayName")
              .description("Title of the day card of the slot")
              .type(STRING),
          fieldWithPath("taskSchedules[].slots[].dayCard.id")
              .description("ID of the day card of the slot")
              .type(STRING),
          fieldWithPath("taskSchedules[].slots[].date")
              .description("The date of the day card of the slot")
              .type(STRING),
          fieldWithPath("taskSchedules[].task.displayName")
              .description("The display name of the task the schedule belongs to"),
          fieldWithPath("taskSchedules[].task.id")
              .description("ID of the task the schedule belongs to"),
          fieldWithPath("taskSchedules[].createdBy.displayName")
              .description("Name of the creator of the task schedule"),
          fieldWithPath("taskSchedules[].createdBy.id")
              .description("ID of the creator of the task schedule"),
          fieldWithPath("taskSchedules[].createdDate")
              .description("Date of the task schedule creation"),
          fieldWithPath("taskSchedules[].lastModifiedBy.displayName")
              .description("Name of the user of last modification"),
          fieldWithPath("taskSchedules[].lastModifiedBy.id")
              .description("ID of the user of last modification"),
          fieldWithPath("taskSchedules[].lastModifiedDate")
              .description("Date of the last modification"),
          subsectionWithPath("_links").optional().ignored(),
          subsectionWithPath("taskSchedules[]._links").ignored(),
          subsectionWithPath("taskSchedules[]._embedded.dayCards")
              .optional()
              .description("Embedded resources")
              .type(OBJECT))

  protected val startDate = now()

  protected val taskId by lazy { getIdentifier("task") }

  protected val userTest by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(userTest.identifier!!)
    projectEventStoreUtils.reset()
  }

  companion object {
    const val DESCRIPTION_LINK_UPDATE_TASKSCHEDULE =
        "Link to <<api-guide-project-context-tasks.adoc#resources-update-task-schedule,update the task schedule>>."
    const val ACCEPT_LANGUAGE_VALUE_EN = "en"
    const val DEFAULT_LANGUAGE = "en"
    val DEFAULT_LOCALE: Locale = ENGLISH
  }
}
