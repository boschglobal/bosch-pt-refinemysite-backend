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
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportWorkAreaIntegrationTest : AbstractExportIntegrationTest() {

  @Autowired private lateinit var projectExportService: ProjectExportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }
  private val project by lazy { checkNotNull(repositories.findProject(projectIdentifier)) }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkAreas(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitWorkArea(asReference = "w2") { it.name = "w2" }
        .submitWorkAreaList(asReference = "wal", eventType = ITEMADDED) {
          it.workAreas.add(getByReference("w2"))
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.name = "t1"
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w2")
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

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)

    assertThat(importModel.workAreas).hasSize(2)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    verifyExportWorkAreasSharedValues(importModel, w1.id)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportWorkAreas(format: String) {

    // First export
    exportWorkAreas(format)

    // Change data, re-export and verify IDs
    eventStreamGenerator.submitWorkArea("w1", eventType = WorkAreaEventEnumAvro.UPDATED) {
      it.name = "updated w1"
    }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)

    assertThat(importModel.workAreas).hasSize(2)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("updated w1")
    validateExportedIds(w1, "w1", 1, 1)

    verifyExportWorkAreasSharedValues(importModel, w1.id)
  }

  private fun verifyExportWorkAreasSharedValues(
      importModel: ImportModel,
      w1Identifier: WorkAreaIdentifier
  ) {
    assertThat(importModel.workAreas).hasSize(2)

    val w2 = importModel.workAreas[1]
    assertThat(w2.name).isEqualTo("w2")
    validateExportedIds(w2, "w2", 2, 3)

    assertContainsOnlyRmsPlaceholderCraft(importModel)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.workAreaId == w1Identifier }
    assertThat(t1.name).isEqualTo("t1")
    validateExportedIds(t1, "t1", 3, 2)
    val t2 = importModel.tasks.single { it.workAreaId == w2.id }
    assertThat(t2.name).isEqualTo("t2")
    validateExportedIds(t2, "t2", 4, 4)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 3, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 4, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun exportWithAndWithoutWorkAreaMsProject(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        // Add working area w2 as an empty working area - it will not be exported
        .submitWorkArea(asReference = "w2") { it.name = "w2" }
        .submitWorkAreaList(asReference = "wal", eventType = ITEMADDED) {
          it.workAreas.add(getByReference("w2"))
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.name = "t1"
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
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

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithAndWithoutWorkAreaMsProjectSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun reExportWithAndWithoutWorkAreaMsProject(format: String) {

    // First export
    exportWithAndWithoutWorkAreaMsProject(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithAndWithoutWorkAreaMsProjectSharedValues(importModel)
  }

  private fun verifyExportWithAndWithoutWorkAreaMsProjectSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    // Work area 2 is exported as empty work area and is therefore recognized as task.
    // The fileId is automatically re-arranged by mpxj.

    assertContainsOnlyRmsPlaceholderCraft(importModel)

    assertThat(importModel.tasks).hasSize(3)
    val t1 = importModel.tasks.single { it.workAreaId == w1.id }
    assertThat(t1.name).isEqualTo("t1")
    validateExportedIds(t1, "t1", 3, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isNull()
    validateExportedIds(t2, "t2", 4, 4)

    // Work area 2 is detected as task, as it doesn't have children
    val t3 = importModel.tasks.single { it.name == "w2" }
    assertThat(t3.workAreaId).isNull()
    validateExportedIds(t3, "w2", 2, 3)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 3, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 4, 4)
  }

  // For P6, the task without working area is exported before the working area with task.
  @ParameterizedTest
  @ValueSource(strings = ["PRIMAVERA_P6_XML"])
  fun exportWithAndWithoutWorkingAreaP6(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        // Add working area w2 as an empty working area - it will not be exported
        .submitWorkArea(asReference = "w2") { it.name = "w2" }
        .submitWorkAreaList(asReference = "wal", eventType = ITEMADDED) {
          it.workAreas.add(getByReference("w2"))
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.name = "t1"
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
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

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithAndWithoutWorkingAreaP6SharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["PRIMAVERA_P6_XML"])
  fun reExportWithAndWithoutWorkingAreaP6(format: String) {

    // First export
    exportWithAndWithoutWorkingAreaP6(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithAndWithoutWorkingAreaP6SharedValues(importModel)
  }

  private fun verifyExportWithAndWithoutWorkingAreaP6SharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(1)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 3)

    // Work area 2 is exported as empty work area and is therefore recognized as task.
    // The fileId is automatically re-arranged by mpxj.

    assertContainsOnlyRmsPlaceholderCraft(importModel)

    assertThat(importModel.tasks).hasSize(3)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    validateExportedIds(t1, "t1", 3, 4)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isNull()
    validateExportedIds(t2, "t2", 4, 2)

    // Work area 2
    val t3 = importModel.tasks.single { it.name == "w2" }
    assertThat(t3.workAreaId).isNull()
    validateExportedIds(t3, "w2", 2, 1)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 3, 4)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 4, 2)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWithoutWorkAreas(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.name = "t1"
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
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

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithoutWorkAreasSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportWithoutWorkAreas(format: String) {

    // First export
    exportWithoutWorkAreas(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWithoutWorkAreasSharedValues(importModel)
  }

  private fun verifyExportWithoutWorkAreasSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).isEmpty()
    assertContainsOnlyRmsPlaceholderCraft(importModel)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1).isNotNull
    validateExportedIds(t1, "t1", 1, 1)
    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2).isNotNull
    validateExportedIds(t2, "t2", 2, 2)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 1, 1)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 2, 2)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkAreasWithoutTasks(format: String) {
    eventStreamGenerator
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitWorkArea(asReference = "w2") { it.name = "w2" }
        .submitWorkAreaList(asReference = "wal", eventType = ITEMADDED) {
          it.workAreas.add(getByReference("w2"))
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWorkAreasWithoutTasksSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportWorkAreasWithoutTasks(format: String) {

    // First export
    exportWorkAreasWithoutTasks(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project)
    verifyExportWorkAreasWithoutTasksSharedValues(importModel)
  }

  private fun verifyExportWorkAreasWithoutTasksSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).isEmpty()

    assertContainsOnlyRmsPlaceholderCraft(importModel)

    // Working areas without tasks cannot be detected as working areas when import them again. They
    // should be detected as tasks.
    assertThat(importModel.tasks).hasSize(2)
    val w1 = importModel.tasks.single { it.name == "w1" }
    assertThat(w1).isNotNull
    validateExportedIds(w1, "w1", 1, 1)

    val w2 = importModel.tasks.single { it.name == "w2" }
    assertThat(w2).isNotNull
    validateExportedIds(w2, "w2", 2, 2)
  }
}
