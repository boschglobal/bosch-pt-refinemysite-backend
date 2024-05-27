/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.legend

import com.bosch.pt.iot.smartsite.project.calendar.boundary.helper.CalendarMessageTranslationHelper
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend.CraftLegendCell
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend.LegendRow
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.legend.MilestoneLegendCell
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import java.text.Collator
import org.springframework.context.i18n.LocaleContextHolder.getLocale
import org.springframework.stereotype.Component

@Component
class LegendRowAssembler(
    private val calendarMessageTranslationHelper: CalendarMessageTranslationHelper
) {

  fun assemble(tasks: Collection<Task>, milestones: Collection<Milestone>): LegendRow {
    val locale = getLocale()

    val craftLegendCells =
        (tasks.map { it.projectCraft } + milestones.filter { it.craft != null }.map { it.craft })
            .distinct()
            .map { assembleCraftLegendCellModel(it!!) }
            .sortedWith(compareBy(Collator.getInstance(locale), CraftLegendCell::name))

    val milestoneLegendCells =
        milestones
            .asSequence()
            .map { it.type }
            .distinct()
            .filterNot { it == CRAFT }
            .map { assembleMilestoneLegendCellModel(it) }
            .sortedWith(compareBy(Collator.getInstance(locale), MilestoneLegendCell::name))
            .toList()

    return LegendRow(craftLegendCells, milestoneLegendCells)
  }

  private fun assembleCraftLegendCellModel(craft: ProjectCraft) =
      CraftLegendCell(craft.name, craft.color)

  private fun assembleMilestoneLegendCellModel(type: MilestoneTypeEnum) =
      when (type) {
        PROJECT ->
            MilestoneLegendCell(PROJECT, calendarMessageTranslationHelper.getProjectMilestoneName())
        INVESTOR ->
            MilestoneLegendCell(
                INVESTOR, calendarMessageTranslationHelper.getInvestorMilestoneName())
        else -> throw IllegalStateException("Invalid milestone type for legend")
      }
}
