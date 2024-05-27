/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.restore

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class RestoreDbCompanyEventListenerImplTest : AbstractRestoreIntegrationTestV2() {

  @Autowired private lateinit var companyEventListener: RestoreDbCompanyEventListenerImpl

  @Test
  open fun `validate fail if unknown tombstone event is received`() {
    val identifier = AggregateIdentifierAvro(randomUUID().toString(), 0L, "fake")
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "fake-topic",
            0,
            0L,
            AggregateEventMessageKey(identifier.buildAggregateIdentifier(), randomUUID()),
            null)
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { companyEventListener.listenToCompanyEvents(record, TestAcknowledgement()) }
        .withMessage("No strategy found to handle event of type: fake")
  }
}
