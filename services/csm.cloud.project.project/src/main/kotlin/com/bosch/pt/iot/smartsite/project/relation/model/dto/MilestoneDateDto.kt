/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.model.dto

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import java.time.LocalDate

data class MilestoneDateDto(
    val identifier: MilestoneId,
    val date: LocalDate,
)
