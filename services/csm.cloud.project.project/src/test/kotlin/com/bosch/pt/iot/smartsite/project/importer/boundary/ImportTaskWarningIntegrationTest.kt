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
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_CRAFT_ADDITIONAL_VALUES_NOT_CONSIDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NOTES_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_WORKING_AREA_ADDITIONAL_VALUES_NOT_CONSIDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.RESOURCE
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.TASK_FIELD
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import net.sf.mpxj.FieldType
import net.sf.mpxj.TaskField
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportTaskWarningIntegrationTest : AbstractImportWarningIntegrationTest() {

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
          it.role = ParticipantRoleEnumAvro.CSM
        }
    setAuthentication("userCsm1")

    projectEventStoreUtils.reset()
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-empty-name.pp",
              "task-with-empty-name.mpp",
              "task-with-empty-name.xer",
              "task-with-empty-name-ms.xml",
              "task-with-empty-name-p6.xml"])
  @ParameterizedTest
  fun `verify info message for a task with empty name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(ValidationResultType.INFO)
      assertThat(this[0].element).isEqualTo("Unnamed Task")
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_TASK_NAME_EMPTY_DEFAULT_SET)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-name-too-long.pp",
              "task-with-name-too-long.mpp",
              "task-with-name-too-long.xer",
              "task-with-name-too-long-ms.xml",
              "task-with-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify info message for a task with too long name`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(ValidationResultType.INFO)
      assertThat(this[0].element?.length).isGreaterThan(Task.MAX_NAME_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_TASK_NAME_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  // There doesn't seem to be a "notes" column in P6, therefore we don't test it.
  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-note-too-long.pp",
              "task-with-note-too-long.mpp",
              "task-with-note-too-long-ms.xml"])
  @ParameterizedTest
  fun `verify info message for a task with too long notes`(file: Resource) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val analysisResult =
        projectImportService.analyze(projectIdentifier, false, null, null, ETag.from("0"))

    analysisResult.validationResults.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(ValidationResultType.INFO)
      assertThat(this[0].element?.length).isGreaterThan(Task.MAX_DESCRIPTION_LENGTH)
      assertThat(this[0].messageKey).isEqualTo(IMPORT_VALIDATION_TASK_NOTES_WILL_BE_SHORTENED)
      assertThat(this[0].messageArguments).isEmpty()
    }
  }

  @FileSource(container = "project-import-testdata", files = ["task-with-resources.mpp"])
  @ParameterizedTest
  fun `verify warnings are generated for tasks with multiple crafts Project`(file: Resource) {
    val column =
        AnalysisColumn(
            (TaskField.RESOURCE_NAMES as FieldType).name(),
            TASK_FIELD,
            IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN)
    `verify warnings are generated for tasks with multiple crafts`(file, column)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-resources.pp",
              "task-with-resources.xer",
              "task-with-resources-ms.xml",
              "task-with-resources-p6.xml"])
  @ParameterizedTest
  fun `verify warnings are generated for tasks with multiple crafts P6`(file: Resource) {
    val column = AnalysisColumn("Resources", RESOURCE, IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN)
    `verify warnings are generated for tasks with multiple crafts`(file, column)
  }

  private fun `verify warnings are generated for tasks with multiple crafts`(
      file: Resource,
      column: AnalysisColumn
  ) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val result =
        projectImportService.analyze(projectIdentifier, false, column, null, ETag.from("0"))

    val isPP = file.file.name.endsWith(".pp")
    assertThat(result.statistics.crafts).isEqualTo(3) // 2 from file + 1 placeholder
    assertThat(result.statistics.milestones).isEqualTo(0)
    assertThat(result.statistics.relations).isEqualTo(0)
    assertThat(result.statistics.tasks).isEqualTo(if (isPP) 5 else 4)
    assertThat(result.statistics.workAreas).isEqualTo(0)

    checkValidationErrors(
        result.validationResults, IMPORT_VALIDATION_TASK_CRAFT_ADDITIONAL_VALUES_NOT_CONSIDERED)
  }

  @FileSource(container = "project-import-testdata", files = ["task-with-resources.mpp"])
  @ParameterizedTest
  fun `verify warnings are generated for tasks with multiple work areas Project`(file: Resource) {
    val column =
        AnalysisColumn(
            (TaskField.RESOURCE_NAMES as FieldType).name(),
            TASK_FIELD,
            IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN)
    `verify warnings are generated for tasks with multiple work areas`(file, column)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-resources.pp",
              "task-with-resources.xer",
              "task-with-resources-ms.xml",
              "task-with-resources-p6.xml"])
  @ParameterizedTest
  fun `verify warnings are generated for tasks with multiple work areas P6`(file: Resource) {
    val column =
        AnalysisColumn("Resources", RESOURCE, IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN)
    `verify warnings are generated for tasks with multiple work areas`(file, column)
  }

  private fun `verify warnings are generated for tasks with multiple work areas`(
      file: Resource,
      column: AnalysisColumn
  ) {
    mockBlobAndProjectImport(projectIdentifier, file)

    val result =
        projectImportService.analyze(projectIdentifier, false, null, column, ETag.from("0"))

    val isPP = file.file.name.endsWith(".pp")
    assertThat(result.statistics.crafts).isEqualTo(0)
    assertThat(result.statistics.milestones).isEqualTo(0)
    assertThat(result.statistics.relations).isEqualTo(0)
    assertThat(result.statistics.tasks).isEqualTo(if (isPP) 5 else 4)
    assertThat(result.statistics.workAreas).isEqualTo(2)

    checkValidationErrors(
        result.validationResults,
        IMPORT_VALIDATION_TASK_WORKING_AREA_ADDITIONAL_VALUES_NOT_CONSIDERED)
  }

  private fun checkValidationErrors(validationErrors: List<ValidationResult>, messageKey: String) {
    assertThat(validationErrors).hasSize(2)

    val first = validationErrors[0]
    assertThat(first.type).isEqualTo(ValidationResultType.INFO)
    assertThat(first.element).isEqualTo("Mason, Painter")
    assertThat(first.messageKey).isEqualTo(messageKey)
    assertThat(first.messageArguments).isEmpty()

    val second = validationErrors[1]
    assertThat(second.type).isEqualTo(ValidationResultType.INFO)
    assertThat(second.element).isEqualTo("Carpenter, Mason, Painter, Roofer")
    assertThat(second.messageKey).isEqualTo(messageKey)
    assertThat(second.messageArguments).isEmpty()
  }
}
