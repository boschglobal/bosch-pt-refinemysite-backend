/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MilestoneDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.OtherParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TaskDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TopicDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.NB
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.math.BigDecimal
import java.time.LocalDate.of
import java.time.LocalDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProjectCopyMergeStrategyTest {

  @Nested
  inner class `When merging Participants` {

    @Test
    fun `a Participant in target remains unchanged`() {
      val source = basicEmptyProject
      val target =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002"),
                          companyId = CompanyId("1c216b32-b1c0-11ed-afa1-0242ac120002"),
                          userId = UserId("220494ca-b1c0-11ed-afa1-0242ac120002"),
                          role = FM)))

      val changes = merge(source, target)

      assertThat(changes.participants).isEmpty()
    }

    @Test
    fun `a Participant in source is added with remapped identifier`() {
      val source =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002"),
                          companyId = CompanyId("1c216b32-b1c0-11ed-afa1-0242ac120002"),
                          userId = UserId("220494ca-b1c0-11ed-afa1-0242ac120002"),
                          role = FM)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.participants.first().identifier)
          .isNotEqualTo(source.participants.first().identifier)
      assertThat(changes.participants)
          .usingRecursiveComparison()
          .ignoringFieldsOfTypes(ParticipantId::class.java)
          .isEqualTo(source.participants)
    }

    @Test
    fun `only active Participants are merged`() {
      val source =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      OtherParticipantDto(
                          identifier = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002"),
                          companyId = null,
                          userId = null,
                          role = null,
                          status = INVITED)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.participants).isEmpty()
    }

    @Test
    fun `a Participant of same User remains unchanged`() {
      val userId = UserId("220494ca-b1c0-11ed-afa1-0242ac120002")
      val source =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = ParticipantId("00d13730-b1c0-11ed-afa1-0242ac120002"),
                          companyId = CompanyId("11216b32-b1c0-11ed-afa1-0242ac120002"),
                          userId = userId,
                          role = FM)))
      val target =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = ParticipantId("33e534ba-b1c3-11ed-afa1-0242ac120002"),
                          companyId = CompanyId("44d626b2-b1c3-11ed-afa1-0242ac120002"),
                          userId = userId,
                          role = FM)))

      val changes = merge(source, target)

      assertThat(changes.participants).isEmpty()
    }

    @Test
    fun `a Task in source keeps its assignee with remapped source identifier`() {
      val participantId = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002")
      val source =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = participantId,
                          companyId = CompanyId("1c216b32-b1c0-11ed-afa1-0242ac120002"),
                          userId = UserId("220494ca-b1c0-11ed-afa1-0242ac120002"),
                          role = FM)),
              projectCrafts =
                  listOf(
                      ProjectCraftDto(
                          identifier = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          name = "Craft A",
                          color = "#AABBCC")),
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = participantId,
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().assignee)
          .isEqualTo(changes.participants.first().identifier)
          .isNotEqualTo(participantId)
    }

    @Test
    fun `a Task in source keeps its assignee in target`() {
      val userId = UserId("220494ca-b1c0-11ed-afa1-0242ac120002")
      val sourceParticipantId = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002")
      val source =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = sourceParticipantId,
                          companyId = CompanyId("11216b32-b1c0-11ed-afa1-0242ac120002"),
                          userId = userId,
                          role = FM)),
              projectCrafts =
                  listOf(
                      ProjectCraftDto(
                          identifier = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          name = "Craft A",
                          color = "#AABBCC")),
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = sourceParticipantId,
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val targetParticipantId = ParticipantId("33e534ba-b1c3-11ed-afa1-0242ac120002")
      val target =
          basicEmptyProject.copy(
              participants =
                  setOf(
                      ActiveParticipantDto(
                          identifier = targetParticipantId,
                          companyId = CompanyId("44d626b2-b1c3-11ed-afa1-0242ac120002"),
                          userId = userId,
                          role = FM)))

      val changes = merge(source, target)

      assertThat(changes.tasks.first().assignee).isEqualTo(targetParticipantId)
    }
  }

  @Nested
  inner class `When merging Crafts` {

    @Test
    fun `a Craft in source is added with remapped identifier`() {
      val source =
          basicEmptyProject.copy(
              projectCrafts =
                  listOf(
                      ProjectCraftDto(
                          identifier = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          name = "Craft A",
                          color = "#AABBCC")))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.projectCrafts.first().identifier)
          .isNotEqualTo(source.projectCrafts.first().identifier)
      assertThat(changes.projectCrafts)
          .usingRecursiveComparison()
          .ignoringFieldsOfTypes(ProjectCraftId::class.java)
          .isEqualTo(source.projectCrafts)
    }

    @Test
    fun `a placeholder Craft is added when source does not have any Craft`() {
      val source = basicEmptyProject
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.projectCrafts)
          .extracting(ProjectCraftDto::name, ProjectCraftDto::color)
          .isEqualTo(listOf(tuple("Placeholder Project Craft", "#d9c200")))
    }

    @Test
    fun `a Task in source keeps its Craft with remapped source identifier`() {
      val sourceCraftId = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9")
      val source =
          basicEmptyProject.copy(
              projectCrafts =
                  listOf(
                      ProjectCraftDto(
                          identifier = sourceCraftId, name = "Craft A", color = "#AABBCC")),
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = sourceCraftId,
                          assignee = ParticipantId("0bd13730-b1c0-11ed-afa1-0242ac120002"),
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().projectCraft)
          .isEqualTo(changes.projectCrafts.first().identifier)
          .isNotEqualTo(sourceCraftId)
    }

    @Test
    fun `a Milestone in source keeps its Craft with remapped source identifier`() {
      val sourceCraftId = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9")
      val source =
          basicEmptyProject.copy(
              projectCrafts =
                  listOf(
                      ProjectCraftDto(
                          identifier = sourceCraftId, name = "Craft A", color = "#AABBCC")),
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - project",
                          type = PROJECT,
                          date = of(2023, 2, 28),
                          projectCraft = sourceCraftId,
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().projectCraft)
          .isEqualTo(changes.projectCrafts.first().identifier)
          .isNotEqualTo(sourceCraftId)
    }

    @Test
    fun `a Milestone without Craft in source is merged without Craft`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - project",
                          type = PROJECT,
                          date = of(2023, 2, 28),
                          projectCraft = null,
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().projectCraft).isNull()
    }

    @Test
    fun `a Milestone in source gets the placeholder Craft when its Craft is not contained in source`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - craft",
                          type = CRAFT,
                          date = of(2023, 2, 28),
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().projectCraft)
          .isEqualTo(changes.projectCrafts.first().identifier)
    }
  }

  @Nested
  inner class `When merging Work Areas` {

    @Test
    fun `a Work Area in source is added with remapped identifier`() {
      val source =
          basicEmptyProject.copy(
              workAreas =
                  listOf(
                      WorkAreaDto(
                          identifier = "1b0d9098-a51f-41e2-9935-2faf8225f8d6".asWorkAreaId(),
                          name = "Work Area 1")))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.workAreas.first().identifier)
          .isNotEqualTo(source.workAreas.first().identifier)
      assertThat(changes.workAreas)
          .usingRecursiveComparison()
          .ignoringFieldsOfTypes(UUID::class.java)
          .isEqualTo(source.workAreas)
    }

    @Test
    fun `a placeholder Work Area is added when source does not have any Work Area but contains a Task`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = null,
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.workAreas)
          .extracting(WorkAreaDto::name)
          .isEqualTo(listOf(tuple("Placeholder Working Area")))
    }

    @Test
    fun `a placeholder Work Area is added when source does not have any Work Area but contains a Milestone`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId(),
                          name = "Milestone 1",
                          type = PROJECT,
                          date = of(2023, 2, 12),
                          header = true),
                  ))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.workAreas)
          .extracting(WorkAreaDto::name)
          .isEqualTo(listOf(tuple("Placeholder Working Area")))
    }

    @Test
    fun `no placeholder Work Area is added when source neither has Tasks nor Milestones nor Work Areas`() {
      val source = basicEmptyProject
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.workAreas).isEmpty()
    }

    @Test
    fun `a Task in source keeps its Work Area with remapped source identifier`() {
      val sourceWorkAreaId = "1b0d9098-a51f-41e2-9935-2faf8225f8d6".asWorkAreaId()
      val source =
          basicEmptyProject.copy(
              workAreas = listOf(WorkAreaDto(identifier = sourceWorkAreaId, name = "Work Area 1")),
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = null,
                          workArea = sourceWorkAreaId,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().workArea)
          .isEqualTo(changes.workAreas.first().identifier)
          .isNotEqualTo(sourceWorkAreaId)
    }

    @Test
    fun `a Task without Work Area in source is merged without Work Area`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = null,
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().workArea).isNull()
    }

    @Test
    fun `a Task in source gets the placeholder Work Area when its Work Area is not contained in source`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      TaskDto(
                          identifier = "f0fc5076-ab7e-11ed-afa1-0242ac120002".asTaskId(),
                          name = "task 1",
                          description = null,
                          location = null,
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          assignee = null,
                          workArea = "1b0d9098-a51f-41e2-9935-2faf8225f8d6".asWorkAreaId(),
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().workArea).isEqualTo(changes.workAreas.first().identifier)
      assertThat(changes.workAreas.first().name).isEqualTo("Placeholder Working Area")
    }

    @Test
    fun `a Milestone in source keeps its Work Area with remapped source identifier`() {
      val sourceWorkAreaId = "1b0d9098-a51f-41e2-9935-2faf8225f8d6".asWorkAreaId()
      val source =
          basicEmptyProject.copy(
              workAreas = listOf(WorkAreaDto(identifier = sourceWorkAreaId, name = "Work Area 1")),
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - project",
                          type = PROJECT,
                          date = of(2023, 2, 28),
                          projectCraft = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                          workArea = sourceWorkAreaId,
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().workArea)
          .isEqualTo(changes.workAreas.first().identifier)
          .isNotEqualTo(sourceWorkAreaId)
    }

    @Test
    fun `a Milestone without Work Area in source is merged without Work Area`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - project",
                          type = PROJECT,
                          date = of(2023, 2, 28),
                          projectCraft = null,
                          workArea = null,
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().workArea).isNull()
    }

    @Test
    fun `a Milestone in source gets the placeholder Work Area when its Work Area is not contained in source`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId("3acd4fc8-a0fa-11ed-a8fc-0242ac120002"),
                          name = "Milestone 1 - project",
                          type = PROJECT,
                          date = of(2023, 2, 28),
                          projectCraft = null,
                          workArea = "0d881f29-78f2-4cdf-830e-7ba8edd1cd58".asWorkAreaId(),
                          header = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().workArea)
          .isEqualTo(changes.workAreas.first().identifier)
      assertThat(changes.workAreas.first().name).isEqualTo("Placeholder Working Area")
    }
  }

  @Nested
  inner class `When merging Tasks and DayCards` {

    @Test
    fun `a Task in source is added with remapped identifier`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      TaskDto(
                          identifier = TaskId(),
                          name = "Task 1",
                          description = "Simple unscheduled task",
                          location = null,
                          projectCraft = "5a37b632-db66-47a5-9d0c-e8718da8c7a1".asProjectCraftId(),
                          assignee = null,
                          workArea = null,
                          status = DRAFT,
                          start = null,
                          end = null)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().identifier).isNotEqualTo(source.tasks.first().identifier)
      assertThat(changes.tasks)
          .usingRecursiveComparison()
          .ignoringFields("identifier")
          .ignoringFieldsOfTypes(ProjectCraftId::class.java)
          .isEqualTo(source.tasks)
    }

    @Test
    fun `a Task with DayCard in source is added with remapped DayCard identifier`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      simpleUnscheduledTask.copy(
                          dayCards =
                              listOf(
                                  DayCardDto(
                                      identifier =
                                          "05959425-06d1-4617-8504-43d31e35f229".asDayCardId(),
                                      date = of(2023, 2, 6),
                                      title = "Work, work",
                                      manpower = BigDecimal(4).setScale(2),
                                      status = DONE)))))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().dayCards.first().identifier)
          .isNotEqualTo(source.tasks.first().dayCards.first().identifier)
      assertThat(changes.tasks.first().dayCards)
          .usingRecursiveComparison()
          .ignoringFields("identifier")
          .isEqualTo(source.tasks.first().dayCards)
    }

    @Test
    fun `a Relation between Tasks is added with remapped relation element ids`() {
      val taskA =
          simpleUnscheduledTask.copy(identifier = "7a5d5950-79bb-48a5-bac5-8bfe62e5b987".asTaskId())
      val taskB =
          simpleUnscheduledTask.copy(identifier = "089ed871-d4ed-4817-8537-561a4ae5cf9e".asTaskId())
      val source =
          basicEmptyProject.copy(
              tasks = listOf(taskA, taskB),
              relations =
                  listOf(
                      RelationDto(
                          type = FINISH_TO_START,
                          source = RelationElementDto(id = taskA.identifier.toUuid(), type = TASK),
                          target = RelationElementDto(id = taskB.identifier.toUuid(), type = TASK),
                          criticality = true)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.relations)
          .usingRecursiveComparison()
          .ignoringFields("source.id", "target.id")
          .isEqualTo(source.relations)
      assertThat(changes.relations.first().source.id)
          .isEqualTo(changes.tasks[0].identifier.toUuid())
          .isNotEqualTo(taskA.identifier)
      assertThat(changes.relations.first().target.id)
          .isEqualTo(changes.tasks[1].identifier.toUuid())
          .isNotEqualTo(taskB.identifier)
    }

    @Test
    fun `a Topic with Message in source is added with remapped identifiers`() {
      val source =
          basicEmptyProject.copy(
              tasks =
                  listOf(
                      simpleUnscheduledTask.copy(
                          topics =
                              listOf(
                                  TopicDto(
                                      identifier =
                                          "fa763a5b-5c25-4d1b-ab05-16758090d697".asTopicId(),
                                      criticality = UNCRITICAL,
                                      description = "Boring Topic",
                                      messages =
                                          listOf(
                                              MessageDto(
                                                  identifier =
                                                      "68a927bf-ac83-4df1-a87a-696a50d55f5a"
                                                          .toUUID()
                                                          .asMessageId(),
                                                  timestamp =
                                                      LocalDateTime.of(2023, 3, 1, 16, 34, 24),
                                                  author =
                                                      "3ee19932-5863-4874-bd92-2405839577b7"
                                                          .asUserId(),
                                                  content = "My message")))))))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.tasks.first().topics.first().identifier)
          .isNotEqualTo(source.tasks.first().topics.first().identifier)
      assertThat(changes.tasks.first().topics.first().messages.first().identifier)
          .isNotEqualTo(source.tasks.first().topics.first().messages.first().identifier)
      assertThat(changes.tasks.first().topics)
          .usingRecursiveComparison()
          .ignoringFields("identifier", "messages.identifier")
          .isEqualTo(source.tasks.first().topics)
    }

    private val simpleUnscheduledTask =
        TaskDto(
            identifier = "97cf08ce-4d6a-4bce-86c7-3de4e0cf152a".asTaskId(),
            name = "Task 1",
            description = "Simple unscheduled task",
            location = null,
            projectCraft = "5a37b632-db66-47a5-9d0c-e8718da8c7a1".asProjectCraftId(),
            assignee = null,
            workArea = null,
            status = DRAFT,
            start = null,
            end = null)
  }

  @Nested
  inner class `When merging Milestones` {

    @Test
    fun `a MileStone in source is added with remapped identifier`() {
      val source =
          basicEmptyProject.copy(
              milestones =
                  listOf(
                      MilestoneDto(
                          identifier = MilestoneId(),
                          name = "Milestone 1",
                          type = PROJECT,
                          date = of(2023, 2, 12),
                          header = true),
                  ))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.milestones.first().identifier)
          .isNotEqualTo(source.milestones.first().identifier)
      assertThat(changes.milestones)
          .usingRecursiveComparison()
          .ignoringFieldsOfTypes(MilestoneId::class.java)
          .isEqualTo(source.milestones)
    }

    @Test
    fun `a Relation between Milestones is added with remapped relation element ids`() {
      val milestoneA =
          MilestoneDto(
              identifier = "adb66773-f58c-498f-b2ad-33a6a908ca53".asMilestoneId(),
              name = "Milestone 1",
              type = PROJECT,
              date = of(2023, 2, 11),
              header = true)
      val milestoneB =
          MilestoneDto(
              identifier = "c8279c04-7969-412b-9260-7f7d1ec9da01".asMilestoneId(),
              name = "Milestone 2",
              type = PROJECT,
              date = of(2023, 2, 14),
              header = true)
      val source =
          basicEmptyProject.copy(
              milestones = listOf(milestoneA, milestoneB),
              relations =
                  listOf(
                      RelationDto(
                          type = FINISH_TO_START,
                          source =
                              RelationElementDto(
                                  id = milestoneA.identifier.toUuid(), type = MILESTONE),
                          target =
                              RelationElementDto(
                                  id = milestoneB.identifier.toUuid(), type = MILESTONE),
                          criticality = false)))
      val target = basicEmptyProject

      val changes = merge(source, target)

      assertThat(changes.relations)
          .usingRecursiveComparison()
          .ignoringFields("source.id", "target.id")
          .isEqualTo(source.relations)
      assertThat(changes.relations.first().source.id.asMilestoneId())
          .isEqualTo(changes.milestones[0].identifier)
          .isNotEqualTo(milestoneA.identifier)
      assertThat(changes.relations.first().target.id.asMilestoneId())
          .isEqualTo(changes.milestones[1].identifier)
          .isNotEqualTo(milestoneB.identifier)
    }
  }

  private fun merge(source: ProjectDto, target: ProjectDto): ProjectDto {
    return ProjectCopyMergeStrategy().merge(source, target)
  }

  private val basicEmptyProject =
      ProjectDto(
          identifier = ProjectId("91b19ee6-f6f2-4491-9e31-73f75aa53d22"),
          client = "Bosch PT",
          description = "New Offices",
          start = of(2023, 2, 5),
          end = of(2023, 3, 21),
          projectNumber = "123",
          title = "Import Project",
          category = NB,
          address =
              ProjectAddressVo(
                  street = "Test Street", houseNumber = "1", city = "Test Town", zipCode = "12345"),
          participants = emptySet(),
          projectCrafts = emptyList(),
          workAreas = emptyList(),
          milestones = emptyList(),
          tasks = emptyList(),
          relations = emptyList())
}
