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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.InvitationEventListener
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

@Profile("restore-db & !project-invitation-context-restore-snapshots-event-listener-disabled")
@Component
open class ProjectInvitationContextRestoreSnapshotsEventListener(
    transactionTemplate: TransactionTemplate,
    eventBus: ProjectInvitationContextLocalEventBus,
    private val logger: Logger
) : InvitationEventListener, RestoreFromKafkaAdapter(transactionTemplate, eventBus) {

  @Transactional(propagation = NEVER)
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('project-invitation')}"],
      clientIdPrefix = "csm-cloud-project-invitation-restore")
  override fun listenToInvitationEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)
    executeWithAsyncRequestScope { processEvent(record) }
    ack.acknowledge()
  }

  open fun processEvent(consumerRecord: ConsumerRecord<EventMessageKey, SpecificRecordBase?>) {
    // Retry in case of a duplicate key exception.
    // This was introduced because of a race condition between participants and invitations.
    try {
      emit(consumerRecord)
    } catch (ex: PersistenceException) {
      logger.warn(
          "Retry after '${ex.message}' error for record from partition " +
              "${consumerRecord.partition()} and offset ${consumerRecord.offset()}")

      sleep(ThreadLocalRandom.current().nextLong(50, 3000))
      emit(consumerRecord)
    }
  }
}
