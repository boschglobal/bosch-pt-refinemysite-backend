/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.tasksearch

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_SEND
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_SEARCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.IanaLinkRelations.NEXT
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TaskSearchApiDocumentationTest : AbstractTaskApiDocumentationTest() {

  @BeforeEach
  fun initializeTestData() {
    eventStreamGenerator
        .submitWorkArea()
        .submitWorkAreaList()
        .submitTask("task-1") { it.status = DRAFT }
        .submitTaskSchedule()
        .submitTask("task-2") { it.status = DRAFT }
        .submitTaskSchedule()
        .submitTask("task-1", eventType = SENT) {
          it.status = OPEN
          it.assignee = getByReference("fm-participant")
        }
        .submitTask("task-2", eventType = SENT) {
          it.status = OPEN
          it.assignee = getByReference("fm-participant")
          it.workarea = getByReference("workArea")
        }

    setAuthentication("csm-user")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document search tasks`() {
    val filterTaskListResource =
        FilterTaskListResource(
            workAreaIds =
                listOf(
                    WorkAreaIdOrEmpty(getIdentifier("workArea").asWorkAreaId()),
                    WorkAreaIdOrEmpty()))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(TASKS_SEARCH_ENDPOINT), getIdentifier("project").asProjectId())
                    .param("sort", "end,start,name,asc")
                    .param("size", "1")
                    .param("page", "0"),
                filterTaskListResource))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.tasks.length()").value(1),
            jsonPath("$.tasks[0].name").isNotEmpty,
            jsonPath("$.tasks[0].location").isNotEmpty,
            jsonPath("$.tasks[0].projectCraft.id").isNotEmpty,
            jsonPath("$.tasks[0].projectCraft.name").isNotEmpty,
            jsonPath("$.tasks[0].assignee.id").isNotEmpty,
            jsonPath("$.tasks[0].assignee.displayName").isNotEmpty,
        )
        .andDo(
            document(
                "tasks/document-get-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(parameterWithName("projectId").description("The id of the project")),
                queryParameters(
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
                requestFields(buildSearchTasksRequestFieldDescriptorsV2()),
                links(
                    linkWithRel(LINK_ASSIGN).description(LINK_ASSIGNED_FOREMAN_DESCRIPTION),
                    linkWithRel(LINK_SEND).description(LINK_SEND_DESCRIPTION),
                    linkWithRel(LINK_TASK_CREATE).description(LINK_TASK_CREATE_DESCRIPTION),
                    linkWithRel(NEXT.value()).description(LINK_NEXT_DESCRIPTION)),
                PAGE_TASKS_RESPONSE_FIELDS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify task search succeeds with empty filter`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(TASKS_SEARCH_ENDPOINT), getIdentifier("project").asProjectId())
                    .param("sort", "end,start,name,asc")
                    .param("size", "1")
                    .param("page", "0"),
                FilterTaskListResource()))
        .andExpect(status().isOk)

    projectEventStoreUtils.verifyEmpty()
  }
}
