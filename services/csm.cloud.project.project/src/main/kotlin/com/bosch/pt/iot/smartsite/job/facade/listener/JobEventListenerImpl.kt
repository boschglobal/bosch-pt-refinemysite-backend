/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.job.facade.listener

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.job.event.listener.JobEventListener
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.job.integration.JobIntegrationService
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!restore-db & !kafka-job-listener-disabled")
@Component
open class JobEventListenerImpl(
    private val jobQueuedEventHandler: List<JobQueuedEventHandler>,
    private val jobCompletedEventHandler: List<JobCompletedEventHandler>,
    private val logger: Logger,
    private val userService: UserService,
    private val jobIntegrationService: JobIntegrationService
) : JobEventListener {

  @KafkaListener(
      topics = ["\${custom.kafka.bindings.job-event.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.job-event.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.job-event.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.job-event.concurrency}",
      containerFactory = "kafkaListenerForJobsContainerFactory")
  override fun listenToJobEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)

    // SMAR-18272: Prevents Kafka from retrying JobQueuedEvent again after max.poll.interval.ms
    // if handleJob takes too long
    ack.acknowledge()

    val event = record.value()
    if (event is JobQueuedEventAvro) {
      doWithAuthentication(event.userIdentifier.toUUID()) {
        executeWithAsyncRequestScope { dispatchJobQueuedEvent(event) }
      }
    } else if (event is JobCompletedEventAvro) {
      dispatchJobCompletedEvent(event)
    }
  }

  private fun doWithAuthentication(userIdentifier: UUID, block: () -> Unit) {
    requireNotNull(userService.findOne(userIdentifier)) {
          "User not found for received JobQueuedEvent"
        }
        .also { doWithAuthenticatedUser(it, block) }
  }

  private fun dispatchJobQueuedEvent(event: JobQueuedEventAvro) {
    jobQueuedEventHandler
        .filter { it.handles(event) }
        .let {
          when (it.size) {
            0 -> logger.info("No job handler found to handle queued job")
            1 -> invokeJobHandler(it.first(), event)
            else -> error("More than one job handler found for JobQueuedEvent")
          }
        }
  }

  private fun dispatchJobCompletedEvent(event: JobCompletedEventAvro) {
    jobCompletedEventHandler
        .filter { it.handles(event) }
        .let {
          when (it.size) {
            0 -> logger.info("No job handler found to handle completed job")
            1 -> invokeCompleteHandler(it.first(), event)
            else -> error("More than one job handler found for JobCompletedEvent")
          }
        }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun invokeJobHandler(handler: JobQueuedEventHandler, job: JobQueuedEventAvro) {
    jobIntegrationService.startJob(job.aggregateIdentifier)
    try {
      handler.handle(job).also { jobIntegrationService.completeJob(job.aggregateIdentifier, it) }
    } catch (exception: Exception) {
      logger.error(
          "Job handling failed for job ${job.aggregateIdentifier.identifier}: ${exception.message}",
          exception)
      jobIntegrationService.failJob(job.aggregateIdentifier)
    }
  }

  private fun invokeCompleteHandler(handler: JobCompletedEventHandler, job: JobCompletedEventAvro) {
    try {
      handler.handle(job)
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
      logger.error(
          "Job completion failed for job ${job.aggregateIdentifier.identifier}: ${exception.message}",
          exception)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(JobEventListenerImpl::class.java)
  }
}
