/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.messages

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.StringMessageKey
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionFinishedMessageKeyAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionStartedMessageKeyAvro
import org.apache.avro.generic.GenericRecord

object MessageKeyFactory {

  fun createEventMessageKey(keyAvro: GenericRecord): EventMessageKey =
      when (keyAvro) {
        is MessageKeyAvro ->
            AggregateEventMessageKey(
                keyAvro.getAggregateIdentifier().buildAggregateIdentifier(),
                keyAvro.getRootContextIdentifier().toUUID())
        is BusinessTransactionStartedMessageKeyAvro ->
            BusinessTransactionStartedMessageKey(
                keyAvro.getTransactionIdentifier().toUUID(),
                keyAvro.getRootContextIdentifier().toUUID())
        is BusinessTransactionFinishedMessageKeyAvro ->
            BusinessTransactionFinishedMessageKey(
                keyAvro.getTransactionIdentifier().toUUID(),
                keyAvro.getRootContextIdentifier().toUUID())
        else -> error("Unsupported message key type: ${keyAvro.javaClass}")
      }

  fun createCommandMessageKey(keyAvro: GenericRecord): CommandMessageKey =
      when (keyAvro) {
        is CommandMessageKeyAvro -> CommandMessageKey(keyAvro.getPartitioningIdentifier().toUUID())
        else -> error("Unsupported message key type: ${keyAvro.javaClass}")
      }

  fun createStringMessageKey(keyAvro: GenericRecord): StringMessageKey =
      when (keyAvro) {
        is StringMessageKeyAvro -> StringMessageKey(keyAvro.identifier)
        else -> error("Unsupported message key type: ${keyAvro.javaClass}")
      }
}
