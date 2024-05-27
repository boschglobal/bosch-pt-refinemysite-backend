/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.IdentifiableResource
import java.time.LocalDate
import java.util.Objects
import java.util.UUID

class CreateTaskScheduleBatchResource(val taskId: UUID, start: LocalDate?, end: LocalDate?) :
    CreateTaskScheduleResource(start, end), IdentifiableResource {

  override val id: UUID
    get() = taskId

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is CreateTaskScheduleBatchResource) {
      return false
    }
    return taskId == other.taskId
  }

  @ExcludeFromCodeCoverage override fun hashCode(): Int = Objects.hash(taskId)
}
