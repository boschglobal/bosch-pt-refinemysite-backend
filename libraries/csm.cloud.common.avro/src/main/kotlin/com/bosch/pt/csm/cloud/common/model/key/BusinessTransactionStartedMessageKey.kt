/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionStartedMessageKeyAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

data class BusinessTransactionStartedMessageKey(
    val transactionIdentifier: UUID,
    override val rootContextIdentifier: UUID
) : EventMessageKey {

  override fun toAvro(): SpecificRecord =
      BusinessTransactionStartedMessageKeyAvro.newBuilder()
          .setTransactionIdentifier(transactionIdentifier.toString())
          .setRootContextIdentifier(rootContextIdentifier.toString())
          .build()
}
