/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.layout.MilestoneCell
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import kotlin.Int.Companion.MIN_VALUE
import org.springframework.stereotype.Component

@Component
class MilestoneCellAssembler {

  fun assemble(
      milestones: Collection<Milestone>,
      criticalMilestoneRelations: Collection<Relation>
  ): List<MilestoneCell> {
    val criticalMilestones = getMilestoneIdentifiersFromRelations(criticalMilestoneRelations)

    return milestones.map {
      MilestoneCell(
          it.name,
          it.craft?.color ?: NO_CRAFT_COLOR,
          it.type,
          it.position ?: NO_POSITION,
          it.date,
          criticalMilestones.contains(it.identifier))
    }
  }

  private fun getMilestoneIdentifiersFromRelations(
      criticalMilestoneRelations: Collection<Relation>
  ) =
      criticalMilestoneRelations
          .flatMap { listOf(it.source, it.target) }
          .filter { it.type == MILESTONE }
          .map { it.identifier.asMilestoneId() }
          .toSet()

  companion object {
    private const val NO_CRAFT_COLOR = ""
    private const val NO_POSITION = MIN_VALUE
  }
}
