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
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft.Companion.MAX_NAME_LENGTH
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportCraftWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

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
              "craft-name-too-long.mpp",
              "craft-name-too-long.pp",
              "craft-name-too-long.xer",
              "craft-name-too-long-p6.xml",
              "craft-name-too-long-ms.xml",
          ])
  @ParameterizedTest
  fun `verify info message for a craft with too long name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)
    val analysisColumn = analysisColumn("Discipline", file, true)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, analysisColumn, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(INFO)
      assertThat(this[0].element?.length).isGreaterThan(MAX_NAME_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_CRAFT_NAME_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }
}
