/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.repository

import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment

interface TaskAttachmentRepositoryExtension {

  fun getByTaskIdsPartitioned(taskIds: List<Long>): List<TaskAttachment>

  fun deletePartitioned(ids: List<Long>)
}
