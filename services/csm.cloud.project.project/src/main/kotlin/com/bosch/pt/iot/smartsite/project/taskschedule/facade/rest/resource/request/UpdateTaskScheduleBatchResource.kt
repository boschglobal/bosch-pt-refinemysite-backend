/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.IdentifiableResource
import java.time.LocalDate
import java.util.UUID

class UpdateTaskScheduleBatchResource(
    val version: Long,
    val taskId: UUID,
    override val start: LocalDate?,
    override val end: LocalDate?,
    override val slots: List<TaskScheduleSlotDto>
) : UpdateTaskScheduleResource(start, end, slots), IdentifiableResource {

  override val id: UUID
    get() = taskId
}
