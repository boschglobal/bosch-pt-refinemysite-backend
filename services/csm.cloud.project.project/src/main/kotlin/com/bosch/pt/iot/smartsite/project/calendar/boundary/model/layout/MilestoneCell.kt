/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout

import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import java.time.LocalDate

data class MilestoneCell(
    val name: String,
    val craftColor: String,
    val type: MilestoneTypeEnum,
    val position: Int,
    val date: LocalDate,
    val critical: Boolean = false,
) {
  val typeName: String = type.name.lowercase()
  val hasColor: Boolean = craftColor.isNotEmpty()
  var inverted: Boolean = false
}
