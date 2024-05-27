/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import java.util.UUID

data class AggregateEventMessageKey(
    val aggregateIdentifier: AggregateIdentifier,
    override val rootContextIdentifier: UUID
) : EventMessageKey {

  override fun toAvro(): MessageKeyAvro =
      MessageKeyAvro.newBuilder()
          .setAggregateIdentifier(aggregateIdentifier.toAvro())
          .setRootContextIdentifier(rootContextIdentifier.toString())
          .build()
}
