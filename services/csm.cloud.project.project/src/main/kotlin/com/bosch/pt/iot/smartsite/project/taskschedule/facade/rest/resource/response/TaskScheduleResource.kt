/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class TaskScheduleResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val start: LocalDate?,
    val end: LocalDate?,
    val task: ResourceReference,
    val slots: Collection<TaskScheduleSlotResource>?
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_UPDATE_TASKSCHEDULE = "update"
    const val LINK_CREATE_DAYCARD = "add"
    const val EMBEDDED_DAYCARDS_SCHEDULE = "dayCards"
  }
}
