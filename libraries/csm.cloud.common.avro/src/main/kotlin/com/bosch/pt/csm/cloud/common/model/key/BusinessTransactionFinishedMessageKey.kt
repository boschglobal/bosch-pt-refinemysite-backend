/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import com.bosch.pt.csm.cloud.common.transaction.messages.BusinessTransactionFinishedMessageKeyAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

data class BusinessTransactionFinishedMessageKey(
    val transactionIdentifier: UUID,
    override val rootContextIdentifier: UUID
) : EventMessageKey {

  override fun toAvro(): SpecificRecord =
      BusinessTransactionFinishedMessageKeyAvro.newBuilder()
          .setTransactionIdentifier(transactionIdentifier.toString())
          .setRootContextIdentifier(rootContextIdentifier.toString())
          .build()
}
