/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.DateBasedImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTaskResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.dataimport.task.model.Task
import com.bosch.pt.iot.smartsite.dataimport.task.rest.TaskRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.project
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.projectcraft
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.projectparticipant
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.workarea
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskImportService(
    private val taskRestClient: TaskRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : DateBasedImportService<Task> {

  override fun importData(data: Task) = importData(data, LocalDate.now())

  override fun importData(data: Task, rootDate: LocalDate) {
    authenticationService.selectUser(data.createWithUserId)
    val taskId = UUID.randomUUID()
    call { taskRestClient.create(taskId, map(data)) }

    idRepository.store(TypedId.typedId(ResourceTypeEnum.task, data.id), taskId)

    if (data.start != null || data.end != null) {
      call { taskRestClient.createSchedule(taskId, mapSchedule(data, rootDate)) }
    }

    // Update task status
    if (STARTED == data.status) {
      call { taskRestClient.start(taskId) }
    } else if (CLOSED == data.status) {
      call { taskRestClient.start(taskId) }
      call { taskRestClient.close(taskId) }
    }
  }

  @Suppress("ThrowsCount")
  private fun map(task: Task): CreateTaskResource {
    val projectId = idRepository[TypedId.typedId(project, task.projectId)]!!
    val assigneeId =
        task.assigneeId?.let {
          idRepository[TypedId.typedId(projectparticipant, it)]
              ?: throw IllegalStateException("Assignee with id ${task.assigneeId} not found")
        }
    val projectCraftId =
        task.projectCraftId.let { idRepository[TypedId.typedId(projectcraft, it)] }
            ?: throw IllegalStateException("Project craft with id ${task.projectCraftId} not found")
    val workAreaId =
        task.workAreaId?.let {
          idRepository[TypedId.typedId(workarea, it)]
              ?: throw IllegalStateException("Work area with id ${task.workAreaId} not found")
        }
    val status = if (DRAFT == task.status) DRAFT else OPEN

    return CreateTaskResource(
        projectId,
        task.name,
        task.description,
        task.location,
        status,
        assigneeId,
        projectCraftId,
        workAreaId)
  }

  private fun mapSchedule(task: Task, rootDate: LocalDate): CreateTaskScheduleResource =
      CreateTaskScheduleResource(
          rootDate.plusDays((task.start ?: 0).toLong()),
          rootDate.plusDays((task.end ?: 0).toLong()))
}
