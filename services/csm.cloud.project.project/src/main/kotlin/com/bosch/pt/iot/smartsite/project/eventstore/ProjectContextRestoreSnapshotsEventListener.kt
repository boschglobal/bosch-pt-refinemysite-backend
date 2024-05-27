/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.eventstore.RestoreFromKafkaAdapter
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import datadog.trace.api.Trace
import java.lang.IllegalArgumentException
import java.util.concurrent.ThreadLocalRandom
import jakarta.persistence.PersistenceException
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.utils.Utils.sleep
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Profile("restore-db & !project-context-restore-snapshot-event-listener-disabled")
@Component
open class ProjectContextRestoreSnapshotsEventListener(
    transactionTemplate: TransactionTemplate,
    eventBus: ProjectContextLocalEventBus,
    @Deprecated("can be removed when all aggregates have been migrated to arch 2.0")
    private val projectRestoreDbStrategyDispatcher:
        RestoreDbStrategyDispatcher<ProjectContextRestoreDbStrategy>,
    private val logger: Logger
) : ProjectEventListener, RestoreFromKafkaAdapter(transactionTemplate, eventBus) {

  @Trace
  @Transactional(propagation = NEVER)
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('project')}"],
      clientIdPrefix = "csm-cloud-project-restore")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)
    executeWithAsyncRequestScope { processEvent(record) }
    ack.acknowledge()
  }

  private fun processEvent(consumerRecord: ConsumerRecord<EventMessageKey, SpecificRecordBase?>) {
    // Retry in case of a duplicate key exception.
    // This was introduced because of a race condition between participants and invitations.
    try {
      // call can be replaced with call to emit() of event bus once all aggregates have been
      // migrated to the arch 2.0
      emitToDeprecatedRestoreDispatcherOrFallBackToEventBus(consumerRecord)
    } catch (ex: PersistenceException) {
      logger.warn(
          "Retry after '${ex.message}' error for record from partition " +
              "${consumerRecord.partition()} and offset ${consumerRecord.offset()}",
      )
      sleep(ThreadLocalRandom.current().nextLong(50, 3000))
      emitToDeprecatedRestoreDispatcherOrFallBackToEventBus(consumerRecord)
    }
  }

  @Suppress("SwallowedException")
  private fun emitToDeprecatedRestoreDispatcherOrFallBackToEventBus(
      consumerRecord: ConsumerRecord<EventMessageKey, SpecificRecordBase?>
  ) {
    try {
      projectRestoreDbStrategyDispatcher.dispatch(consumerRecord)
    } catch (ex: IllegalArgumentException) {
      logger.info("No old restore strategy found, event processed via new event bus")
      emit(consumerRecord)
    }
  }
}
