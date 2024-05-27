/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.job.application.config.KafkaConsumerConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaProducerAvroConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaTopicInitializationConfiguration
import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.extensions.KafkaTestExtension
import com.bosch.pt.csm.cloud.job.user.query.ExternalUserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.UserChangedEvent
import com.bosch.pt.csm.cloud.job.user.query.UserDeletedEvent
import com.bosch.pt.csm.cloud.job.user.query.UserProjector
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberAvro
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Locale
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles

@ExtendWith(KafkaTestExtension::class)
@ActiveProfiles("local")
@SpringBootTest(classes = [UserEventListenerIntegrationTestConfiguration::class])
class UserEventListenerIntegrationTest {

  @MockkBean lateinit var userProjector: UserProjector

  @Value("\${custom.kafka.bindings.user.kafkaTopic}") private lateinit var topic: String

  @Autowired
  @Qualifier("avro")
  lateinit var kafkaTemplate: KafkaTemplate<MessageKeyAvro, SpecificRecord?>

  @Autowired lateinit var userEventListener: UserEventListener

  @BeforeEach
  fun setupMock() {
    every { userProjector.handle(any()) } just runs
  }

  @Test
  fun `listens for UserChangedEvent`() {
    val userChangedEvent =
        UserChangedEvent(
            UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"),
            ExternalUserIdentifier("externalUserIdentifier"),
            Locale.GERMAN)

    send(userChangedEvent)

    verify(timeout = 30_000) { userProjector.handle(userChangedEvent) }
  }

  @Test
  fun `listens for User tombstones`() {
    val userDeletedEvent = UserDeletedEvent(UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"))

    sendTombstoneFor(messageKeyAvro(userDeletedEvent.userIdentifier))

    verify(timeout = 30_000) { userProjector.handle(userDeletedEvent) }
  }

  @Test
  fun `ignores tombstones for other events`() {
    // Calling the listener directly.  Asynchronous execution makes no sense, because we don't want
    // to wait for a timeout before we can verify.
    userEventListener.listen(
        ConsumerRecord(
            topic,
            0,
            123L,
            aggregateEventMessageKey(
                UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"), USERPICTURE),
            null))

    verify { userProjector wasNot Called }
  }

  private fun send(userChangedEvent: UserChangedEvent) {
    kafkaTemplate.executeInTransaction {
      it.send(
          topic,
          messageKeyAvro(userChangedEvent.userIdentifier),
          UserEventAvro.newBuilder()
              .apply {
                name = UserEventEnumAvro.CREATED
                aggregateBuilder = userAggregateBuilder(userChangedEvent)
              }
              .build())
    }
  }

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  private fun sendTombstoneFor(messageKeyAvro: MessageKeyAvro) {
    kafkaTemplate.executeInTransaction { it.send(topic, messageKeyAvro, null) }
  }

  private fun userAggregateBuilder(userChangedEvent: UserChangedEvent) =
      UserAggregateAvro.newBuilder().apply {
        aggregateIdentifierBuilder = aggregateIdentifierAvroBuilder(userChangedEvent.userIdentifier)
        userId = userChangedEvent.externalUserIdentifier.value
        locale = userChangedEvent.locale.toString()
        // unused mandatory fields below
        auditingInformationBuilder =
            AuditingInformationAvro.newBuilder().apply {
              createdByBuilder =
                  aggregateIdentifierAvroBuilder(
                      UserIdentifier("39a4437e-597f-462c-80b4-ab89bd86a9f6"))
              createdDate = LocalDateTime.parse("2022-03-15T09:31:08.372").toEpochMilli()
              lastModifiedByBuilder =
                  aggregateIdentifierAvroBuilder(
                      UserIdentifier("39a4437e-597f-462c-80b4-ab89bd86a9f6"))
              lastModifiedDate = LocalDateTime.parse("2022-03-15T09:31:08.372").toEpochMilli()
            }
        registered = true
        admin = false
        phoneNumbers =
            listOf(
                PhoneNumberAvro.newBuilder()
                    .apply {
                      phoneNumberType = PhoneNumberTypeEnumAvro.BUSINESS
                      countryCode = "0049"
                      callNumber = "123456789"
                    }
                    .build())
        crafts = emptyList()
      }

  private fun messageKeyAvro(
      userIdentifier: UserIdentifier,
      aggregateType: UsermanagementAggregateTypeEnum = USER
  ) =
      MessageKeyAvro.newBuilder()
          .apply {
            rootContextIdentifier = userIdentifier.value
            aggregateIdentifierBuilder =
                aggregateIdentifierAvroBuilder(userIdentifier, aggregateType)
          }
          .build()

  private fun aggregateIdentifierAvroBuilder(
      userIdentifier: UserIdentifier,
      aggregateType: UsermanagementAggregateTypeEnum = USER
  ) =
      AggregateIdentifierAvro.newBuilder().apply {
        type = aggregateType.name
        identifier = userIdentifier.value
        version = 1
      }

  private fun aggregateEventMessageKey(
      userIdentifier: UserIdentifier,
      aggregateType: UsermanagementAggregateTypeEnum = USER
  ) =
      AggregateEventMessageKey(
          aggregateIdentifier =
              AggregateIdentifier(aggregateType.name, userIdentifier.value.toUUID(), 1),
          rootContextIdentifier = userIdentifier.value.toUUID())
}

@Import(
    UserEventListener::class,
    KafkaTopicInitializationConfiguration::class,
    KafkaProducerAvroConfiguration::class,
    KafkaConsumerConfiguration::class,
    SimpleMeterRegistry::class,
    LoggerConfiguration::class)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
private class UserEventListenerIntegrationTestConfiguration
