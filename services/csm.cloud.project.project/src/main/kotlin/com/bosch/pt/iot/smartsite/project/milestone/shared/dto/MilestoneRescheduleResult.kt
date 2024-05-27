/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.dto

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId

data class MilestoneRescheduleResult(
    /**
     * milestone identifiers that can be rescheduled successfully (or have already been rescheduled
     * successfully)
     */
    val successful: List<MilestoneId> = emptyList(),

    /**
     * milestone identifiers that cannot be rescheduled successfully (or have already been
     * rescheduled unsuccessfully)
     */
    val failed: List<MilestoneId> = emptyList(),
)
