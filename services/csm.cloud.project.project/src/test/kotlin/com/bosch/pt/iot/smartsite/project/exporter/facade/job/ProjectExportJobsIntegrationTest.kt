/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.facade.job

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.deleteSubjectFromWhitelistOfFeature
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.FailJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.BlobTestConfiguration
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.EXPORT_IMPOSSIBLE_FEATURE_DEACTIVATED
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.PRIMAVERA_P6_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.handler.ProjectExportJobHandler
import com.bosch.pt.iot.smartsite.project.exporter.facade.job.submitter.ProjectExportJobSubmitter
import com.bosch.pt.iot.smartsite.project.exporter.submitProjectExportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import java.io.IOException
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteMockKTest
@EnableAllKafkaListeners
class ProjectExportJobsIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobSubmitter: ProjectExportJobSubmitter

  /** The test context contains a mock bean for this dependency. See [BlobTestConfiguration] */
  @Autowired private lateinit var downloadBlobStorageRepository: DownloadBlobStorageRepository

  @Autowired private lateinit var projectExportJobHandler: ProjectExportJobHandler

  @SpykBean private lateinit var projectExportService: ProjectExportService

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData().submitProjectExportFeatureToggle()
    setAuthentication("userCsm1")

    useOnlineListener()

    commandSendingService.apply {
      // The job service is simulated by the CommandSendingServiceTestDouble.
      // It generates the JobQueuedEvent and therewith triggers the job handler.
      activateJobServiceSimulation()
      clearRecords()
    }
  }

  @Test
  fun `verify export project job is handled successfully`() {
    jobSubmitter
        .enqueueExportJob(project.identifier.identifier, exportParameters(PRIMAVERA_P6_XML))
        .apply { assertJobCommandsAreSentForSuccessfulJob(this) }
  }

  @Test
  fun `verify job fails when internal processing fails`() {
    every { projectExportService.getProjectWriter(any()) }.throws(IllegalArgumentException("Error"))

    jobSubmitter
        .enqueueExportJob(project.identifier.identifier, exportParameters(MS_PROJECT_XML))
        .apply { assertJobCommandsAreSentForFailedJob(this) }
  }

  @Test
  fun `verify job fails when feature toggle is inactive`() {
    // Disable feature toggle
    eventStreamGenerator.deleteSubjectFromWhitelistOfFeature("projectExport") {
      it.featureName = PROJECT_EXPORT.name
      it.subjectRef = getIdentifier("company").toString()
      it.type = SubjectTypeEnum.COMPANY.name
    }

    // Check submit is rejected
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          jobSubmitter.enqueueExportJob(
              project.identifier.identifier, exportParameters(MS_PROJECT_XML))
        }
        .withMessageKey(EXPORT_IMPOSSIBLE_FEATURE_DEACTIVATED)

    assertThat(commandSendingService.capturedRecords).isEmpty()
  }

  @Test
  fun `verify job fails when azure storage account call fails`() {
    every { downloadBlobStorageRepository.save(any(), any(), any(), any()) }
        .throws(IOException("Error"))

    jobSubmitter
        .enqueueExportJob(project.identifier.identifier, exportParameters(MS_PROJECT_XML))
        .apply { assertJobCommandsAreSentForFailedJob(this) }
  }

  @Test
  fun `verify job fails for invalid job event type`() {
    val queueEvent = JobQueuedEventAvro().apply { this.jobType = "UnexpectedImportJobType" }
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { projectExportJobHandler.handle(queueEvent) }
        .withMessage("Unknown import job type received.")
  }

  private fun exportParameters(format: ProjectExportFormatEnum) =
      ProjectExportParameters(format, includeMilestones = false, includeComments = false)

  private fun assertJobCommandsAreSentForSuccessfulJob(jobIdentifier: UUID) {
    commandSendingService.capturedRecords.apply {
      assertThat(this).hasSize(3)
      assertThat(this[0].value).isInstanceOf(EnqueueJobCommandAvro::class.java)
      assertThat(this[1].value).isInstanceOf(StartJobCommandAvro::class.java)
      (this[1].value as StartJobCommandAvro).apply {
        assertThat(this.aggregateIdentifier.identifier).isEqualTo(jobIdentifier.toString())
      }
      assertThat(this[2].value).isInstanceOf(CompleteJobCommandAvro::class.java)
      (this[2].value as CompleteJobCommandAvro).apply {
        assertThat(this.aggregateIdentifier.identifier).isEqualTo(jobIdentifier.toString())
      }
    }
  }

  private fun assertJobCommandsAreSentForFailedJob(jobIdentifier: UUID) {
    commandSendingService.capturedRecords.apply {
      assertThat(this).hasSize(3)
      assertThat(this[0].value).isInstanceOf(EnqueueJobCommandAvro::class.java)
      assertThat(this[1].value).isInstanceOf(StartJobCommandAvro::class.java)
      (this[1].value as StartJobCommandAvro).apply {
        assertThat(this.aggregateIdentifier.identifier).isEqualTo(jobIdentifier.toString())
      }
      assertThat(this[2].value).isInstanceOf(FailJobCommandAvro::class.java)
      (this[2].value as FailJobCommandAvro).apply {
        assertThat(this.aggregateIdentifier.identifier).isEqualTo(jobIdentifier.toString())
      }
    }
  }
}
