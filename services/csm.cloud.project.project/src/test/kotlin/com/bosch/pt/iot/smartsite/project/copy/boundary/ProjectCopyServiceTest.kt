/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.copy.messages.ProjectCopyStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.copy.boundary.ExportSettings.Companion.exportEverything
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum.NB
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate.of
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ProjectCopyServiceTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var projectCopyService: ProjectCopyService

  @Autowired private lateinit var genericExporter: GenericExporter

  @Autowired private lateinit var genericImporter: GenericImporter

  private val copyingUser: UserId by lazy { getIdentifier("userCsm2").asUserId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    setAuthentication(copyingUser.toUuid())
  }

  @Test
  fun `verify a Project with Crafts and WorkAreas is copied`() {
    genericImporter.import(basicProjectWithCraftsAndWorkAreas)

    val copyResult =
        projectCopyService.copy(
            basicProjectWithCraftsAndWorkAreas.identifier,
            basicProjectCopyParameters.copy(workingAreas = true, disciplines = true))

    val export = genericExporter.export(copyResult.projectId, exportEverything)
    assertThat(export.withoutCopyingUserAsParticipant().toString().withErasedIdentifiers())
        .isEqualTo(basicProjectWithCraftsAndWorkAreas.toString().withErasedIdentifiers())
  }

  @Test
  fun `verify a Project is copied with replaced title`() {
    genericImporter.import(basicProjectWithCraftsAndWorkAreas)

    val copyResult =
        projectCopyService.copy(
            basicProjectWithCraftsAndWorkAreas.identifier,
            basicProjectCopyParameters.copy(projectName = "My Project (Copy)"))

    val export = genericExporter.export(copyResult.projectId, exportEverything)
    assertThat(export.withoutCopyingUserAsParticipant().toString().withErasedIdentifiers())
        .isEqualTo(
            basicProjectWithCraftsAndWorkAreas
                .copy(title = "My Project (Copy)")
                .toString()
                .withErasedIdentifiers())
  }

  @Test
  fun `verify active task assignees remain assigned to tasks on copied Projects`() {
    val copy =
        projectCopyService.copy(
            getIdentifier("project").asProjectId(),
            ProjectCopyParameters(projectName = "Copy", tasks = true, keepTaskAssignee = true))

    val exportOfCopy = genericExporter.export(copy.projectId, exportEverything)

    assertThat(exportOfCopy.tasks).singleElement().extracting { it.assignee }.isNotNull()
  }

  @Test
  fun `verify inactive task assignees are unassigned from tasks on copied Projects`() {
    // deactivate the task assignee (note: there's only one task assigned to "participant")
    eventStreamGenerator.submitParticipantG3(asReference = "participant", eventType = DEACTIVATED) {
      it.status = INACTIVE
    }

    val copy =
        projectCopyService.copy(
            getIdentifier("project").asProjectId(),
            ProjectCopyParameters(projectName = "Copy", tasks = true, keepTaskAssignee = true))

    val exportOfCopy = genericExporter.export(copy.projectId, exportEverything)

    assertThat(exportOfCopy.tasks).singleElement().extracting { it.assignee }.isNull()
  }

  @Test
  fun `verify copying a project publishes events in a copy business transaction`() {
    eventStreamGenerator.submitProject("copyProject").submitParticipantG3("copyParticipantCsm2") {
      it.user = EventStreamGeneratorStaticExtensions.getByReference("userCsm2")
      it.role = ParticipantRoleEnumAvro.CSM
    }
    val originalProjectId = getIdentifier("copyProject").asProjectId()
    projectEventStoreUtils.reset()

    projectCopyService.copy(originalProjectId, ProjectCopyParameters("My new copied project"))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            ProjectCopyStartedEventAvro::class.java,
            ProjectEventAvro::class.java,
            ParticipantEventG3Avro::class.java,
            ProjectCraftListEventAvro::class.java,
            WorkAreaListEventAvro::class.java,
            WorkdayConfigurationEventAvro::class.java,
            ProjectCraftEventG2Avro::class.java,
            ProjectCraftListEventAvro::class.java,
            ProjectCopyFinishedEventAvro::class.java))
  }

  @Nested
  inner class `When exporting` {

    @Test
    fun `Participants are exported when Tasks are copied with their Assignees`() {
      val copyParameters =
          ProjectCopyParameters(projectName = "My project", tasks = true, keepTaskAssignee = true)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportParticipants).isTrue
    }

    @Test
    fun `Participants are not exported when not required`() {
      val copyParameters =
          ProjectCopyParameters(projectName = "My project", keepTaskAssignee = false)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportParticipants).isFalse
    }

    @Test
    fun `Relations are exported when Tasks are copied`() {
      val copyParameters = ProjectCopyParameters(projectName = "My project", tasks = true)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportRelations).isTrue
    }

    @Test
    fun `Relations are exported when Milestones are copied`() {
      val copyParameters = ProjectCopyParameters(projectName = "My project", milestones = true)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportRelations).isTrue
    }

    @Test
    fun `Relations are not exported when not required`() {
      val copyParameters =
          ProjectCopyParameters(projectName = "My project", tasks = false, milestones = false)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportRelations).isFalse
    }

    @Test
    fun `Task status are not exported when not required`() {
      val copyParameters =
          ProjectCopyParameters(projectName = "My project", tasks = true, keepTaskStatus = false)

      val exportSettings = copyParameters.createExportSettings()

      assertThat(exportSettings.exportTaskStatus).isFalse
    }

    @Test
    fun `verify exception is thrown when project name is blank`() {
      assertThatThrownBy { ProjectCopyParameters(projectName = " ") }
          .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `verify exception is thrown when DayCards are copied without tasks`() {
      assertThatThrownBy {
            ProjectCopyParameters(projectName = "My new project", dayCards = true, tasks = false)
          }
          .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `verify exception is thrown when TaskAssignees are copied without tasks`() {
      assertThatThrownBy {
            ProjectCopyParameters(
                projectName = "My new project", keepTaskAssignee = true, tasks = false)
          }
          .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `verify exception is thrown when TaskStatus are copied without TaskAssignees`() {
      assertThatThrownBy {
            ProjectCopyParameters(
                projectName = "My new project", keepTaskStatus = true, keepTaskAssignee = false)
          }
          .isInstanceOf(IllegalArgumentException::class.java)
    }
  }

  private val basicProjectWithCraftsAndWorkAreas =
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
          projectCrafts =
              listOf(
                  ProjectCraftDto(
                      identifier = ProjectCraftId("40e7b37b-65ab-4b23-8178-8fac651abab9"),
                      name = "Craft A",
                      color = "#AABBCC"),
                  ProjectCraftDto(
                      identifier = ProjectCraftId("80df458e-c625-44c7-bcbb-6e4d6f338f1a"),
                      name = "Craft B",
                      color = "#DDEEFF")),
          workAreas =
              listOf(
                  WorkAreaDto(
                      identifier = "1b0d9098-a51f-41e2-9935-2faf8225f8d6".asWorkAreaId(),
                      name = "Work Area 1"),
                  WorkAreaDto(
                      identifier = "50151dc4-8921-488f-8276-9c08a7f9ac81".asWorkAreaId(),
                      name = "Work Area 2")),
          milestones = emptyList(),
          tasks = emptyList(),
          relations = emptyList())

  private val basicProjectCopyParameters =
      ProjectCopyParameters(
          projectName = basicProjectWithCraftsAndWorkAreas.title,
          workingAreas = true,
          disciplines = true)

  private fun ProjectDto.withoutCopyingUserAsParticipant(): ProjectDto =
      this.copy(participants = this.participants.filter { it.userId != copyingUser }.toSet())

  private fun String.withErasedIdentifiers() =
      this.replace(Regex("identifier=.+?,"), "identifier=,")
}
