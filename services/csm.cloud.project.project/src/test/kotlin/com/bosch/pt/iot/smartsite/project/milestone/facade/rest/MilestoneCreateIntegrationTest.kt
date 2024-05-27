/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_WORK_AREA_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch.CreateMilestoneBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.CreateMilestoneResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class MilestoneCreateIntegrationTest : AbstractMilestoneIntegrationTest() {

  @Autowired lateinit var cut: MilestoneController

  @Autowired lateinit var createMilestoneBatchCommandHandler: CreateMilestoneBatchCommandHandler

  @Autowired lateinit var milestoneSearchController: MilestoneSearchController

  private val existingMilestone by lazy {
    repositories.findMilestoneWithDetails(getIdentifier("milestone").asMilestoneId())
  }

  @Test
  fun `verify create craft milestone fails without craft`() {
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = getIdentifier("project").asProjectId(),
            type = CRAFT,
            name = "Test",
            date = LocalDate.now(),
            header = true)

    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.create(createMilestoneResource = createMilestoneResource)
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify craft is ignored when creating project milestone`() {
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = getIdentifier("project").asProjectId(),
            type = PROJECT,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            craftId = getIdentifier("projectCraft").asProjectCraftId())

    val response = cut.create(createMilestoneResource = createMilestoneResource)

    assertThat(response.body!!.craft).isNull()
  }

  @Test
  fun `verify craft is ignored when creating investor milestone`() {
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = getIdentifier("project").asProjectId(),
            type = INVESTOR,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            craftId = getIdentifier("projectCraft").asProjectCraftId())

    val response = cut.create(createMilestoneResource = createMilestoneResource)

    assertThat(response.body!!.craft).isNull()
  }

  @Test
  fun `verify work area is ignored when creating header milestone`() {
    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = getIdentifier("project").asProjectId(),
            type = INVESTOR,
            name = "Test",
            date = LocalDate.now(),
            header = true,
            workAreaId = getIdentifier("workArea").asWorkAreaId())

    val response = cut.create(createMilestoneResource = createMilestoneResource)

    assertThat(response.body!!.workArea).isNull()
  }

  @Test
  fun `verify milestone added at defined position`() {
    val projectIdentifier = getIdentifier("project").asProjectId()

    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = projectIdentifier,
            type = INVESTOR,
            name = "New Milestone",
            date = existingMilestone.date,
            header = existingMilestone.header,
            workAreaId = existingMilestone.workArea?.identifier,
            position = 1)

    val createResult = cut.create(createMilestoneResource = createMilestoneResource)

    milestoneSearchController
        .search(projectIdentifier, defaultFilter, DEFAULT_SORTING)
        .body!!
        .items
        .also { items ->
          assertThat(items).hasSize(2)
          assertThat(items[0].name).isEqualTo(existingMilestone.name)
          assertThat(items[0].id).isEqualTo(existingMilestone.identifier.toUuid())
          assertThat(items[1].name).isEqualTo(createResult.body!!.name)
          assertThat(items[1].id).isEqualTo(createResult.body!!.id)
        }
  }

  @Test
  fun `verify milestone added at default position (top of list)`() {
    val projectIdentifier = getIdentifier("project").asProjectId()

    val createMilestoneResource =
        CreateMilestoneResource(
            projectId = projectIdentifier,
            type = INVESTOR,
            name = "New Milestone",
            date = existingMilestone.date,
            header = existingMilestone.header,
            workAreaId = existingMilestone.workArea?.identifier)

    val createResult = cut.create(createMilestoneResource = createMilestoneResource)

    milestoneSearchController
        .search(projectIdentifier, defaultFilter, DEFAULT_SORTING)
        .body!!
        .items
        .also { items ->
          assertThat(items).hasSize(2)
          assertThat(items[0].name).isEqualTo(createResult.body!!.name)
          assertThat(items[0].id).isEqualTo(createResult.body!!.id)
          assertThat(items[1].name).isEqualTo(existingMilestone.name)
          assertThat(items[1].id).isEqualTo(existingMilestone.identifier.toUuid())
        }
  }

  @Test
  fun `verify create multiple milestones in batch succeeds`() {
    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now(),
            header = true,
            craftRef = getIdentifier("projectCraft").asProjectCraftId())

    val investorMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "New Milestone",
            type = INVESTOR,
            date = LocalDate.now(),
            header = true)

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThat(
            createMilestoneBatchCommandHandler.handle(
                projectIdentifier, listOf(craftMilestone, investorMilestone)))
        .hasSize(2)

    projectEventStoreUtils.verifyContains(
        MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.ITEMADDED, 2, false)
  }

  @Test
  fun `verify create multiple milestones in batch succeeds for multiple milestones in same list`() {
    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now().plusDays(2),
            header = true,
            craftRef = getIdentifier("projectCraft").asProjectCraftId())

    val craftMilestone2 =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test2",
            type = CRAFT,
            date = LocalDate.now().plusDays(2),
            header = true,
            craftRef = getIdentifier("projectCraft").asProjectCraftId())

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThat(
            createMilestoneBatchCommandHandler.handle(
                projectIdentifier, listOf(craftMilestone, craftMilestone2)))
        .hasSize(2)

    projectEventStoreUtils.verifyContains(
        MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)
  }

  @Test
  fun `verify create multiple milestones fails if milestones belong to different projects`() {
    eventStreamGenerator.submitProject("p2")

    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now(),
            header = true,
            craftRef = getIdentifier("projectCraft").asProjectCraftId())

    val investorMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("p2").asProjectId(),
            name = "New Milestone",
            type = INVESTOR,
            date = existingMilestone.date,
            header = existingMilestone.header,
            workAreaRef = existingMilestone.workArea?.identifier)

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          createMilestoneBatchCommandHandler.handle(
              projectIdentifier, listOf(craftMilestone, investorMilestone))
        }
        .withMessage("Multiple milestones can only be created for one project at at time")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple milestones fails if milestones belong to a foreign project`() {
    eventStreamGenerator.submitProject("p2")

    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("p2").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now(),
            header = true,
            craftRef = getIdentifier("projectCraft").asProjectCraftId())

    val investorMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("p2").asProjectId(),
            name = "New Milestone",
            type = INVESTOR,
            date = existingMilestone.date,
            header = existingMilestone.header,
            workAreaRef = existingMilestone.workArea?.identifier)

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          createMilestoneBatchCommandHandler.handle(
              projectIdentifier, listOf(craftMilestone, investorMilestone))
        }
        .withMessage("Milestones cannot be created for a foreign project")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple milestones fails if a referenced craft cannot be found`() {
    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now(),
            header = true,
            craftRef = ProjectCraftId())

    val investorMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "New Milestone",
            type = INVESTOR,
            date = existingMilestone.date,
            header = existingMilestone.header,
            workAreaRef = existingMilestone.workArea?.identifier)

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          createMilestoneBatchCommandHandler.handle(
              projectIdentifier, listOf(craftMilestone, investorMilestone))
        }
        .extracting("messageKey")
        .isEqualTo(MILESTONE_VALIDATION_ERROR_CRAFT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple milestones fails if a referenced work area cannot be found`() {
    val craftMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "Test",
            type = CRAFT,
            date = LocalDate.now(),
            header = false,
            craftRef = getIdentifier("projectCraft").asProjectCraftId(),
            workAreaRef = WorkAreaId())

    val investorMilestone =
        CreateMilestoneCommand(
            identifier = MilestoneId(),
            projectRef = getIdentifier("project").asProjectId(),
            name = "New Milestone",
            type = INVESTOR,
            date = existingMilestone.date,
            header = existingMilestone.header)

    val projectIdentifier = getIdentifier("project").asProjectId()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          createMilestoneBatchCommandHandler.handle(
              projectIdentifier, listOf(craftMilestone, investorMilestone))
        }
        .extracting("messageKey")
        .isEqualTo(MILESTONE_VALIDATION_ERROR_WORK_AREA_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }
}
