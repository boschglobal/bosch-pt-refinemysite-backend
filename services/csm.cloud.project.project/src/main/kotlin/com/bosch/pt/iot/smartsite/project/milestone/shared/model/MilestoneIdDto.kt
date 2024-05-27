/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.model

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId

/** this class is meant to be used in a Spring Data dynamic projection (Spring Data DTO) */
data class MilestoneIdDto(val identifier: MilestoneId)
