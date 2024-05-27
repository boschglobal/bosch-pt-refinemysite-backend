/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import net.sf.mpxj.TaskMode
import net.sf.mpxj.reader.UniversalProjectReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportMilestoneIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportMilestone(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
        }
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
        }
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(1)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 2, 2)
    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    // Check milestone properties
    assertThat(importModel.milestones).hasSize(1)
    val m1 = importModel.milestones[0]
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportMilestoneAutomaticallyScheduled(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false,
                milestoneExportSchedulingType = MilestoneExportSchedulingType.AUTO_SCHEDULED,
            ))

    val projectFile = UniversalProjectReader().read(ByteArrayInputStream(exportedFile))
    val milestoneTaskMode = projectFile.tasks.find { it.name == "m1" }!!.taskMode
    assertThat(milestoneTaskMode).isEqualTo(TaskMode.AUTO_SCHEDULED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun exportMilestoneManuallyScheduled(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitMilestone("m1") {
          it.name = "m1"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
        }
        .submitMilestoneList("ml1") {
          it.milestones = listOf(getByReference("m1"))
          it.date = nextMonday.plusDays(2).toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeComments = false,
                milestoneExportSchedulingType = MilestoneExportSchedulingType.MANUALLY_SCHEDULED,
            ))

    val projectFile = UniversalProjectReader().read(ByteArrayInputStream(exportedFile))
    val milestoneTaskMode = projectFile.tasks.find { it.name == "m1" }!!.taskMode
    assertThat(milestoneTaskMode).isEqualTo(TaskMode.MANUALLY_SCHEDULED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun reExportMilestoneMsProject(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    val importModel = reExport(format)

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertThat(importModel.crafts).hasSize(2)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    val craft2 = importModel.crafts.single { it.name == "pc2" }
    assertThat(craft2).isNotNull
    validateExportedIds(craft2, "m2", 5, 5)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 2, 2)
    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    // Check milestone properties
    assertThat(importModel.milestones).hasSize(3)
    val m1 = importModel.milestones[0]
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("updated m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 4)

    val m2 = importModel.milestones[1]
    assertThat(m2.workAreaId).isEqualTo(w1.id)
    assertThat(m2.name).isEqualTo("m2")
    assertThat(m2.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m2.header).isFalse
    assertThat(m2.notes).isEmpty()
    assertThat(m2.craftId).isEqualTo(craft2.id)
    validateExportedIds(m2, "m2", 5, 5)

    val m3 = importModel.milestones[2]
    assertThat(m3.workAreaId).isNull()
    assertThat(m3.name).isEqualTo("m3")
    assertThat(m3.date).isEqualTo(nextMonday.plusDays(7))
    assertThat(m3.header).isTrue()
    assertThat(m3.notes).isEmpty()
    assertThat(m3.craftId).isNull()
    validateExportedIds(m3, "m3", 6, 6)
  }

  // Different export order in P6 than in MS Project
  @ParameterizedTest
  @ValueSource(strings = ["PRIMAVERA_P6_XML"])
  fun reExportMilestoneP6(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    val importModel = reExport(format)

    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 2)

    assertThat(importModel.crafts).hasSize(2)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 3)

    val craft2 = importModel.crafts.single { it.name == "pc2" }
    assertThat(craft2).isNotNull
    validateExportedIds(craft2, "m2", 5, 6)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 2, 3)
    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t2, "t2", 3, 4)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 3)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 4)

    // Check milestone properties
    assertThat(importModel.milestones).hasSize(3)
    val m3 = importModel.milestones[0]
    assertThat(m3.workAreaId).isNull()
    assertThat(m3.name).isEqualTo("m3")
    assertThat(m3.date).isEqualTo(nextMonday.plusDays(7))
    assertThat(m3.header).isTrue()
    assertThat(m3.notes).isEmpty()
    assertThat(m3.craftId).isNull()
    validateExportedIds(m3, "m3", 6, 1)

    val m1 = importModel.milestones[1]
    assertThat(m1.workAreaId).isEqualTo(w1.id)
    assertThat(m1.name).isEqualTo("updated m1")
    assertThat(m1.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m1.header).isFalse
    assertThat(m1.notes).isEmpty()
    assertThat(m1.craftId).isEqualTo(craft1.id)
    validateExportedIds(m1, "m1", 4, 5)

    val m2 = importModel.milestones[2]
    assertThat(m2.workAreaId).isEqualTo(w1.id)
    assertThat(m2.name).isEqualTo("m2")
    assertThat(m2.date).isEqualTo(nextMonday.plusDays(2))
    assertThat(m2.header).isFalse()
    assertThat(m2.notes).isEmpty()
    assertThat(m2.craftId).isEqualTo(craft2.id)
    validateExportedIds(m2, "m2", 5, 6)
  }

  private fun reExport(format: String): ImportModel {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    // First export
    exportMilestone(format)

    // Change data, re-export and verify IDs
    eventStreamGenerator
        .submitProjectCraftG2("pc2") { it.name = "pc2" }
        .submitMilestone("m1", eventType = MilestoneEventEnumAvro.UPDATED) {
          it.name = "updated m1"
        }
        // Add m2 to the same date and work area with different craft
        .submitMilestone("m2") {
          it.name = "m2"
          it.date = nextMonday.plusDays(2).toEpochMilli()
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc2")
        }
        .submitMilestoneList("ml1", eventType = MilestoneListEventEnumAvro.ITEMADDED) {
          it.milestones = listOf(getByReference("m1"), getByReference("m2"))
        }
        .submitMilestone("m3") {
          it.name = "m3"
          it.date = nextMonday.plusDays(7).toEpochMilli()
          it.header = true
          it.type = MilestoneTypeEnumAvro.INVESTOR
        }
        .submitMilestoneList("ml2") {
          it.milestones = listOf(getByReference("m3"))
          it.date = nextMonday.plusDays(7).toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    return readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
  }
}
