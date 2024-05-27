/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.online

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
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
open class CompanyEventListenerHandleTombstonesTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var companyEventListener: CompanyEventListenerImpl

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate()
    useOnlineListener()
  }

  @Test
  fun `throws exception when empty message is received`() {
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>("test", 0, 0, randomMessageKey(), null)
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { companyEventListener.listenToCompanyEvents(record, TestAcknowledgement()) }
        .withMessage("Unknown tombstone avro message received: " + record.key())
  }

  @Test
  fun `throws exception when unknown message is received`() {
    val record =
        ConsumerRecord<EventMessageKey, SpecificRecordBase?>(
            "test", 0, 0, randomMessageKey(), randomMessageKeyAvro())
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { companyEventListener.listenToCompanyEvents(record, TestAcknowledgement()) }
        .withMessage("Unknown Avro message received: " + record.value()?.schema?.name)
  }

  private fun randomMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          AggregateIdentifier(type = COMPANY.value, identifier = randomUUID(), version = 0L),
          randomUUID())

  private fun randomMessageKeyAvro(): MessageKeyAvro =
      MessageKeyAvro.newBuilder()
          .setRootContextIdentifier(randomUUID().toString())
          .setAggregateIdentifier(
              AggregateIdentifierAvro.newBuilder()
                  .setType(COMPANY.value)
                  .setIdentifier(randomUUID().toString())
                  .setVersion(0L)
                  .build())
          .build()
}
