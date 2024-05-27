/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.restore

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.CraftContextRestoreEventListener
import com.bosch.pt.csm.cloud.usermanagement.common.util.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@CodeExample
class CraftContextRestoreEventListenerTest : AbstractRestoreIntegrationTest() {

  @Autowired lateinit var cut: CraftContextRestoreEventListener

  @Test
  fun `consumption of not supported tombstone event fails`() {
    val acknowledgement = TestAcknowledgement()
    val identifier = AggregateIdentifierAvro(randomUUID().toString(), 0L, "fake")
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "fake-topic",
            0,
            0L,
            AggregateEventMessageKey(identifier.buildAggregateIdentifier(), randomUUID()),
            null)
    assertThatExceptionOfType(UnsupportedOperationException::class.java).isThrownBy {
      cut.listenToCraftEvents(record, acknowledgement)
    }
  }
}
