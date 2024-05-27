/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskListResource.Companion.LINK_SEND
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASKS_BATCH_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASKS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Component
open class TaskListResourceFactory(
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val factoryHelper: TaskResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  open fun build(
      tasks: Page<Task>,
      pageable: Pageable,
      projectId: ProjectId?,
      showBasedOnRole: Boolean?
  ): TaskListResource {
    val taskResources: Collection<TaskResource> = factoryHelper.build(tasks.content, false)

    return TaskListResource(
            taskResources, tasks.number, tasks.size, tasks.totalPages, tasks.totalElements)
        .apply { addLinks(projectId) }
  }

  @PageLinks
  open fun build(tasks: Page<Task>, pageable: Pageable, projectId: ProjectId?): TaskListResource {
    val taskResources: Collection<TaskResource> = factoryHelper.build(tasks.content, false)

    return TaskListResource(
            taskResources, tasks.number, tasks.size, tasks.totalPages, tasks.totalElements)
        .apply { addLinks(projectId) }
  }

  open fun TaskListResource.addLinks(projectId: ProjectId?) {
    addIf(
        projectId != null &&
            projectAuthorizationComponent.hasAssignPermissionOnProject(projectId)) {
          linkFactory.linkTo(ASSIGN_TASKS_BATCH_ENDPOINT).withRel(LINK_ASSIGN)
        }

    addIf(
        projectId != null && projectAuthorizationComponent.hasOpenPermissionOnProject(projectId)) {
          linkFactory.linkTo(SEND_TASK_BY_TASK_ID_ENDPOINT).withRel(LINK_SEND)
        }

    addIf(
        projectId != null &&
            projectAuthorizationComponent.hasCreateTaskPermissionOnProject(projectId)) {
          linkFactory.linkTo(TASKS_ENDPOINT).withRel(LINK_TASK_CREATE)
        }
  }
}
