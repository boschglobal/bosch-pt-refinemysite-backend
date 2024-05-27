/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.model.TaskAttachment
import java.util.UUID

interface TaskAttachmentRepositoryExtension {

  fun findTaskAttachments(projectIdentifier: UUID): List<TaskAttachment>

  fun deleteTaskAttachment(identifier: UUID, projectIdentifier: UUID)
}
