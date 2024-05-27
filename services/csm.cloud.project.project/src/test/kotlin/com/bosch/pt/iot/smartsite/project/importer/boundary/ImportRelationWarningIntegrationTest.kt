/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
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
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_RELATION_UNSUPPORTED
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportRelationWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

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
    LocaleContextHolder.setLocale(null)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-3-non-finish-to-start-relations.pp",
              "relation-3-non-finish-to-start-relations.mpp",
              "relation-3-non-finish-to-start-relations.xer",
              "relation-3-non-finish-to-start-relations-ms.xml",
              "relation-3-non-finish-to-start-relations-p6.xml",
          ])
  @ParameterizedTest
  fun `verify info messages for skipping non-finish-to-start relations`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(3)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element).isEqualTo("Start-to-Start: “task01“ → “task02“")
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED)
      assertThat(this[0].messageArguments).isEmpty()

      assertThat(this[1].type).isEqualTo(INFO)
      assertThat(this[1].element).isEqualTo("Finish-to-Finish: “task02“ → “task03“")
      assertThat(this[1].messageKey).isEqualTo(IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED)
      assertThat(this[1].messageArguments).isEmpty()

      assertThat(this[2].type).isEqualTo(INFO)
      assertThat(this[2].element).isEqualTo("Start-to-Finish: “task03“ → “task04“")
      assertThat(this[2].messageKey).isEqualTo(IMPORT_VALIDATION_RELATION_TYPE_UNSUPPORTED)
      assertThat(this[2].messageArguments).isEmpty()
    }
  }

  // Relations between tasks and work areas are not possible in P6 and PP as the hierarchy is
  // applied as WBS and cannot be referenced in relations.
  @FileSource(
      container = "project-import-testdata",
      files = ["relation-task-workarea.mpp", "relation-task-workarea-ms.xml"])
  @ParameterizedTest
  fun `verify info messages for skipping unsupported relations`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, true, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element).isEqualTo("workarea1 → task2")
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_RELATION_UNSUPPORTED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }
}
