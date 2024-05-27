/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.query

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

class TaskQueryServiceAuthorizationIntegrationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: TaskQueryService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify viewing tasks is authorized for`(userType: UserTypeAccess) {
    for (findFunctionToTest in allFindTaskFunctionsOfTaskSearchService) {
      checkAccessWith(userType) { assertThat(findFunctionToTest(cut, taskUnassigned)).isNotNull() }
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify viewing a task is denied for non-existing task or project for`(
      userType: UserTypeAccess
  ) {
    for (findFunctionToTest in allFindTaskFunctionsOfTaskSearchService) {
      checkAccessWith(userType) { assertThat(findFunctionToTest(cut, null)).isNotNull() }
    }
  }

  private val allFindTaskFunctionsOfTaskSearchService by lazy {
    listOf<FindTaskFunctionToTest>(
        { service, task ->
          service.findTasksWithDetailsForFilters(
              SearchTasksDto(projectIdentifier = task?.project?.identifier ?: ProjectId()),
              Pageable.ofSize(10))
        },
        { service, task -> service.findTask(task?.identifier ?: TaskId()) },
        { service, task -> service.findTasks(listOf(task?.identifier ?: TaskId())) },
        { service, task ->
          service.findTasks(task?.project?.identifier ?: ProjectId(), true, Pageable.ofSize(10))
        },
        { service, task -> service.findTaskWithDetails(task?.identifier ?: TaskId()) },
        { service, task -> service.findTasksWithDetails(listOf(task?.identifier ?: TaskId())) },
        { service, task ->
          service.findBatch(
              setOf(task?.identifier ?: TaskId()), task?.project?.identifier ?: ProjectId())
        },
    )
  }
}

typealias FindTaskFunctionToTest = (taskSearchService: TaskQueryService, task: Task?) -> Any?
