/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.facade.listener

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionFinishedMessageKeyAvro
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.FeaturetogglemanagementAggregateTypeEnum.FEATURE_TOGGLE
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import com.bosch.pt.csm.cloud.featuretogglemanagement.extensions.JupiterKafkaTestExtension
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDeletedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureWhitelistActivatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectAddedToWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.SubjectDeletedFromWhitelistEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureProjector
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.UpstreamFeatureEventPublisher
import com.bosch.pt.csm.cloud.testapp.TestApplication
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.util.UUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ExtendWith(JupiterKafkaTestExtension::class)
@DirtiesContext
@ActiveProfiles("local", "test")
@SpringBootTest(
    classes = [TestApplication::class, UpstreamFeatureEventPublisher::class],
    properties = ["custom.feature.enabled=true"])
class UpstreamFeatureEventListenerTest {
  private val verificationTimeoutMs: Long = 10_000

  @Value("\${custom.kafka.bindings.feature.kafkaTopic}") private lateinit var topic: String

  @MockkBean private lateinit var projector: FeatureProjector

  @Autowired private lateinit var eventListener: UpstreamFeatureEventListener

  @Autowired private lateinit var eventPublisher: UpstreamFeatureEventPublisher

  @BeforeEach
  fun init() {
    every { projector.handle(any()) } just runs
  }

  @Test
  fun `listens for feature created event`() {
    val event =
        FeatureCreatedEvent(FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"), "BIM")

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for feature deleted event`() {
    val event = FeatureDeletedEvent(FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for feature enabled event`() {
    val event = FeatureEnabledEvent(FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for feature disabled event`() {
    val event = FeatureDisabledEvent(FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for feature whitelist activated event`() {
    val event =
        FeatureWhitelistActivatedEvent(FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for subject added to whitelist event`() {
    val event =
        SubjectAddedToWhitelistEvent(
            FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"),
            WhitelistedSubject("projectId", SubjectTypeEnum.PROJECT))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `listens for subject deleted from whitelist event`() {
    val event =
        SubjectDeletedFromWhitelistEvent(
            FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f"),
            WhitelistedSubject("projectId", SubjectTypeEnum.PROJECT))

    eventPublisher.publish(event)

    verify(timeout = verificationTimeoutMs) { projector.handle(event) }
  }

  @Test
  fun `ignores other events`() {
    // Calling the listener directly.  Asynchronous execution makes no sense, because we don't want
    // to wait for a timeout before we can verify.
    eventListener.listen(ConsumerRecord(topic, 0, 123L, employeeAggregateEventMessageKey(), null))

    verify { projector wasNot Called }
  }

  @Test
  fun `ignores events where key is not aggregate message key`() {
    // Calling the listener directly.  Asynchronous execution makes no sense, because we don't want
    // to wait for a timeout before we can verify.
    eventListener.listen(
        ConsumerRecord(
            topic,
            0,
            234L,
            BusinessTransactionFinishedMessageKeyAvro("rootId", "transactionId"),
            null))

    verify { projector wasNot Called }
  }

  @Test
  fun `works with non avro key type`() {
    assertDoesNotThrow {
      eventListener.listen(
          ConsumerRecord(
              topic,
              0,
              234L,
              AggregateEventMessageKey(
                  AggregateIdentifier(
                      FEATURE_TOGGLE.name,
                      UUID.fromString("190475f1-d604-4690-8772-ca16b9219369"),
                      0,
                  ),
                  UUID.randomUUID(),
              ),
              null))
    }
  }

  private fun employeeAggregateEventMessageKey() =
      MessageKeyAvro(
          UUID.randomUUID().toString(),
          AggregateIdentifierAvro(UUID.randomUUID().toString(), 1, "EMPLOYEE"),
      )
}
