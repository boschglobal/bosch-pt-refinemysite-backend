/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.job.submitter

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_ALREADY_RUNNING
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_EXISTING_DATA
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobContext
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobType.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.FAILED
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.IN_PROGRESS
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ProjectImportJobSubmitterIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectImportJobSubmitter

  @Autowired private lateinit var projectImportRepository: ProjectImportRepository

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var blobStorageRepository: ImportBlobStorageRepository

  @MockkBean private lateinit var jobIntegrationService: JobIntegrationService

  private var counter = 2L

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
    setAuthentication("userCsm1")

    every { blobStorageRepository.find(any()) } returns
        Blob(
            "project",
            ByteArray(0),
            BlobMetadata.fromMap(mapOf("filename" to "my-project.mpp")),
            "application/msproject")
  }

  @Test
  fun `verify enqueue import works as expected`() {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)

    val project = projectRepository.findOneByIdentifier(getIdentifier(reference).asProjectId())!!

    val expectedJobId = randomUUID()
    every { jobIntegrationService.enqueueJob(any(), any(), any(), any(), any()) } answers
        {
          assertThat(it.invocation.args[0]).isEqualTo(PROJECT_IMPORT.name)
          assertThat(it.invocation.args[1])
              .isEqualTo(SecurityContextHelper.getInstance().getCurrentUser().identifier!!)
          val projectImportContext = it.invocation.args[2] as ProjectImportJobContext
          projectImportContext.apply {
            assertThat(this.project.identifier.asProjectId()).isEqualTo(project.identifier)
            assertThat(this.fileName).isEqualTo("my-project.mpp")
          }
          assertThat((it.invocation.args[3] as ProjectImportCommand).projectIdentifier)
              .isEqualTo(project.identifier)
          expectedJobId
        }

    val jobId = cut.enqueueImportJob(getIdentifier(reference).asProjectId(), ETag.from(0))
    assertThat(jobId).isEqualTo(expectedJobId)

    val import =
        projectImportRepository.findOneByProjectIdentifier(getIdentifier(reference).asProjectId())!!
    assertThat(import.status).isEqualTo(IN_PROGRESS)
  }

  @Test
  fun `verify enqueue import not possible if kafka rejects submit`() {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference)

    val expectedMessage = "Cannot enqueue message"
    every { jobIntegrationService.enqueueJob(any(), any(), any(), any(), any()) } throws
        IllegalArgumentException(expectedMessage)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.enqueueImportJob(getIdentifier(reference).asProjectId(), ETag.from(0)) }
        .withMessage(expectedMessage)

    val import =
        projectImportRepository.findOneByProjectIdentifier(getIdentifier(reference).asProjectId())!!
    assertThat(import.status).isEqualTo(FAILED)
  }

  @Test
  fun `verify enqueue not possible existing data`() {
    createProjectImport("project")

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.enqueueImportJob(getIdentifier("project").asProjectId(), ETag.from(0)) }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_EXISTING_DATA)
  }

  @Test
  fun `verify enqueue not possible already running`() {
    val reference = "${counter++}"
    createProject(reference)
    createProjectImport(reference) { it.status = IN_PROGRESS }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.enqueueImportJob(getIdentifier(reference).asProjectId(), ETag.from(0)) }
        .extracting("messageKey")
        .isEqualTo(IMPORT_IMPOSSIBLE_ALREADY_RUNNING)
  }

  private fun createProject(reference: String) {
    eventStreamGenerator.submitProject(reference).submitParticipantG3("${reference}csm") {
      it.user = getByReference("userCsm1")
      it.role = ParticipantRoleEnumAvro.CSM
    }
  }

  private fun createProjectImport(reference: String, block: ((ProjectImport) -> Unit)? = null) {
    val projectImport =
        ProjectImport(
            getIdentifier(reference).asProjectId(),
            "abc",
            ProjectImportStatus.PLANNING,
            LocalDateTime.now())
    block?.invoke(projectImport)
    projectImportRepository.save(projectImport)
  }
}
