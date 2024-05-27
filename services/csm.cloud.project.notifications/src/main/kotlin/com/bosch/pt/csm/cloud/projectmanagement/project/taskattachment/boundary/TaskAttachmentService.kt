/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.model.TaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository.TaskAttachmentRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskAttachmentService(private val taskAttachmentRepository: TaskAttachmentRepository) {

  @Trace
  fun save(taskAttachment: TaskAttachment): TaskAttachment =
      taskAttachmentRepository.save(taskAttachment)

  @Trace
  fun deleteTaskAttachment(taskAttachmentIdentifier: UUID, projectIdentifier: UUID) =
      taskAttachmentRepository.deleteTaskAttachment(taskAttachmentIdentifier, projectIdentifier)
}
