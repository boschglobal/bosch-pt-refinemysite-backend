/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.command.service

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskschedule.command.service.TaskScheduleDeleteService
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicDeleteService
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskDeleteService(
    private val taskRepository: TaskRepository,
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val topicDeleteService: TopicDeleteService,
    private val taskAttachmentService: TaskAttachmentService,
    private val taskScheduleDeleteService: TaskScheduleDeleteService,
    private val blobStoreService: BlobStoreService,
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  open fun deletePartitioned(projectId: Long) {
    val taskIds = taskRepository.findIdsByProjectId(projectId)
    taskRepository.findIdentifiersOfTasksWithAttachmentsByProjectId(projectId).forEach {
        taskId: TaskId ->
      blobStoreService.deleteImagesInDirectory(taskId.toString())
    }

    taskConstraintSelectionService.deletePartitioned(taskIds)
    topicDeleteService.deletePartitioned(taskIds)
    taskAttachmentService.deletePartitioned(taskIds)
    taskScheduleDeleteService.deletePartitioned(taskIds)
    taskRepository.deletePartitioned(taskIds)
  }
}
