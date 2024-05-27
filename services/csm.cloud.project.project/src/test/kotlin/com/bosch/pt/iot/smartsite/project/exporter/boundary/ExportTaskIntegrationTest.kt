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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.BAD_WEATHER
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService.Companion.FIELD_ALIAS_CRAFT
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import java.io.ByteArrayInputStream
import java.math.BigDecimal
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
class ExportTaskIntegrationTest : AbstractExportIntegrationTest() {

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
  fun exportTasks(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
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
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
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
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML"])
  fun exportTasksManuallyScheduled(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitWorkdayConfiguration("p2wdc") {
          it.project = getByReference("p2")
          it.allowWorkOnNonWorkingDays = true
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
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

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeComments = false,
                taskExportSchedulingType = TaskExportSchedulingType.MANUALLY_SCHEDULED,
            ))

    val projectFile = UniversalProjectReader().read(ByteArrayInputStream(exportedFile))
    val milestoneTaskMode = projectFile.tasks.find { it.name == "t2" }!!.taskMode
    assertThat(milestoneTaskMode).isEqualTo(TaskMode.MANUALLY_SCHEDULED)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTasksWithStatus(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t2", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask(asReference = "t3") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t3"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t3", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
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
        .submitTaskSchedule("s3") {
          it.task = getByReference("t3")
          it.start = nextMonday.toEpochMilli()
          it.end = nextMonday.plusDays(1).toEpochMilli()
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

    assertThat(importModel.tasks).hasSize(3)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.status).isEqualTo(TaskStatusEnum.DRAFT)
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    assertThat(t2.status).isEqualTo(TaskStatusEnum.STARTED)
    validateExportedIds(t2, "t2", 3, 3)

    val t3 = importModel.tasks.single { it.name == "t3" }
    assertThat(t3.workAreaId).isEqualTo(w1.id)
    assertThat(t3.craftId).isEqualTo(craft1.id)
    assertThat(t3.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateExportedIds(t3, "t3", 4, 4)

    assertThat(importModel.taskSchedules).hasSize(3)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    val s3 = importModel.taskSchedules.single { it.taskId == t3.id }
    assertThat(s3.start).isEqualTo(getEventStartDate("s3"))
    assertThat(s3.end).isEqualTo(getEventEndDate("s3"))
    validateExportedIds(s3, "t3", 4, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTasksScheduledInTheFuture(format: String) {
    val nextMondayInAYear =
        LocalDate.now().plusYears(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t2", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
        }
        .submitTask(asReference = "t3") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t3"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t3", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMondayInAYear.toEpochMilli()
          it.end = nextMondayInAYear.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = nextMondayInAYear.toEpochMilli()
          it.end = nextMondayInAYear.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s3") {
          it.task = getByReference("t3")
          it.start = nextMondayInAYear.toEpochMilli()
          it.end = nextMondayInAYear.plusDays(1).toEpochMilli()
        }
        // Test all day cards are finished
        .submitDayCardG2("t2d1") {
          it.title = "t2d1"
          it.task = getByReference("t2")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitDayCardG2("t2d2") {
          it.title = "t3d2"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitTaskSchedule("s2", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots =
              listOf(
                  getByReference("t2d1").asSlot(nextMondayInAYear),
                  getByReference("t2d2").asSlot(nextMondayInAYear.plusDays(1)))
        }
        // Test not all day cards are finished
        .submitDayCardG2("t3d1") {
          it.title = "t3d1"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitDayCardG2("t3d2") {
          it.title = "t3d2"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.NOTDONE
          it.manpower = BigDecimal.ONE
          it.reason = BAD_WEATHER
        }
        .submitTaskSchedule("s3", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots =
              listOf(
                  getByReference("t3d1").asSlot(nextMondayInAYear),
                  getByReference("t3d2").asSlot(nextMondayInAYear.plusDays(1)))
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

    assertThat(importModel.tasks).hasSize(3)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.status).isEqualTo(TaskStatusEnum.DRAFT)
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    assertThat(t2.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateExportedIds(t2, "t2", 3, 3)

    val t3 = importModel.tasks.single { it.name == "t3" }
    assertThat(t3.workAreaId).isEqualTo(w1.id)
    assertThat(t3.craftId).isEqualTo(craft1.id)
    assertThat(t3.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateExportedIds(t3, "t3", 4, 4)

    assertThat(importModel.taskSchedules).hasSize(3)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    val s3 = importModel.taskSchedules.single { it.taskId == t3.id }
    assertThat(s3.start).isEqualTo(getEventStartDate("s3"))
    assertThat(s3.end).isEqualTo(getEventEndDate("s3"))
    validateExportedIds(s3, "t3", 4, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTasksScheduledInThePast(format: String) {
    val nextMondayOneYearAgo =
        LocalDate.now().minusYears(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t2", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
        }
        .submitTask(asReference = "t3") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t3"
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("t3", eventType = TaskEventEnumAvro.UPDATED) {
          it.status = TaskStatusEnumAvro.ACCEPTED
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMondayOneYearAgo.toEpochMilli()
          it.end = nextMondayOneYearAgo.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s2") {
          it.task = getByReference("t2")
          it.start = nextMondayOneYearAgo.toEpochMilli()
          it.end = nextMondayOneYearAgo.plusDays(1).toEpochMilli()
        }
        .submitTaskSchedule("s3") {
          it.task = getByReference("t3")
          it.start = nextMondayOneYearAgo.toEpochMilli()
          it.end = nextMondayOneYearAgo.plusDays(1).toEpochMilli()
        }
        // Test all day cards are finished
        .submitDayCardG2("t2d1") {
          it.title = "t2d1"
          it.task = getByReference("t2")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitDayCardG2("t2d2") {
          it.title = "t3d2"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitTaskSchedule("s2", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots =
              listOf(
                  getByReference("t2d1").asSlot(nextMondayOneYearAgo),
                  getByReference("t2d2").asSlot(nextMondayOneYearAgo.plusDays(1)))
        }
        // Test not all day cards are finished
        .submitDayCardG2("t3d1") {
          it.title = "t3d1"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.DONE
          it.manpower = BigDecimal.ONE
        }
        .submitDayCardG2("t3d2") {
          it.title = "t3d2"
          it.task = getByReference("t3")
          it.status = DayCardStatusEnumAvro.NOTDONE
          it.manpower = BigDecimal.ONE
          it.reason = BAD_WEATHER
        }
        .submitTaskSchedule("s3", eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.slots =
              listOf(
                  getByReference("t3d1").asSlot(nextMondayOneYearAgo),
                  getByReference("t3d2").asSlot(nextMondayOneYearAgo.plusDays(1)))
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

    assertThat(importModel.tasks).hasSize(3)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.status).isEqualTo(TaskStatusEnum.DRAFT)
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    assertThat(t2.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateExportedIds(t2, "t2", 3, 3)

    val t3 = importModel.tasks.single { it.name == "t3" }
    assertThat(t3.workAreaId).isEqualTo(w1.id)
    assertThat(t3.craftId).isEqualTo(craft1.id)
    assertThat(t3.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateExportedIds(t3, "t3", 4, 4)

    assertThat(importModel.taskSchedules).hasSize(3)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 3)

    val s3 = importModel.taskSchedules.single { it.taskId == t3.id }
    assertThat(s3.start).isEqualTo(getEventStartDate("s3"))
    assertThat(s3.end).isEqualTo(getEventEndDate("s3"))
    validateExportedIds(s3, "t3", 4, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTasks(format: String) {

    // First export
    exportTasks(format)

    // Create second craft pc1 and work area w2. Assign task2 to pc2 and w2.
    eventStreamGenerator
        .submitProjectCraftG2("pc2") { it.name = "pc2" }
        .submitWorkArea("w2") { it.name = "w2" }
        .submitWorkAreaList(asReference = "wal", eventType = WorkAreaListEventEnumAvro.ITEMADDED) {
          it.workAreas = listOf(getByReference("w1"), getByReference("w2"))
          it.project = getByReference("p2")
        }
        .submitTask("t2", eventType = TaskEventEnumAvro.UPDATED) {
          it.name = "updated t2"
          it.workarea = getByReference("w2")
          it.craft = getByReference("pc2")
        }

    // Re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyReExportTasksSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reReExportTasksWithChangedSchedule(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))

    // Export and re-export (with craft / work area change)
    reExportTasks(format)

    // t1 starts one week later
    eventStreamGenerator.submitTaskSchedule("s1", eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.task = getByReference("t1")
      it.start = nextMonday.plusDays(7).toEpochMilli()
      it.end = nextMonday.plusDays(8).toEpochMilli()
    }

    // Re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyReExportTasksSharedValues(importModel)
  }

  private fun verifyReExportTasksSharedValues(importModel: ImportModel) {
    assertThat(importModel.workAreas).hasSize(2)
    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    val w2 = importModel.workAreas[1]
    assertThat(w2.name).isEqualTo("w2")
    validateExportedIds(w2, "w2", 4, 3)

    assertThat(importModel.crafts).hasSize(2)
    val craft1 = importModel.crafts.single { it.name == "pc1" }
    assertThat(craft1).isNotNull
    validateExportedIds(craft1, "t1", 2, 2)

    val craft2 = importModel.crafts.single { it.name == "pc2" }
    assertThat(craft2).isNotNull
    validateExportedIds(craft2, "t2", 3, 4)

    assertThat(importModel.tasks).hasSize(2)
    val t1 = importModel.tasks.single { it.name == "t1" }
    assertThat(t1.workAreaId).isEqualTo(w1.id)
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "updated t2" }
    assertThat(t2.workAreaId).isEqualTo(w2.id)
    assertThat(t2.craftId).isEqualTo(craft2.id)
    validateExportedIds(t2, "t2", 3, 4)

    assertThat(importModel.taskSchedules).hasSize(2)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    assertThat(s1.start).isEqualTo(getEventStartDate("s1"))
    assertThat(s1.end).isEqualTo(getEventEndDate("s1"))
    validateExportedIds(s1, "t1", 2, 2)

    val s2 = importModel.taskSchedules.single { it.taskId == t2.id }
    assertThat(s2.start).isEqualTo(getEventStartDate("s2"))
    assertThat(s2.end).isEqualTo(getEventEndDate("s2"))
    validateExportedIds(s2, "t2", 3, 4)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportTasksWithoutSchedule(format: String) {
    eventStreamGenerator
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t1"
          it.description = "description"
        }
        .submitProjectCraftG2("pc1") { it.name = "pc1" }
        .submitTask(asReference = "t2") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.craft = getByReference("pc1")
          it.name = "t2"
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTasksWithoutScheduleSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun reExportTasksWithoutSchedule(format: String) {

    // First export
    exportTasksWithoutSchedule(format)

    // Don't change anything, just re-export and verify IDs
    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = true,
                includeComments = false))

    val importModel = readExportedFile(exportedFile, project, FIELD_ALIAS_CRAFT)
    verifyExportTasksWithoutScheduleSharedValues(importModel)
  }

  private fun verifyExportTasksWithoutScheduleSharedValues(importModel: ImportModel) {
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
    assertThat(t1.craftId).isEqualTo(craft1.id)
    assertThat(t1.notes).isEqualTo("description")
    validateExportedIds(t1, "t1", 2, 2)

    val t2 = importModel.tasks.single { it.name == "t2" }
    assertThat(t2.workAreaId).isEqualTo(w1.id)
    assertThat(t2.craftId).isEqualTo(craft1.id)
    validateExportedIds(t2, "t2", 3, 3)

    assertThat(importModel.taskSchedules).isEmpty()
  }
}
