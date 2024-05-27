/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import java.util.Date
import java.util.UUID

data class TaskResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val project: ResourceReference,
    val name: String,
    val description: String?,
    val company: ResourceReference?,
    val location: String?,
    val projectCraft: ProjectCraftResource,
    val workArea: ResourceReference?,
    val assignee: ResourceReferenceWithPicture?,
    val creator: ResourceReferenceWithPicture?,
    val status: TaskStatusEnum,
    val assigned: Boolean,
    val editDate: Date?,
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_ASSIGN = "assign"
    const val LINK_UNASSIGN = "unassign"
    const val LINK_SEND = "send"
    const val LINK_START = "start"
    const val LINK_CLOSE = "close"
    const val LINK_RESET = "reset"
    const val LINK_ACCEPT = "accept"
    const val LINK_DELETE = "delete"
    const val LINK_TASK_CREATE = "create"
    const val LINK_TASK_UPDATE = "update"
    const val LINK_TOPIC_CREATE = "createTopic"
    const val LINK_TOPIC = "topic"
    const val LINK_TASK_SCHEDULE = "schedule"
    const val LINK_CONSTRAINTS = "constraints"
    const val LINK_CONSTRAINTS_UPDATE = "updateConstraints"
    const val EMBEDDED_TASK_ATTACHMENTS = "attachments"
    const val EMBEDDED_TASK_STATISTICS = "statistics"
    const val EMBEDDED_TASK_SCHEDULE = "schedule"
    const val EMBEDDED_TASK_CONSTRAINTS = "constraints"
  }
}
