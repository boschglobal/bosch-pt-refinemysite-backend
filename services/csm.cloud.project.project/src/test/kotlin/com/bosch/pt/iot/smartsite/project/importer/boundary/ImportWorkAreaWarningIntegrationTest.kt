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
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WBS_NAME_TOO_LONG
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORKING_AREAS_MAX_AMOUNT_EXCEEDED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_POSITION_VALUE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportWorkAreaWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

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

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "workingArea-from-column-name-too-long.mpp",
              "workingArea-from-column-name-too-long.pp",
              "workingArea-from-column-name-too-long.xer",
              "workingArea-from-column-name-too-long-ms.xml",
              "workingArea-from-column-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify info message for a work area with too long name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)
    val analysisColumn = analysisColumn("Working Area", file, false)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, analysisColumn, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element?.length).isGreaterThan(MAX_WORKAREA_NAME_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_WORK_AREA_NAME_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  @FileSource(container = "project-import-testdata", files = ["wbs-name-too-long.mpp"])
  @ParameterizedTest
  fun `verify error message for wbs name too long`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, true, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(21)
      this.forEach {
        assertThat(it.type).isEqualTo(ERROR)
        assertThat(it.element?.length).isEqualTo(103)
        assertThat(it.messageKey).isEqualTo(IMPORT_VALIDATION_WBS_NAME_TOO_LONG)
      }
    }
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "workingArea-1001-from-column.mpp",
              "workingArea-1001-from-column.pp",
              "workingArea-1001-from-column.xer",
              "workingArea-1001-from-column-ms.xml",
              "workingArea-1001-from-column-p6.xml"])
  @ParameterizedTest
  fun `verify error message for too many work areas`(
      file: Resource,
  ) {
    mockBlobAndProjectImport(projectIdentifier, file)
    val analysisColumn = analysisColumn("Working Area", file, false)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, analysisColumn, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(ERROR)
      assertThat(this[0].element).isNull()
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_WORKING_AREAS_MAX_AMOUNT_EXCEEDED)
      assertThat(this[0].messageArguments.first().toInt())
          .isGreaterThan(MAX_WORKAREA_POSITION_VALUE)
    }
  }
}
