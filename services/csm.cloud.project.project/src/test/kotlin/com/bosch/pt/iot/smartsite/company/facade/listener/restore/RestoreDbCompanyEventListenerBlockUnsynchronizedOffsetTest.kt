/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.restore

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
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.listener.BlockOffsetSynchronizationManager
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy.CompanyContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy.RestoreCompanyStrategy
import java.util.TimeZone
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.persistence.EntityManager
import kotlin.Long.Companion.MAX_VALUE
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class RestoreDbCompanyEventListenerBlockUnsynchronizedOffsetTest :
    AbstractRestoreIntegrationTestV2() {

  @Autowired lateinit var entityManager: EntityManager

  private lateinit var companyEventListener: CompanyEventListener

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

    val craftContextStrategyDispatcher:
        RestoreDbStrategyDispatcher<CompanyContextRestoreDbStrategy> =
        RestoreDbStrategyDispatcher(
            transactionTemplate,
            listOf(
                RestoreCompanyStrategy(
                    repositories.companyRepository, repositories.userRepository, entityManager)))
    companyEventListener =
        RestoreDbCompanyEventListenerImpl(craftContextStrategyDispatcher, synchronizationManager)
  }

  @Test
  open fun `validate processing succeeds if max offset is larger than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, MAX_VALUE)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 0L)

    assertThat(repositories.companyRepository.count()).isEqualTo(0L)
    companyEventListener.listenToCompanyEvents(record, acknowledgement)
    assertThat(acknowledgement.isAcknowledged).isTrue
    assertThat(repositories.companyRepository.count()).isEqualTo(1L)
  }

  @Test
  open fun `validate processing fails if max offset is smaller than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, 5L)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 6L)

    assertThat(repositories.companyRepository.count()).isEqualTo(0L)
    assertThatExceptionOfType(RestoreServiceAheadOfOnlineServiceException::class.java).isThrownBy {
      companyEventListener.listenToCompanyEvents(record, acknowledgement)
    }
    assertThat(acknowledgement.isAcknowledged).isFalse
    assertThat(repositories.companyRepository.count()).isEqualTo(0L)
  }

  fun record(
      topic: String,
      partition: Int,
      offset: Long
  ): ConsumerRecord<EventMessageKey, SpecificRecordBase?> {
    val csmIdentifier = user.identifier!!
    val userIdentifier = identifier(csmIdentifier, USER.value)
    val companyIdentifier = identifier(COMPANY.value)
    val value = company(companyIdentifier, userIdentifier)
    val key = messageKey(companyIdentifier)
    return ConsumerRecord(topic, partition, offset, key, value)
  }

  fun company(
      identifier: AggregateIdentifierAvro,
      userIdentifier: AggregateIdentifierAvro
  ): CompanyEventAvro {
    val date = System.currentTimeMillis()
    return CompanyEventAvro(
        CREATED,
        CompanyAggregateAvro.newBuilder()
            .setAggregateIdentifier(identifier)
            .setAuditingInformation(
                AuditingInformationAvro.newBuilder()
                    .setCreatedBy(userIdentifier)
                    .setLastModifiedBy(userIdentifier)
                    .setCreatedDate(date)
                    .setLastModifiedDate(date)
                    .build())
            .setName("Test")
            .setStreetAddress(
                StreetAddressAvro.newBuilder()
                    .setCountry("A")
                    .setZipCode("B")
                    .setStreet("C")
                    .setHouseNumber("D")
                    .setCity("E")
                    .setArea("F")
                    .build())
            .build())
  }

  private fun messageKey(identifier: AggregateIdentifierAvro): AggregateEventMessageKey =
      AggregateEventMessageKey(
          identifier.buildAggregateIdentifier(), identifier.getIdentifier().toUUID())

  private fun identifier(type: String): AggregateIdentifierAvro = identifier(randomUUID(), type)

  fun identifier(identifier: UUID, type: String): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setType(type)
          .setVersion(0L)
          .build()
}
