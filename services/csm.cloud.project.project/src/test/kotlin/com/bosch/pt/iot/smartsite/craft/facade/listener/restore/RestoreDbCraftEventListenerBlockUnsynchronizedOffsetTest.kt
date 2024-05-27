/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.craft.facade.listener.restore

import com.bosch.pt.csm.cloud.common.exception.RestoreServiceAheadOfOnlineServiceException
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.referencedata.craft.CraftTranslationAvro
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum.CRAFT
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.listener.BlockOffsetSynchronizationManager
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy.CraftContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy.RestoreCraftStrategy
import jakarta.persistence.EntityManager
import java.util.Locale.UK
import java.util.TimeZone
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.Long.Companion.MAX_VALUE
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class RestoreDbCraftEventListenerBlockUnsynchronizedOffsetTest :
    AbstractRestoreIntegrationTestV2() {

  @Autowired lateinit var entityManager: EntityManager

  private lateinit var craftEventListener: CraftEventListener

  private val user by lazy { repositories.findUser(getIdentifier("system"))!! }

  private val synchronizationManager = BlockOffsetSynchronizationManager()

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }

  @BeforeEach
  open fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate()
    setAuthentication(getIdentifier("system"))

    val craftContextStrategyDispatcher: RestoreDbStrategyDispatcher<CraftContextRestoreDbStrategy> =
        RestoreDbStrategyDispatcher(
            transactionTemplate,
            listOf(
                RestoreCraftStrategy(
                    repositories.craftRepository, repositories.userRepository, entityManager)))
    craftEventListener =
        RestoreDbCraftEventListenerImpl(craftContextStrategyDispatcher, synchronizationManager)
  }

  @Test
  open fun `validate processing succeeds if max offset is larger than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, MAX_VALUE)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 0L)

    assertThat(repositories.craftRepository.count()).isEqualTo(0L)
    craftEventListener.listenToCraftEvents(record, acknowledgement)
    assertThat(acknowledgement.isAcknowledged).isTrue
    assertThat(repositories.craftRepository.count()).isEqualTo(1L)
  }

  @Test
  open fun `validate processing fails if max offset is smaller than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, 5L)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 6L)

    assertThat(repositories.craftRepository.count()).isEqualTo(0L)
    assertThatExceptionOfType(RestoreServiceAheadOfOnlineServiceException::class.java).isThrownBy {
      craftEventListener.listenToCraftEvents(record, acknowledgement)
    }
    assertThat(acknowledgement.isAcknowledged).isFalse
    assertThat(repositories.craftRepository.count()).isEqualTo(0L)
  }

  fun record(
      topic: String,
      partition: Int,
      offset: Long
  ): ConsumerRecord<EventMessageKey, SpecificRecordBase?> {
    val csmIdentifier = user.identifier!!
    val userIdentifier = identifier(csmIdentifier, USER.value)
    val craftIdentifier = identifier(CRAFT.value)
    val value = craft(craftIdentifier, userIdentifier)
    val key = messageKey(craftIdentifier)
    return ConsumerRecord(topic, partition, offset, key, value)
  }

  fun craft(
      identifier: AggregateIdentifierAvro,
      userIdentifier: AggregateIdentifierAvro
  ): CraftEventAvro {
    val date = System.currentTimeMillis()
    return CraftEventAvro(
        CREATED,
        CraftAggregateAvro.newBuilder()
            .setAggregateIdentifier(identifier)
            .setAuditingInformation(
                AuditingInformationAvro.newBuilder()
                    .setCreatedBy(userIdentifier)
                    .setLastModifiedBy(userIdentifier)
                    .setCreatedDate(date)
                    .setLastModifiedDate(date)
                    .build())
            .setDefaultName("Test")
            .setTranslations(
                listOf(
                    CraftTranslationAvro.newBuilder()
                        .setLocale(UK.language)
                        .setValue("Test")
                        .build()))
            .build())
  }

  private fun messageKey(identifier: AggregateIdentifierAvro): AggregateEventMessageKey =
      AggregateEventMessageKey(
          identifier.buildAggregateIdentifier(), identifier.getIdentifier().toUUID())

  private fun identifier(type: String): AggregateIdentifierAvro = identifier(randomUUID(), type)

  private fun identifier(identifier: UUID, type: String): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setType(type)
          .setVersion(0L)
          .build()
}
