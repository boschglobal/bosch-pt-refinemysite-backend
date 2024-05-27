/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDeletedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDisabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureEnabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureWhitelistActivatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectAddedToWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectDeletedFromWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.event.listener.FeatureEventListener
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureProjector
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-feature-projector-listener-disabled")
@ConditionalOnProperty(value = ["custom.feature.enabled"], havingValue = "true")
@Component
internal class UpstreamFeatureEventListener(
    private val logger: Logger,
    private val projector: FeatureProjector
) : FeatureEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.feature.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.feature-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.feature-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.feature-projector.concurrency:1}",
      containerFactory = "featuretoggleKafkaListenerContainerFactory",
  )
  // key is Any to work with avro and mapped classes in case interceptor is configured
  fun listen(record: ConsumerRecord<Any, SpecificRecordBase?>) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey || key is MessageKeyAvro) {
      when (event) {
        is FeatureCreatedEventAvro -> projector.handle(event.event())
        is FeatureDeletedEventAvro -> projector.handle(event.event())
        is FeatureWhitelistActivatedEventAvro -> projector.handle(event.event())
        is FeatureDisabledEventAvro -> projector.handle(event.event())
        is FeatureEnabledEventAvro -> projector.handle(event.event())
        is SubjectAddedToWhitelistEventAvro -> projector.handle(event.event())
        is SubjectDeletedFromWhitelistEventAvro -> projector.handle(event.event())
        else -> logger.info("Unhandled feature event received: $event")
      }
    }
  }

  private fun FeatureCreatedEventAvro.event(): FeatureCreatedEvent =
      FeatureCreatedEvent(FeatureIdentifier(aggregateIdentifier.identifier.toUUID()), featureName)

  private fun FeatureDeletedEventAvro.event(): FeatureDeletedEvent =
      FeatureDeletedEvent(FeatureIdentifier(aggregateIdentifier.identifier.toUUID()))

  private fun FeatureWhitelistActivatedEventAvro.event(): FeatureWhitelistActivatedEvent =
      FeatureWhitelistActivatedEvent(FeatureIdentifier(aggregateIdentifier.identifier.toUUID()))

  private fun FeatureDisabledEventAvro.event(): FeatureDisabledEvent =
      FeatureDisabledEvent(FeatureIdentifier(aggregateIdentifier.identifier.toUUID()))

  private fun FeatureEnabledEventAvro.event(): FeatureEnabledEvent =
      FeatureEnabledEvent(FeatureIdentifier(aggregateIdentifier.identifier.toUUID()))

  private fun SubjectAddedToWhitelistEventAvro.event(): SubjectAddedToWhitelistEvent =
      SubjectAddedToWhitelistEvent(
          FeatureIdentifier(aggregateIdentifier.identifier.toUUID()),
          WhitelistedSubject(subjectRef, SubjectTypeEnum.valueOf(type)))

  private fun SubjectDeletedFromWhitelistEventAvro.event(): SubjectDeletedFromWhitelistEvent =
      SubjectDeletedFromWhitelistEvent(
          FeatureIdentifier(aggregateIdentifier.identifier.toUUID()),
          WhitelistedSubject(subjectRef, SubjectTypeEnum.valueOf(type)))

  override fun listenToFeatureEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    listen(
        ConsumerRecord(
            record.topic(),
            record.partition(),
            record.offset(),
            record.timestamp(),
            record.timestampType(),
            record.serializedKeySize(),
            record.serializedValueSize(),
            record.key(),
            record.value(),
            record.headers(),
            record.leaderEpoch(),
        ))
    ack.acknowledge()
  }
}
