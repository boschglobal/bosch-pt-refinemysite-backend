/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.facade.job

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.project.copy.boundary.ProjectCopyParameters
import com.bosch.pt.iot.smartsite.project.copy.facade.job.handler.ProjectCopyJobHandler
import com.bosch.pt.iot.smartsite.project.copy.facade.job.submitter.ProjectCopyJobSubmitter
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteMockKTest
@EnableAllKafkaListeners
class ProjectCopyJobsIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobSubmitter: ProjectCopyJobSubmitter

  @Autowired private lateinit var projectCopyJobHandler: ProjectCopyJobHandler

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()
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
  fun `verify copy project job is handled successfully`() {
    jobSubmitter.enqueueCopyJob(project.identifier.identifier, copyParameters).apply {
      assertJobCommandsAreSentForSuccessfulJob(this)
    }
  }

  @Test
  fun `verify job fails for invalid job event type`() {
    val queueEvent = JobQueuedEventAvro().apply { this.jobType = "UnexpectedImportJobType" }
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { projectCopyJobHandler.handle(queueEvent) }
        .withMessage("Unknown import job type received.")
  }

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
        assertThat(this.serializedResult.json.contains("New project name"))
      }
    }
  }

  companion object {
    private val copyParameters =
        ProjectCopyParameters(
            "New project name",
            workingAreas = true,
            disciplines = true,
            milestones = true,
            tasks = true,
            dayCards = true,
            keepTaskStatus = true,
            keepTaskAssignee = true)
  }
}
