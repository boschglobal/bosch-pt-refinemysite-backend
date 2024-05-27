/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.eventstore.restore

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextRestoreEventListener
import com.bosch.pt.csm.cloud.usermanagement.common.util.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserContextRestoreEventListenerTest : AbstractRestoreIntegrationTest() {

  @Autowired lateinit var cut: UserContextRestoreEventListener

  // TODO: [SMAR-13767] enable again when there are more implements of EventMessageKey
  @Disabled
  @Test
  fun `consumption of unknown tombstone event with unknown key is received`() {
    val acknowledgement = TestAcknowledgement()
    // TODO the key should be an EventMessageKey but different from AggregateEventMessageKey
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>("fake-topic", 0, 0L, null, null)
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.listenToUserEvents(record, acknowledgement) }
        .withMessage("Unknown message key type received: " + AggregateIdentifierAvro::class.java)
  }

  @Test
  fun `consumption of unknown tombstone event fails`() {
    val acknowledgement = TestAcknowledgement()
    val identifier = AggregateIdentifierAvro(randomUUID().toString(), 0L, "fake")
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "fake-topic",
            0,
            0L,
            AggregateEventMessageKey(identifier.buildAggregateIdentifier(), randomUUID()),
            null)
    assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
      cut.listenToUserEvents(record, acknowledgement)
    }
  }
}
