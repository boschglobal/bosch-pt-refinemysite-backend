/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend

import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum

data class LegendRow(val crafts: List<CraftLegendCell>, val milestones: List<MilestoneLegendCell>)

data class CraftLegendCell(val name: String, val color: String)

data class MilestoneLegendCell(val type: MilestoneTypeEnum, val name: String) {
  val typeName = type.name.lowercase()
}
