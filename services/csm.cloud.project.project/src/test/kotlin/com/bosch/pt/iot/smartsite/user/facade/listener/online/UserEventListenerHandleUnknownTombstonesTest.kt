/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
open class UserEventListenerHandleUnknownTombstonesTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var userEventListener: UserEventListenerImpl

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate()
  }

  @Test
  fun `validate fails if unknown tombstone event with unknown key is received`() {
    val acknowledgement = TestAcknowledgement()
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "fake-topic",
            0,
            0L,
            BusinessTransactionStartedMessageKey(randomUUID(), randomUUID()),
            null)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { userEventListener.listenToUserEvents(record, acknowledgement) }
        .withMessageContaining("Unknown tombstone avro message received")
  }

  @Test
  fun `validate fails if unknown tombstone event is received`() {
    val acknowledgement = TestAcknowledgement()
    val identifier = AggregateIdentifierAvro(randomUUID().toString(), 0L, "fake")
    val key = AggregateEventMessageKey(identifier.buildAggregateIdentifier(), randomUUID())
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>("fake-topic", 0, 0L, key, null)

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { userEventListener.listenToUserEvents(record, acknowledgement) }
        .withMessage("Unknown Avro tombstone message received: $key")
  }
}
