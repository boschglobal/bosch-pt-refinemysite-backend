/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.job.handler

import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.job.facade.listener.JobQueuedEventHandler
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.job.integration.toJsonSerializedObject
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.reschedule.command.handler.RescheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType
import com.bosch.pt.iot.smartsite.project.reschedule.facade.job.dto.RescheduleJobType.PROJECT_RESCHEDULE
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Profile("!restore-db")
@Component
open class RescheduleJobHandler(
    private val jobJsonSerializer: JobJsonSerializer,
    private val rescheduleCommandHandler: RescheduleCommandHandler
) : JobQueuedEventHandler {

  override fun handles(job: JobQueuedEventAvro) =
      job.jobType in RescheduleJobType.values().map { it.name }

  /**
   * The maximum time between kafka consumer polls is 5 minutes (by default). This need to be
   * considered when setting the max attempts for retries here in combination with the max read time
   * configured by the [RestTemplateConfiguration]. Otherwise, we might run in an endless loop in
   * the kafka job listener when the time use for all retries is longer than the configured maximum
   * allowed between polls.
   */
  @Retryable(backoff = Backoff(5000), maxAttempts = 2)
  override fun handle(job: JobQueuedEventAvro) =
      when (job.jobType) {
        PROJECT_RESCHEDULE.name -> handleRescheduleJob(job)
        else -> error("Unknown reschedule job type received.")
      }

  private fun handleRescheduleJob(event: JobQueuedEventAvro) =
      rescheduleCommandHandler.handle(event.toRescheduleCommand())

  private fun JobQueuedEventAvro.toRescheduleCommand() =
      jobJsonSerializer.deserialize(jsonSerializedCommand.toJsonSerializedObject())
          as RescheduleCommand
}
