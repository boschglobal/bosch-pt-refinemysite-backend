/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_ALREADY_RUNNING
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_EXISTING_DATA
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_MALICIOUS_FILE
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType.TASK_FIELD
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.IN_PROGRESS
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.PLANNING
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.MALICIOUS
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.NOT_SCANNED
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.SAFE
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import io.mockk.every
import io.mockk.verify
import java.time.LocalDateTime
import net.sf.mpxj.FieldType
import net.sf.mpxj.TaskField
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ProjectImportServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectImportService

  @Autowired private lateinit var projectImportRepository: ProjectImportRepository

  @Autowired private lateinit var blobStorageRepository: ImportBlobStorageRepository

  private var counter = 2L

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
              "craft-41-different-crafts.pp",
              "craft-41-different-crafts.mpp",
              "craft-41-different-crafts.xer",
              "craft-41-different-crafts-ms.xml",
              "craft-41-different-crafts-p6.xml"])
  @ParameterizedTest
  fun `verify upload is possible for allowed file types`(resource: Resource) {
    every { blobStorageRepository.read(any()) } returns resource.inputStream
    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns SAFE

    assertThat(
            cut.upload(
                getIdentifier("p2").asProjectId(),
                resource.inputStream.readAllBytes(),
                resource.file.name,
                "application/msproject"))
        .isNotNull
  }

  @FileSource(container = "project-import-testdata", files = ["craft-41-different-crafts.mpx"])
  @ParameterizedTest
  fun `verify upload is not possible for unsupported but valid file types`(resource: Resource) {
    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns SAFE

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.upload(
              getIdentifier("p2").asProjectId(),
              resource.inputStream.readAllBytes(),
              resource.file.name,
              "application/msproject")
        }
        .withMessageKey(IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE)
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify upload import not possible existing data`(resource: Resource) {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.upload(
              getIdentifier("project").asProjectId(),
              resource.inputStream.readAllBytes(),
              resource.file.name,
              "application/msproject")
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_EXISTING_DATA)
  }

  @Test
  fun `verify analyze import not possible existing data`() {
    createProjectImport("project")

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.analyze(getIdentifier("project").asProjectId(), false, null, null, ETag.from("0"))
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_EXISTING_DATA)
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify upload with malware`(resource: Resource) {
    every { blobStorageRepository.getMalwareScanResult(any()) } returns MALICIOUS

    val reference = "${counter++}"
    createProject(reference)

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.upload(
              getIdentifier(reference).asProjectId(),
              resource.inputStream.readAllBytes(),
              resource.file.name,
              "application/msproject")
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_MALICIOUS_FILE)

    verify(exactly = 0) { blobStorageRepository.moveFromQuarantine(any()) }
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify upload with malware times out without result or timeout`(resource: Resource) {
    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns NOT_SCANNED

    val reference = "${counter++}"
    createProject(reference)

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.upload(
              getIdentifier(reference).asProjectId(),
              resource.inputStream.readAllBytes(),
              resource.file.name,
              "application/msproject")
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_MALICIOUS_FILE)

    verify(exactly = 0) { blobStorageRepository.moveFromQuarantine(any()) }
    verify(exactly = 1) { blobStorageRepository.getMalwareScanResultBlocking(any()) }
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify upload import already running`(resource: Resource) {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference) { it.status = IN_PROGRESS }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.upload(
              getIdentifier(reference).asProjectId(),
              resource.inputStream.readAllBytes(),
              resource.file.name,
              "application/msproject")
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
  }

  @Test
  fun `verify analyze import already running`() {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference) { it.status = IN_PROGRESS }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.analyze(getIdentifier(reference).asProjectId(), false, null, null, ETag.from("0"))
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify upload to existing import`(resource: Resource) {
    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns SAFE

    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)
    every { blobStorageRepository.find(any()) } returns
        Blob("project", ByteArray(0), BlobMetadata.fromMap(emptyMap()), "application/msproject")
    every { blobStorageRepository.read(any()) } returns resource.inputStream

    val uploadResult =
        cut.upload(
            getIdentifier(reference).asProjectId(),
            resource.inputStream.readAllBytes(),
            resource.file.name,
            "application/msproject")

    assertThat(uploadResult.version).isEqualTo(1L)
    assertThat(uploadResult.columns.map { it.name })
        .isEqualTo(
            listOf("Duration", "Finish", "Indicators", "Resource Names", "Start", "Vorgangsname"))

    verify(exactly = 1) { blobStorageRepository.save(any(), any(), any(), any(), any()) }
    verify(exactly = 1) { blobStorageRepository.moveFromQuarantine(any()) }
    verify(exactly = 1) { blobStorageRepository.deleteIfExists(any()) }
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify analyze import results`(resource: Resource) {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)
    every { blobStorageRepository.find(any()) } returns
        Blob(
            "project",
            resource.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            "application/msproject")

    val analysisResult =
        cut.analyze(
            getIdentifier(reference).asProjectId(),
            false,
            AnalysisColumn(
                (TaskField.DURATION_TEXT as FieldType).name(),
                TASK_FIELD,
                IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN),
            AnalysisColumn(
                (TaskField.FINISH_TEXT as FieldType).name(),
                TASK_FIELD,
                IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN),
            ETag.from("0"))

    assertThat(analysisResult.version).isEqualTo(1L)
    assertThat(analysisResult.craftColumn).isNotNull
    assertThat(analysisResult.workAreaColumn).isNotNull
    assertThat(analysisResult.statistics.milestones).isEqualTo(0)
    assertThat(analysisResult.statistics.crafts).isEqualTo(0)
    assertThat(analysisResult.statistics.workAreas).isEqualTo(0)
    assertThat(analysisResult.statistics.tasks).isEqualTo(1)
    assertThat(analysisResult.statistics.relations).isEqualTo(0)
    assertThat(analysisResult.validationResults).isEmpty()
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify analyze fails with unknown craft column`(resource: Resource) {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)
    every { blobStorageRepository.find(any()) } returns
        Blob(
            "project",
            resource.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            "application/msproject")

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.analyze(
              getIdentifier(reference).asProjectId(),
              false,
              AnalysisColumn("Unknown", TASK_FIELD, IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN),
              AnalysisColumn(
                  (TaskField.FINISH_TEXT as FieldType).name(),
                  TASK_FIELD,
                  IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN),
              ETag.from("0"))
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN)
  }

  @FileSource("project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify analyze fails with unknown working area column`(resource: Resource) {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)
    every { blobStorageRepository.find(any()) } returns
        Blob(
            "project",
            resource.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            "application/msproject")

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.analyze(
              getIdentifier(reference).asProjectId(),
              false,
              AnalysisColumn(
                  (TaskField.DURATION_TEXT as FieldType).name(),
                  TASK_FIELD,
                  IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN),
              AnalysisColumn(
                  "Unknown", TASK_FIELD, IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN),
              ETag.from("0"))
        }
        .extracting("messageKey")
        .isEqualTo(IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN)
  }

  private fun createProject(reference: String) {
    eventStreamGenerator.submitProject(reference).submitParticipantG3("${reference}csm") {
      it.user = getByReference("userCsm1")
      it.role = ParticipantRoleEnumAvro.CSM
    }
  }

  private fun createProjectImport(reference: String, block: ((ProjectImport) -> Unit)? = null) {
    val projectImport =
        ProjectImport(getIdentifier(reference).asProjectId(), "abc", PLANNING, LocalDateTime.now())
    block?.invoke(projectImport)
    projectImportRepository.save(projectImport)
  }
}
