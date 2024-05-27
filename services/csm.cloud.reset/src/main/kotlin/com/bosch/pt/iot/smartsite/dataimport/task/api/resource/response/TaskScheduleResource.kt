/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class TaskScheduleResource(
    val slots: Collection<TaskScheduleSlotResource>? = null,
    val id: UUID? = null,
    val task: ResourceReference? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null
) : AuditableResource()
