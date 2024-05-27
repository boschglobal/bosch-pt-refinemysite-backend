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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.ProjectUtils.isMppOrMSPDI
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_AFTERNOON
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_MORNING
import net.sf.mpxj.ProjectFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ExportWorkdayConfigurationIntegrationTest : AbstractExportIntegrationTest() {

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
        .submitWorkArea(asReference = "w1") { it.name = "w1" }
        .submitWorkAreaList(asReference = "wal") {
          it.workAreas = listOf(getByReference("w1"))
          it.project = getByReference("p2")
        }
        .submitTask(asReference = "t1") {
          it.project = getByReference("p2")
          it.workarea = getByReference("w1")
          it.name = "t1"
        }

    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkdayConfigurationWithoutModifications(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(MONDAY))
    val nextFriday = nextMonday.plusDays(4)

    eventStreamGenerator
        .submitWorkdayConfiguration("p2WorkDayConfiguration") {
          it.project = getByReference("p2")
          it.startOfWeek = DayEnumAvro.MONDAY
          it.allowWorkOnNonWorkingDays = false
          it.workingDays = moFrWeekAvro()
          it.holidays = emptyList()
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextFriday.toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val projectFile = parseExportedFile(exportedFile)
    val importModel = readExportedFile(projectFile, project)

    val expectedStartDate =
        nextMonday.atStartOfDay().plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

    val expectedEndDate =
        nextFriday.atStartOfDay().plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

    verifyTaskDates(importModel, expectedStartDate, expectedEndDate)
    verifyCalendarDaysMondayFriday(projectFile)
    verifyExportedWorkAreasSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkdayConfigurationWithExtendedTaskWeekend(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(MONDAY))
    val nextSaturday = nextMonday.plusDays(2).with(TemporalAdjusters.next(SATURDAY))

    eventStreamGenerator
        .submitWorkdayConfiguration("p2WorkDayConfiguration") {
          it.project = getByReference("p2")
          it.startOfWeek = DayEnumAvro.MONDAY
          it.allowWorkOnNonWorkingDays = false
          it.workingDays = moFrWeekAvro()
          it.holidays = emptyList()
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextSaturday.toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val projectFile = parseExportedFile(exportedFile)
    val importModel = readExportedFile(projectFile, project)

    val expectedStartDate =
        nextMonday.atStartOfDay().plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

    val expectedEndDate =
        nextSaturday
            .plusDays(2) // Following monday
            .atStartOfDay()
            .plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

    verifyTaskDates(importModel, expectedStartDate, expectedEndDate)
    verifyCalendarDaysMondayFriday(projectFile)
    verifyExportedWorkAreasSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkdayConfigurationWithExtendedTaskHoliday(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(MONDAY))
    val nextThursday = nextMonday.plusDays(2).with(TemporalAdjusters.next(THURSDAY))

    eventStreamGenerator
        .submitWorkdayConfiguration("p2WorkDayConfiguration") {
          it.project = getByReference("p2")
          it.startOfWeek = DayEnumAvro.MONDAY
          it.allowWorkOnNonWorkingDays = false
          it.workingDays = moFrWeekAvro()
          it.holidays = listOf(HolidayAvro("Thursday", nextThursday.toEpochMilli()))
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextThursday.toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val projectFile = parseExportedFile(exportedFile)
    val importModel = readExportedFile(projectFile, project)

    val expectedStartDate =
        nextMonday.atStartOfDay().plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

    val expectedEndDate =
        nextThursday
            .plusDays(1) // Following friday
            .atStartOfDay()
            .plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

    verifyTaskDates(importModel, expectedStartDate, expectedEndDate)
    verifyCalendarDaysMondayFriday(projectFile)
    verifyExportedWorkAreasSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkdayConfigurationWithWorkOnNonworkingDay(format: String) {
    val nextMonday = LocalDate.now().with(TemporalAdjusters.next(MONDAY))
    val nextSaturday = nextMonday.plusDays(2).with(TemporalAdjusters.next(SATURDAY))

    eventStreamGenerator
        .submitWorkdayConfiguration("p2WorkDayConfiguration") {
          it.project = getByReference("p2")
          it.startOfWeek = DayEnumAvro.MONDAY
          it.allowWorkOnNonWorkingDays = true
          it.workingDays = moFrWeekAvro()
          it.holidays = emptyList()
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextMonday.toEpochMilli()
          it.end = nextSaturday.toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val projectFile = parseExportedFile(exportedFile)
    val importModel = readExportedFile(projectFile, project)

    val expectedStartDate =
        nextMonday.atStartOfDay().plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

    val expectedEndDate =
        nextSaturday.atStartOfDay().plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

    verifyTaskDates(importModel, expectedStartDate, expectedEndDate)
    verifyCalendarDaysMondayFriday(projectFile)
    verifyExportedWorkAreasSharedValues(importModel)
  }

  @ParameterizedTest
  @ValueSource(strings = ["MS_PROJECT_XML", "PRIMAVERA_P6_XML"])
  fun exportWorkdayConfigurationWithCustomWeek(format: String) {
    val nextWednesday = LocalDate.now().with(TemporalAdjusters.next(WEDNESDAY))
    val nextThursday = nextWednesday.plusDays(1).with(TemporalAdjusters.next(THURSDAY))

    eventStreamGenerator
        .submitWorkdayConfiguration("p2WorkDayConfiguration") {
          it.project = getByReference("p2")
          it.startOfWeek = DayEnumAvro.WEDNESDAY
          it.allowWorkOnNonWorkingDays = false
          it.workingDays = listOf(DayEnumAvro.TUESDAY, DayEnumAvro.WEDNESDAY, DayEnumAvro.THURSDAY)
          it.holidays = emptyList()
        }
        .submitTaskSchedule("s1") {
          it.task = getByReference("t1")
          it.start = nextWednesday.toEpochMilli()
          it.end = nextThursday.toEpochMilli()
        }

    val exportedFile =
        projectExportService.export(
            project,
            ProjectExportParameters(
                format = ProjectExportFormatEnum.valueOf(format),
                includeMilestones = false,
                includeComments = false))

    val projectFile = parseExportedFile(exportedFile)
    val importModel = readExportedFile(projectFile, project)

    val expectedStartDate =
        nextWednesday.atStartOfDay().plusHours(DEFAULT_WORKING_MORNING.start.hour.toLong())

    val expectedEndDate =
        nextThursday.atStartOfDay().plusHours(DEFAULT_WORKING_AFTERNOON.end.hour.toLong())

    verifyTaskDates(importModel, expectedStartDate, expectedEndDate)
    verifyCalendarDaysTuesdayThursday(projectFile)
    verifyExportedWorkAreasSharedValues(importModel)
  }

  private fun verifyTaskDates(
      importModel: ImportModel,
      expectedStartDate: LocalDateTime,
      expectedEndDate: LocalDateTime
  ) {
    val t1 = importModel.tasks.single { it.name == "t1" }
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }

    assertThat(s1.start).isEqualTo(expectedStartDate)
    assertThat(s1.end).isEqualTo(expectedEndDate)
  }

  private fun verifyCalendarDaysMondayFriday(projectFile: ProjectFile) {
    assertThat(projectFile.defaultCalendar.isWorkingDay(MONDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(TUESDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(WEDNESDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(THURSDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(FRIDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(SATURDAY)).isFalse()
    assertThat(projectFile.defaultCalendar.isWorkingDay(SUNDAY)).isFalse()

    // Start of week is not saved in P6 files (only locally in the application)
    if (isMppOrMSPDI(projectFile)) {
      assertThat(projectFile.projectProperties.weekStartDay).isEqualTo(MONDAY)
    }
  }

  private fun verifyCalendarDaysTuesdayThursday(projectFile: ProjectFile) {
    assertThat(projectFile.defaultCalendar.isWorkingDay(MONDAY)).isFalse()
    assertThat(projectFile.defaultCalendar.isWorkingDay(TUESDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(WEDNESDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(THURSDAY)).isTrue()
    assertThat(projectFile.defaultCalendar.isWorkingDay(FRIDAY)).isFalse()
    assertThat(projectFile.defaultCalendar.isWorkingDay(SATURDAY)).isFalse()
    assertThat(projectFile.defaultCalendar.isWorkingDay(SUNDAY)).isFalse()

    // Start of week is not saved in P6 files (only locally in the application)
    if (isMppOrMSPDI(projectFile)) {
      assertThat(projectFile.projectProperties.weekStartDay).isEqualTo(WEDNESDAY)
    }
  }

  private fun verifyExportedWorkAreasSharedValues(
      importModel: ImportModel,
  ) {
    assertThat(importModel.workAreas).hasSize(1)

    val w1 = importModel.workAreas[0]
    assertThat(w1.name).isEqualTo("w1")
    validateExportedIds(w1, "w1", 1, 1)

    assertContainsOnlyRmsPlaceholderCraft(importModel)

    assertThat(importModel.tasks).hasSize(1)
    val t1 = importModel.tasks.single { it.workAreaId == w1.id }
    assertThat(t1.name).isEqualTo("t1")
    validateExportedIds(t1, "t1", 2, 2)

    assertThat(importModel.taskSchedules).hasSize(1)
    val s1 = importModel.taskSchedules.single { it.taskId == t1.id }
    validateExportedIds(s1, "t1", 2, 2)
  }

  private fun moFrWeekAvro() =
      listOf(
          DayEnumAvro.MONDAY,
          DayEnumAvro.TUESDAY,
          DayEnumAvro.WEDNESDAY,
          DayEnumAvro.THURSDAY,
          DayEnumAvro.FRIDAY)
}
