/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.layout

import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildMilestone
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildProject
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import java.time.LocalDate
import java.util.UUID.randomUUID
import kotlin.Int.Companion.MIN_VALUE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MilestoneCellAssemblerTest {

  private val cut = MilestoneCellAssembler()

  @Test
  fun `verify creation of cell for milestones`() {
    val milestone = buildMilestone {
      it.date = DATE
      it.craft!!.color = ""
    }

    val milestoneCells = cut.assemble(listOf(milestone), emptyList())

    assertThat(milestoneCells).isNotNull
    assertThat(milestoneCells).hasSize(1)
    assertThat(milestoneCells[0].name).isEqualTo("milestone")
    assertThat(milestoneCells[0].craftColor).isEqualTo("")
    assertThat(milestoneCells[0].type).isEqualTo(PROJECT)
    assertThat(milestoneCells[0].position).isEqualTo(MIN_VALUE)
    assertThat(milestoneCells[0].date).isEqualTo(DATE)
  }

  @Test
  fun `verify creation of cell for milestones with craft`() {
    val milestone = buildMilestone {
      it.date = DATE
      it.type = CRAFT
      it.header = false
    }

    val milestoneCells = cut.assemble(listOf(milestone), emptyList())

    assertThat(milestoneCells).isNotNull
    assertThat(milestoneCells).hasSize(1)
    assertThat(milestoneCells[0].craftColor).isEqualTo("Black")
    assertThat(milestoneCells[0].type).isEqualTo(CRAFT)
  }

  @Test
  fun `verify creation of cell for milestones with position`() {
    val milestone =
        Milestone("milestone", INVESTOR, DATE, false, buildProject(), null, null, null, null, 1)
            .apply { identifier = MilestoneId() }

    val milestoneCells = cut.assemble(listOf(milestone), emptyList())

    assertThat(milestoneCells).isNotNull
    assertThat(milestoneCells).hasSize(1)
    assertThat(milestoneCells[0].type).isEqualTo(INVESTOR)
    assertThat(milestoneCells[0].position).isEqualTo(1)
  }

  @Test
  fun `verify creation of cell for milestones with criticality`() {
    val criticalMilestone = buildMilestone()
    val nonCriticalMilestone = buildMilestone()
    val milestoneRelation = buildMilestoneRelation(criticalMilestone.identifier)

    val milestoneCells =
        cut.assemble(listOf(criticalMilestone, nonCriticalMilestone), listOf(milestoneRelation))

    assertThat(milestoneCells).isNotNull
    assertThat(milestoneCells).hasSize(2)
    assertThat(milestoneCells[0].critical).isTrue
    assertThat(milestoneCells[1].critical).isFalse
  }

  companion object {
    private val DATE = LocalDate.of(2019, 10, 7)

    fun buildMilestoneRelation(milestoneIdentifier: MilestoneId) =
        Relation(
            FINISH_TO_START,
            RelationElement(milestoneIdentifier.identifier, MILESTONE),
            RelationElement(randomUUID(), TASK),
            true,
            buildProject())
  }
}
