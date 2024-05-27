/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.restore

import com.bosch.pt.csm.cloud.common.exception.RestoreServiceAheadOfOnlineServiceException
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.listener.BlockOffsetSynchronizationManager
import com.bosch.pt.iot.smartsite.test.Repositories
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.RestoreProfilePictureStrategy
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.RestoreUserStrategy
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.UserContextRestoreDbStrategy
import java.util.TimeZone
import java.util.UUID.randomUUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

@RestoreStrategyTest
open class RestoreDbUserEventListenerBlockUnsynchronizedOffsetTest {

  @Autowired lateinit var entityManager: EntityManager

  @Autowired lateinit var repositories: Repositories

  @Autowired lateinit var transactionTemplate: TransactionTemplate

  private lateinit var userEventListener: UserEventListener

  private val synchronizationManager = BlockOffsetSynchronizationManager()

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }

  @BeforeEach
  open fun setup() {
    val userContextStrategyDispatcher: RestoreDbStrategyDispatcher<UserContextRestoreDbStrategy> =
        RestoreDbStrategyDispatcher<UserContextRestoreDbStrategy>(
            transactionTemplate,
            listOf(
                RestoreUserStrategy(
                    repositories.craftRepository,
                    repositories.profilePictureRepository,
                    repositories.participantRepository,
                    repositories.userRepository,
                    entityManager),
                RestoreProfilePictureStrategy(
                    repositories.profilePictureRepository,
                    repositories.userRepository,
                    entityManager)))
    userEventListener =
        RestoreDbUserEventListenerImpl(userContextStrategyDispatcher, synchronizationManager)
  }

  @AfterEach
  fun clean() {
    repositories.truncateDatabase()
  }

  @Test
  open fun `validate processing succeeds if max offset is larger than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, Long.MAX_VALUE)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 0L)

    assertThat(repositories.userRepository.count()).isEqualTo(0L)
    userEventListener.listenToUserEvents(record, acknowledgement)
    assertThat(acknowledgement.isAcknowledged).isTrue()
    assertThat(repositories.userRepository.count()).isEqualTo(1L)
  }

  @Test
  open fun `validate processing fails if max offset is smaller than record offset`() {
    val topic = "topic"
    val partition = 0
    synchronizationManager.setMaxTopicPartitionOffset(topic, partition, 5L)
    val acknowledgement = TestAcknowledgement()
    val record = record(topic, partition, 6L)

    assertThat(repositories.userRepository.count()).isEqualTo(0L)
    Assertions.assertThatExceptionOfType(RestoreServiceAheadOfOnlineServiceException::class.java)
        .isThrownBy { userEventListener.listenToUserEvents(record, acknowledgement) }
    assertThat(acknowledgement.isAcknowledged).isFalse()
    assertThat(repositories.userRepository.count()).isEqualTo(0L)
  }

  fun record(
      topic: String,
      partition: Int,
      offset: Long
  ): ConsumerRecord<EventMessageKey, SpecificRecordBase?> {
    val userIdentifier = userIdentifier()
    val value = user(userIdentifier)
    val key = messageKey(userIdentifier)
    return ConsumerRecord(topic, partition, offset, key, value)
  }

  fun user(identifier: AggregateIdentifierAvro): UserEventAvro {
    val date = System.currentTimeMillis()
    return UserEventAvro(
        UserEventEnumAvro.CREATED,
        UserAggregateAvro.newBuilder()
            .setAdmin(true)
            .setCrafts(emptyList())
            .setPhoneNumbers(emptyList())
            .setAggregateIdentifier(identifier)
            .setAuditingInformation(
                AuditingInformationAvro.newBuilder()
                    .setCreatedBy(identifier)
                    .setLastModifiedBy(identifier)
                    .setCreatedDate(date)
                    .setLastModifiedDate(date)
                    .build())
            .setEmail("admin@example.com")
            .setEulaAcceptedDate(date)
            .setFirstName("admin")
            .setGender(GenderEnumAvro.MALE)
            .setLastName("user")
            .setPosition("admin")
            .setRegistered(true)
            .setUserId(randomUUID().toString())
            .build())
  }

  private fun messageKey(identifier: AggregateIdentifierAvro): AggregateEventMessageKey =
      AggregateEventMessageKey(
          identifier.buildAggregateIdentifier(), identifier.identifier.toUUID())

  private fun userIdentifier(): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(randomUUID().toString())
          .setType(UsermanagementAggregateTypeEnum.USER.value)
          .setVersion(0L)
          .build()
}
