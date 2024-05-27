/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.legend

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildMilestone
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildProjectCraft
import com.bosch.pt.iot.smartsite.project.calendar.util.CalendarBuilderUtility.buildTask
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import java.util.Locale
import java.util.Locale.FRANCE
import java.util.Locale.GERMANY
import java.util.Locale.UK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder.setLocale

@SmartSiteSpringBootTest
class LegendRowAssemblerTest {

  @Autowired private lateinit var cut: LegendRowAssembler

  @Test
  fun `verify creation of row with craft legends from a task`() {
    val task = buildTask {
      it.projectCraft.name = "ProjectCraft"
      it.projectCraft.color = "Black"
    }

    val legendRow = cut.assemble(listOf(task), emptyList())

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(1)
    assertThat(legendRow.milestones).hasSize(0)
    assertThat(legendRow.crafts[0].name).isEqualTo("ProjectCraft")
    assertThat(legendRow.crafts[0].color).isEqualTo("Black")
  }

  @Test
  fun `verify creation of row with craft legends from a milestone`() {
    val milestone = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft!!.name = "ProjectCraft"
      it.craft!!.color = "Black"
    }

    val legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(1)
    assertThat(legendRow.milestones).hasSize(0)
    assertThat(legendRow.crafts[0].name).isEqualTo("ProjectCraft")
    assertThat(legendRow.crafts[0].color).isEqualTo("Black")
  }

  @Test
  fun `verify creation of row with craft legends from a task and milestone`() {
    val task = buildTask {
      it.projectCraft.name = "ProjectCraft1"
      it.projectCraft.color = "Black1"
    }
    val milestone = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft!!.name = "ProjectCraft2"
      it.craft!!.color = "Black2"
    }

    val legendRow = cut.assemble(listOf(task), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(2)
    assertThat(legendRow.milestones).hasSize(0)
    assertThat(legendRow.crafts[0].name).isEqualTo("ProjectCraft1")
    assertThat(legendRow.crafts[0].color).isEqualTo("Black1")
    assertThat(legendRow.crafts[1].name).isEqualTo("ProjectCraft2")
    assertThat(legendRow.crafts[1].color).isEqualTo("Black2")
  }

  @Test
  fun `verify creation of row with craft legends from a task and milestone with no duplicates`() {
    val craft = buildProjectCraft {
      it.name = "ProjectCraft1"
      it.color = "Black1"
    }
    val task1 = buildTask { it.projectCraft = craft }
    val task2 = buildTask { it.projectCraft = craft }
    val milestone1 = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft = craft
    }
    val milestone2 = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft = craft
    }

    val legendRow = cut.assemble(listOf(task1, task2), listOf(milestone1, milestone2))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(1)
    assertThat(legendRow.milestones).hasSize(0)
    assertThat(legendRow.crafts[0].name).isEqualTo("ProjectCraft1")
    assertThat(legendRow.crafts[0].color).isEqualTo("Black1")
  }

  @Test
  fun `verify creation of row with craft legends ordered by name`() {
    val task1 = buildTask {
      it.projectCraft.name = "A"
      it.projectCraft.color = "Black1"
    }
    val task2 = buildTask {
      it.projectCraft.name = "C"
      it.projectCraft.color = "Black3"
    }
    val milestone1 = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft!!.name = "B"
      it.craft!!.color = "Black2"
    }
    val milestone2 = buildMilestone {
      it.type = CRAFT
      it.header = false
      it.craft!!.name = "D"
      it.craft!!.color = "Black4"
    }

    val legendRow = cut.assemble(listOf(task1, task2), listOf(milestone1, milestone2))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(4)
    assertThat(legendRow.milestones).hasSize(0)
    assertThat(legendRow.crafts[0].name).isEqualTo("A")
    assertThat(legendRow.crafts[0].color).isEqualTo("Black1")
    assertThat(legendRow.crafts[1].name).isEqualTo("B")
    assertThat(legendRow.crafts[1].color).isEqualTo("Black2")
    assertThat(legendRow.crafts[2].name).isEqualTo("C")
    assertThat(legendRow.crafts[2].color).isEqualTo("Black3")
    assertThat(legendRow.crafts[3].name).isEqualTo("D")
    assertThat(legendRow.crafts[3].color).isEqualTo("Black4")
  }

  @Test
  fun `verify creation of row with milestone legends for milestone project`() {
    val milestone = buildMilestone {
      it.type = PROJECT
      it.header = true
      it.craft = null
    }

    setLocale(UK)
    var legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Project")
    assertThat(legendRow.milestones[0].type).isEqualTo(PROJECT)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("project")

    setLocale(GERMANY)
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Projekt")
    assertThat(legendRow.milestones[0].type).isEqualTo(PROJECT)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("project")

    setLocale(Locale("es", "ES"))
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Proyecto")
    assertThat(legendRow.milestones[0].type).isEqualTo(PROJECT)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("project")

    setLocale(FRANCE)
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Projet")
    assertThat(legendRow.milestones[0].type).isEqualTo(PROJECT)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("project")

    setLocale(Locale("pt", "PT"))
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Projeto")
    assertThat(legendRow.milestones[0].type).isEqualTo(PROJECT)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("project")
  }

  @Test
  fun `verify creation of row with milestone legends for milestone investor`() {
    val milestone = buildMilestone {
      it.type = INVESTOR
      it.header = true
      it.craft = null
    }

    setLocale(UK)
    var legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Client")
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("investor")

    setLocale(GERMANY)
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Kunde")
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("investor")

    setLocale(Locale("es", "ES"))
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Cliente")
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("investor")

    setLocale(FRANCE)
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Client")
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("investor")

    setLocale(Locale("pt", "PT"))
    legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].name).isEqualTo("Cliente")
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[0].typeName).isEqualTo("investor")
  }

  @Test
  fun `verify creation of row with milestone legends with no duplicates`() {
    val milestone1 = buildMilestone {
      it.type = INVESTOR
      it.header = true
      it.craft = null
    }
    val milestone2 = buildMilestone {
      it.type = INVESTOR
      it.header = true
      it.craft = null
    }

    val legendRow = cut.assemble(emptyList(), listOf(milestone1, milestone2))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(1)
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
  }

  @Test
  fun `verify creation of row with milestone legends ordered by name`() {
    val milestone1 = buildMilestone {
      it.type = PROJECT
      it.header = true
      it.craft = null
    }
    val milestone2 = buildMilestone {
      it.type = INVESTOR
      it.header = true
      it.craft = null
    }

    val legendRow = cut.assemble(emptyList(), listOf(milestone1, milestone2))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(0)
    assertThat(legendRow.milestones).hasSize(2)
    assertThat(legendRow.milestones[0].type).isEqualTo(INVESTOR)
    assertThat(legendRow.milestones[1].type).isEqualTo(PROJECT)
  }

  @Test
  fun `verify creation of row with milestone legends ignore milestone of craft type`() {
    val milestone = buildMilestone {
      it.type = CRAFT
      it.header = true
      it.craft!!.name = "ProjectCraft"
      it.craft!!.color = "Black"
    }

    val legendRow = cut.assemble(emptyList(), listOf(milestone))

    assertThat(legendRow).isNotNull
    assertThat(legendRow.crafts).isNotNull
    assertThat(legendRow.milestones).isNotNull
    assertThat(legendRow.crafts).hasSize(1)
    assertThat(legendRow.milestones).hasSize(0)
  }
}
