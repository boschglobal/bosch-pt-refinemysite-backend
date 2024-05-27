/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.project.model.MilestoneTypeEnum
import java.time.LocalDate
import java.util.UUID

class CreateMilestoneResource(
    val name: String? = null,
    val type: MilestoneTypeEnum? = null,
    val date: LocalDate? = null,
    val header: Boolean = false,
    val projectId: UUID? = null,
    val description: String? = null,
    val craftId: UUID? = null,
    val workAreaId: UUID? = null
)
