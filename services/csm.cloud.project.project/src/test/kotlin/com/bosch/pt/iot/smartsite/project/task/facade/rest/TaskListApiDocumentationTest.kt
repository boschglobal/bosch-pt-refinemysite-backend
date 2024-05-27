/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_SEND
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_BY_PROJECT_ID_ENDPOINT
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskListApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitWorkArea()
        .submitWorkAreaList()
        .submitTask("task-1") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTaskSchedule()
        .submitTask("task-2") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTaskSchedule()
        .submitTask("task-1", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-2", eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = getByReference("fm-participant")
          it.workarea = getByReference("workArea")
        }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document list tasks`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(TASKS_BY_PROJECT_ID_ENDPOINT), getIdentifier("project"))
                    .param("size", "1")
                    .param("page", "0")
                    .param("showBasedOnRole", "true")))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.tasks.length()").value(1),
            jsonPath("$.tasks[0].name").isNotEmpty,
            jsonPath("$.tasks[0].location").isNotEmpty,
            jsonPath("$.tasks[0].projectCraft.id").exists(),
            jsonPath("$.tasks[0].projectCraft.name").isNotEmpty,
            jsonPath("$.tasks[0].assignee.displayName").isNotEmpty,
            jsonPath("$.tasks[0].assignee.id").isNotEmpty,
        )
        .andDo(
            document(
                "tasks/document-list-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("showBasedOnRole")
                        .optional()
                        .description(
                            "If set, tasks are filtered according to current users participant role. " +
                                "(CSM = all tasks, " +
                                "CR = tasks assigned to users participant company, " +
                                "FM = tasks assigned to current users participant"),
                    parameterWithName("sort")
                        .optional()
                        .description(
                            "List of task attributes separated by commas followed by sorting order ASC/DESC " +
                                "(Optional)"),
                    parameterWithName("page")
                        .optional()
                        .description("Number of the requested page, defaults to 0 (Optional)"),
                    parameterWithName("size")
                        .optional()
                        .description(
                            "Size of the requested page, defaults to 20, maximum is 100 (Optional)")),
                links(
                    linkWithRel(LINK_ASSIGN).description(LINK_ASSIGNED_FOREMAN_DESCRIPTION),
                    linkWithRel(LINK_SEND).description(LINK_SEND_DESCRIPTION),
                    linkWithRel(LINK_TASK_CREATE).description(LINK_TASK_CREATE_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION)),
                PAGE_TASKS_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyEmpty()
  }
}
