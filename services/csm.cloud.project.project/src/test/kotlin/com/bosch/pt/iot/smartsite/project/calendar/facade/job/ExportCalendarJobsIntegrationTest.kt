/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.job

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.FailJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.BlobTestConfiguration
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.pdf.integration.PdfIntegrationService
import com.bosch.pt.iot.smartsite.project.calendar.api.AssigneesFilter
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.facade.job.submitter.CalendarExportJobSubmitter
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.io.IOException
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.client.RestClientException

@SmartSiteMockKTest
@EnableAllKafkaListeners
class ExportCalendarJobsIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble
  @Autowired private lateinit var jobSubmitter: CalendarExportJobSubmitter

  @MockkBean private lateinit var pdfIntegrationService: PdfIntegrationService

  /** The test context contains a mock bean for this dependency. See [BlobTestConfiguration] */
  @Autowired private lateinit var downloadBlobStorageRepository: DownloadBlobStorageRepository

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()
    authorizeWithUser(repositories.findUser(getIdentifier("user")))

    useOnlineListener()

    commandSendingService.apply {
      // The job service is simulated by the CommandSendingServiceTestDouble.
      // It generates the JobQueuedEvent and therewith triggers the job handler.
      activateJobServiceSimulation()
      clearRecords()
    }

    every { pdfIntegrationService.convertToPdf(any(), any(), any(), any()) }
        .answers { ByteArrayResource(ByteArray(0)) }
  }

  @Test
  fun `verify export calendar as pdf job is handled successfully`() {
    jobSubmitter.enqueuePdfExportJob(project.identifier, exportParameters(), BEARER_TOKEN).apply {
      assertJobCommandsAreSentForSuccessfulJob(this)
    }
  }

  @Test
  fun `verify export calendar as json job is handled successfully`() {
    jobSubmitter.enqueueJsonExportJob(project.identifier, exportParameters()).apply {
      assertJobCommandsAreSentForSuccessfulJob(this)
    }
  }

  @Test
  fun `verify export calendar as csv job is handled successfully`() {
    jobSubmitter.enqueueCsvExportJob(project.identifier, exportParameters()).apply {
      assertJobCommandsAreSentForSuccessfulJob(this)
    }
  }

  @Test
  fun `verify job fails when pdf service call fails`() {
    every { pdfIntegrationService.convertToPdf(any(), any(), any(), any()) }
        .throws(RestClientException("Error"))

    jobSubmitter.enqueuePdfExportJob(project.identifier, exportParameters(), BEARER_TOKEN).apply {
      assertJobCommandsAreSentForFailedJob(this)
    }
  }

  @Test
  fun `verify job fails when azure storage account call fails`() {
    every { downloadBlobStorageRepository.save(any(), any(), any(), any()) }
        .throws(IOException("Error"))

    jobSubmitter.enqueuePdfExportJob(project.identifier, exportParameters(), BEARER_TOKEN).apply {
      assertJobCommandsAreSentForFailedJob(this)
    }
  }

  @Test
  fun `verify export to PDF job is not triggered when start date before end date`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      jobSubmitter.enqueuePdfExportJob(
          project.identifier,
          exportParameters().copy(from = project.end, to = project.start),
          BEARER_TOKEN)
    }
  }

  @Test
  fun `verify export to JSON job is not triggered when start date before end date`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      jobSubmitter.enqueueJsonExportJob(
          project.identifier, exportParameters().copy(from = project.end, to = project.start))
    }
  }

  @Test
  fun `verify export to CSV job is not triggered when start date before end date`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      jobSubmitter.enqueueCsvExportJob(
          project.identifier, exportParameters().copy(from = project.end, to = project.start))
    }
  }

  private fun exportParameters() =
      CalendarExportParameters(
          AssigneesFilter(
              listOf(getIdentifier("participant").asParticipantId()),
              listOf(getIdentifier("company"))),
          project.start,
          project.end,
          emptyList(),
          listOf(WorkAreaIdOrEmpty()),
          listOf(*TaskStatusEnum.values()),
          listOf(*TopicCriticalityEnum.values()),
          hasTopics = false,
          includeDayCards = true,
          includeMilestones = true,
          allDaysInDateRange = false)

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

  companion object {
    const val BEARER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dC..."
  }
}
