/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_HOLIDAYS_MAX_AMOUNT_EXCEEDED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORKDAY_CONFIGURATION_HAS_WORK_ON_NON_WORKDAY
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday.Companion.MAX_HOLIDAY_AMOUNT
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportWorkdayConfigurationWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  private val projectIdentifier by lazy { getIdentifier("p2").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .submitProject("p2")
        .submitParticipantG3("p2Csm1") {
          it.user = getByReference("userCsm1")
          it.role = CSM
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @FileSource(container = "project-import-testdata", files = ["workday-configuration-3-days.mpp"])
  @ParameterizedTest
  fun `verify info message for a workday configuration with tasks on non-working day`(
      file: Resource
  ) {

    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, true, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element).isNull()
      assertThat(this[0].messageKey)
          .isEqualTo(IMPORT_VALIDATION_WORKDAY_CONFIGURATION_HAS_WORK_ON_NON_WORKDAY)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  @FileSource(
      container = "project-import-testdata",
      files = ["workday-configuration-too-many-holidays.mpp"])
  @ParameterizedTest
  fun `verify error message for a workday configuration with too many non-working day exceptions`(
      file: Resource
  ) {

    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, true, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(ERROR)
      assertThat(this[0].element).isNull()
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_HOLIDAYS_MAX_AMOUNT_EXCEEDED)
      assertThat(this[0].messageArguments).hasSize(1).isEqualTo(arrayOf("$MAX_HOLIDAY_AMOUNT"))
    }
  }

  @FileSource(
      container = "project-import-testdata",
      files = ["workday-configuration-duplicate-holiday.xml"])
  @ParameterizedTest
  fun `verify no error message for a workday configuration with duplicate holiday`(file: Resource) {

    val project = mockk<Project>()
    every { project.identifier }.returns(projectIdentifier)
    val importContext = ImportContext(mutableMapOf(), mutableMapOf())
    val projectFile = projectImportService.readProjectFile(file.inputStream)

    val importModel =
        projectReader.read(project, projectFile, importContext, false, null, null, null, null)

    val hasDuplicateHolidays =
        importModel.holidays.groupBy(Holiday::date, Holiday::name).values.any {
          it.size != it.distinctBy(String::lowercase).size
        }
    assertFalse(hasDuplicateHolidays)
  }
}
