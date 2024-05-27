/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import java.time.DayOfWeek
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportWorkdayConfigurationIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var workdayConfigurationRepository: WorkdayConfigurationRepository

  @Autowired private lateinit var projectController: ProjectController

  private val projectIdentifier = ProjectId()
  private val project by lazy { projectRepository.findOneByIdentifier(projectIdentifier)!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitProjectImportFeatureToggle()

    setAuthentication("userCsm1")
    projectController.createProject(projectIdentifier, createDefaultSaveProjectResource())

    projectEventStoreUtils.reset()
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "workday-configuration-3-days.pp",
              "workday-configuration-3-days.mpp",
              "workday-configuration-3-days.xer",
              "workday-configuration-3-days-ms.xml",
              "workday-configuration-3-days-p6.xml"])
  @ParameterizedTest
  fun `verify work day configuration with 3 days is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val workdayConfiguration =
        workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)

    val threeDayWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)

    assertThat(workdayConfiguration).isNotNull
    assertThat(workdayConfiguration!!.workingDays).isEqualTo(threeDayWeek)
    assertThat(workdayConfiguration.holidays).isEmpty()

    // In power project it is not possible to create tasks on non-working days,
    // therefore it is expected to not allow work in RmS as well.
    val isPP = file.file.name.endsWith(".pp")
    if (isPP) {
      assertThat(workdayConfiguration.allowWorkOnNonWorkingDays).isFalse()
    } else {
      assertThat(workdayConfiguration.allowWorkOnNonWorkingDays).isTrue()
    }

    projectEventStoreUtils
        .verifyContainsAndGet(WorkdayConfigurationEventAvro::class.java, UPDATED, 1, false)
        .map { it.aggregate }
        .all { it.workingDays == threeDayWeek && it.holidays.isEmpty() }

    // The new version of mpxj seem to see an additional task in the power project file
    val expectedNumberOfEvents = if (isPP) 2 else 1

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, expectedNumberOfEvents, false)

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java,
        ExternalIdEventEnumAvro.CREATED,
        expectedNumberOfEvents,
        false)

    projectEventStoreUtils.verifyNumberOfEvents(if (isPP) 10 else 8)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "workday-configuration-workday-tasks.pp",
              "workday-configuration-workday-tasks.mpp",
              "workday-configuration-workday-tasks.xer",
              "workday-configuration-workday-tasks-ms.xml",
              "workday-configuration-workday-tasks-p6.xml"])
  @ParameterizedTest
  fun `verify work day configuration with tasks on working days is imported successfully`(
      file: Resource
  ) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val workdayConfiguration =
        workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)

    val threeDayWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)

    assertThat(workdayConfiguration).isNotNull
    assertThat(workdayConfiguration!!.workingDays).isEqualTo(threeDayWeek)
    assertThat(workdayConfiguration.holidays).isEmpty()
    assertThat(workdayConfiguration.allowWorkOnNonWorkingDays).isFalse()

    projectEventStoreUtils
        .verifyContainsAndGet(WorkdayConfigurationEventAvro::class.java, UPDATED, 1, false)
        .map { it.aggregate }
        .all { it.workingDays == threeDayWeek && it.holidays.isEmpty() }

    val isPP = file.file.name.endsWith(".pp")
    // The new version of mpxj seem to see an additional task in the power project file
    val expectedNumberOfEvents = if (isPP) 2 else 1

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, expectedNumberOfEvents, false)

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java,
        ExternalIdEventEnumAvro.CREATED,
        expectedNumberOfEvents,
        false)

    projectEventStoreUtils.verifyNumberOfEvents(if (isPP) 10 else 8)
  }
}
