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
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NOTES_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportMilestoneWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

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
              "milestone-name-too-long.pp",
              "milestone-name-too-long.mpp",
              "milestone-name-too-long.xer",
              "milestone-name-too-long-ms.xml",
              "milestone-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify info message for a milestone with too long name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element?.length).isGreaterThan(Milestone.MAX_NAME_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_MILESTONE_NAME_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  // In P6 you cannot create milestones without names
  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-without-name.pp",
              "milestone-without-name.mpp",
              "milestone-without-name-ms.xml"])
  @ParameterizedTest
  fun `verify info message for a milestone without name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element).isEqualTo("Unnamed Milestone")
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_MILESTONE_NAME_EMPTY_DEFAULT_SET)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  // There doesn't seem to be a "notes" column in P6 and PP, therefore we don't test it.
  @FileSource(
      container = "project-import-testdata",
      files = ["milestone-notes-too-long.mpp", "milestone-notes-too-long-ms.xml"])
  @ParameterizedTest
  fun `verify info message for a milestone with notes too long`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element?.length).isGreaterThan(Milestone.MAX_DESCRIPTION_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_MILESTONE_NOTES_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }
}
