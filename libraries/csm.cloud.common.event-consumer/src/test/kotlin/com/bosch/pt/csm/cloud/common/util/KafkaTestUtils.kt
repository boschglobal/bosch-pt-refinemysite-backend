/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.util

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.kafka.CustomKafkaHeaders.BUSINESS_TRANSACTION_ID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeader

object KafkaTestUtils {

  fun buildRandomSpecificRecord() =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(UUID.randomUUID().toString())
          .setType("SOME_TYPE")
          .setVersion(Random.nextLong())
          .build()

  fun buildRandomMessageKey() =
      AggregateEventMessageKey(
          AggregateIdentifier("SOME_TYPE", UUID.randomUUID(), Random.nextLong()), UUID.randomUUID())

  fun mockRecord(
      offset: Long,
      key: EventMessageKey = buildRandomMessageKey(),
      value: SpecificRecordBase? = buildRandomSpecificRecord(),
      transactionIdentifier: UUID? = null,
      timestamp: LocalDateTime = LocalDateTime.now()
  ) =
      mockk<ConsumerRecord<EventMessageKey, SpecificRecordBase?>>().apply {
        every { offset() } returns offset
        every { key() } returns key
        every { value() } returns value
        every { timestamp() } returns timestamp.toEpochMilli()
        when (transactionIdentifier) {
          null -> every { headers().lastHeader(BUSINESS_TRANSACTION_ID) } returns null
          else ->
              every { headers().lastHeader(BUSINESS_TRANSACTION_ID) } returns
                  RecordHeader(
                      BUSINESS_TRANSACTION_ID, transactionIdentifier.toString().toByteArray())
        }
      }
}
