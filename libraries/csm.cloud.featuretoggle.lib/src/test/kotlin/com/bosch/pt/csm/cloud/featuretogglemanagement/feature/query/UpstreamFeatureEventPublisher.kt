/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.kafka.logProduction
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureCreatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDeletedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureDisabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureEnabledEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.FeatureWhitelistActivatedEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectAddedToWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretoggle.feature.messages.SubjectDeletedFromWhitelistEventAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.UpstreamFeatureEvent
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Profile("test")
@Component
internal class UpstreamFeatureEventPublisher(
    @Value("\${custom.kafka.bindings.feature.kafkaTopic}") private val topic: String,
    private val kafkaTemplate: KafkaTemplate<MessageKeyAvro, SpecificRecord>,
    private val logger: Logger
) {
  fun publish(event: UpstreamFeatureEvent) {
    val key =
        MessageKeyAvro.newBuilder()
            .setRootContextIdentifier(event.featureIdentifier.toString())
            .setAggregateIdentifierBuilder(
                AggregateIdentifierAvro.newBuilder()
                    .setIdentifier(event.featureIdentifier.toString())
                    .setType(FEATURE_TOGGLE.name)
                    .setVersion(0))
            .build()

    kafkaTemplate.send(ProducerRecord(topic, 0, key, event.toAvro() as SpecificRecord)).get().also {
      logger.logProduction(it.producerRecord)
    }
  }

  private fun UpstreamFeatureEvent.toAvro() =
      when (this) {
        is FeatureCreatedEvent -> toAvro()
        is FeatureDeletedEvent -> toAvro()
        is FeatureEnabledEvent -> toAvro()
        is FeatureDisabledEvent -> toAvro()
        is FeatureWhitelistActivatedEvent -> toAvro()
        is SubjectAddedToWhitelistEvent -> toAvro()
        is SubjectDeletedFromWhitelistEvent -> toAvro()
      }

  private fun FeatureCreatedEvent.toAvro() =
      FeatureCreatedEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          name,
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun FeatureDeletedEvent.toAvro() =
      FeatureDeletedEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun FeatureEnabledEvent.toAvro() =
      FeatureEnabledEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun FeatureDisabledEvent.toAvro() =
      FeatureDisabledEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun FeatureWhitelistActivatedEvent.toAvro() =
      FeatureWhitelistActivatedEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun SubjectAddedToWhitelistEvent.toAvro() =
      SubjectAddedToWhitelistEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          subject.subjectRef,
          subject.subjectType.name,
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))

  private fun SubjectDeletedFromWhitelistEvent.toAvro() =
      SubjectDeletedFromWhitelistEventAvro(
          AggregateIdentifierAvro(featureIdentifier.toString(), 0, FEATURE_TOGGLE.name),
          subject.subjectRef,
          subject.subjectType.name,
          "BIM",
          EventAuditingInformationAvro(
              LocalDateTime.now().toEpochMilli(), UUID.randomUUID().toString()))
}
