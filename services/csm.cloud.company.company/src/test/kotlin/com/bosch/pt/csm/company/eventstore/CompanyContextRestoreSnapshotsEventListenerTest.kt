/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.common.AbstractRestoreIntegrationTest
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CompanyContextRestoreSnapshotsEventListenerTest : AbstractRestoreIntegrationTest() {

  @Autowired private lateinit var cut: CompanyContextRestoreSnapshotsEventListener

  @Test
  fun `validate fail if unsupported tombstone event is received`() {
    val acknowledgement = TestAcknowledgement()
    val identifier = AggregateIdentifierAvro(randomString(), 0L, "fake")
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "fake-topic",
            0,
            0L,
            AggregateEventMessageKey(identifier.buildAggregateIdentifier(), UUID.randomUUID()),
            null)
    assertThatExceptionOfType(UnsupportedOperationException::class.java).isThrownBy {
      cut.listenToCompanyEvents(record, acknowledgement)
    }
  }
}
