/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.extension

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.CustomKafkaHeaders.BUSINESS_TRANSACTION_ID
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import java.util.UUID
import kotlin.text.Charsets.UTF_8
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Header

fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.isBusinessTransactionFinished() =
    this.key() is BusinessTransactionFinishedMessageKey

fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.isBusinessTransactionStarted() =
    this.key() is BusinessTransactionStartedMessageKey

fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.isPartOfBusinessTransaction() =
    this.getTransactionIdentifier() != null

fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.getTransactionIdentifier(): UUID? {
  val header: Header? = this.headers().lastHeader(BUSINESS_TRANSACTION_ID)
  if (header == null || header.value().isEmpty()) {
    return null
  }
  return header.let { (it.value() as ByteArray).toString(UTF_8).toUUID() }
}
