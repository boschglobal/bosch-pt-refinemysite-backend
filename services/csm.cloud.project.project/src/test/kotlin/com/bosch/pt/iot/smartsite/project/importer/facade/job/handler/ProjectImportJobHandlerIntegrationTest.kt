/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.job.handler

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobContext
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobType.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.DONE
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.FAILED
import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImportStatus.IN_PROGRESS
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.ProjectImportRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import io.mockk.every
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.nameUUIDFromBytes
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ProjectImportJobHandlerIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: ProjectImportJobHandler

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var projectImportRepository: ProjectImportRepository

  @Autowired private lateinit var blobStorageRepository: ImportBlobStorageRepository

  @Autowired private lateinit var projectController: ProjectController

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  private var counter = 2L

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().setupDatasetTestData()
    setAuthentication("userCsm1")
  }

  @Test
  fun `verifies handles job queued event`() {
    val reference = nameUUIDFromBytes("${counter++}".encodeToByteArray())
    createProject(reference)
    val project = projectRepository.findOneByIdentifier(reference.asProjectId())!!
    val import = createProjectImport(reference) { it.status = IN_PROGRESS }

    val jobId = randomUUID()
    val event = jobQueuedEvent(jobId, import, project)

    assertThat(cut.handles(event)).isTrue
  }

  @Test
  fun `verifies handles job completed event`() {
    val reference = nameUUIDFromBytes("${counter++}".encodeToByteArray())
    createProject(reference)

    val jobId = randomUUID()
    val import =
        createProjectImport(reference) {
          it.status = IN_PROGRESS
          it.jobId = jobId
        }

    val event = jobCompletedEvent(jobId, import)

    assertThat(cut.handles(event)).isTrue
  }

  @FileSource(container = "project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verifies handle job queued event`(file: Resource) {
    every { blobStorageRepository.find(any()) } returns
        Blob(
            "project",
            file.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            "application/msproject")

    val reference = nameUUIDFromBytes("${counter++}".encodeToByteArray())
    createProject(reference)
    val project = projectRepository.findOneByIdentifier(reference.asProjectId())!!
    val import = createProjectImport(reference) { it.status = IN_PROGRESS }

    val event = jobQueuedEvent(randomUUID(), import, project)
    cut.handle(event)

    val updatedImport =
        projectImportRepository.findOneByProjectIdentifier(reference.asProjectId())!!
    assertThat(updatedImport.status).isEqualTo(DONE)
  }

  @Test
  fun `verifies handle job queued event fails for invalid file`() {
    every { blobStorageRepository.find(any()) } returns
        Blob("project", ByteArray(0), BlobMetadata.fromMap(emptyMap()), "application/msproject")

    val reference = nameUUIDFromBytes("${counter++}".encodeToByteArray())
    createProject(reference)
    val project = projectRepository.findOneByIdentifier(reference.asProjectId())!!
    val import = createProjectImport(reference) { it.status = IN_PROGRESS }

    val event = jobQueuedEvent(randomUUID(), import, project)
    try {
      cut.handle(event)
    } catch (e: PreconditionViolationException) {
      assertThat(e.messageKey).isEqualTo(IMPORT_IMPOSSIBLE_UNSUPPORTED_FILE_TYPE)
    }

    val updatedImport =
        projectImportRepository.findOneByProjectIdentifier(reference.asProjectId())!!
    assertThat(updatedImport.status).isEqualTo(FAILED)
  }

  @Test
  fun `verifies handle job completed event`() {
    val reference = nameUUIDFromBytes("${counter++}".encodeToByteArray())
    createProject(reference)

    val jobId = randomUUID()
    val import =
        createProjectImport(reference) {
          it.status = IN_PROGRESS
          it.jobId = jobId
        }

    val event = jobCompletedEvent(jobId, import)

    cut.handle(event)
    assertThat(projectImportRepository.findOneByProjectIdentifier(reference.asProjectId())).isNull()
  }

  private fun jobQueuedEvent(
      jobId: UUID,
      import: ProjectImport,
      project: Project
  ): JobQueuedEventAvro =
      JobQueuedEventAvro.newBuilder()
          .setAggregateIdentifier(
              AggregateIdentifierAvro(jobId.toString(), import.version, JOB.name))
          .setJobType(PROJECT_IMPORT.name)
          .setUserIdentifier(getIdentifier("userCsm1").toString())
          .setTimestamp(LocalDateTime.now().toEpochMilli())
          .setJsonSerializedContext(
              jobJsonSerializer
                  .serialize(
                      ProjectImportJobContext(
                          ResourceReference.from(project), "my-project-file.mpp"))
                  .toAvro())
          .setJsonSerializedCommand(
              jobJsonSerializer
                  .serialize(
                      ProjectImportCommand(LocaleContextHolder.getLocale(), project.identifier))
                  .toAvro())
          .build()

  private fun jobCompletedEvent(
      jobId: UUID,
      import: ProjectImport,
  ): JobCompletedEventAvro =
      JobCompletedEventAvro.newBuilder()
          .setAggregateIdentifier(
              AggregateIdentifierAvro(jobId.toString(), import.version, JOB.name))
          .setTimestamp(LocalDateTime.now().toEpochMilli())
          .setSerializedResult(jobJsonSerializer.serialize(Unit).toAvro())
          .build()

  private fun createProject(reference: UUID) {
    projectController.createProject(
        ProjectId(reference),
        SaveProjectResource(
            client = "client",
            description = "description",
            start = LocalDate.now(),
            end = LocalDate.now().plus(1, ChronoUnit.DAYS),
            projectNumber = "projectNumber",
            title = reference.toString(),
            category = ProjectCategoryEnum.OB,
            address = ProjectAddressDto("city", "HN", "street", "ZC")))
  }

  private fun createProjectImport(
      reference: UUID,
      block: ((ProjectImport) -> Unit)? = null
  ): ProjectImport {
    val projectImport =
        ProjectImport(
            reference.asProjectId(), "abc", ProjectImportStatus.PLANNING, LocalDateTime.now())
    block?.invoke(projectImport)
    return projectImportRepository.save(projectImport)
  }
}
