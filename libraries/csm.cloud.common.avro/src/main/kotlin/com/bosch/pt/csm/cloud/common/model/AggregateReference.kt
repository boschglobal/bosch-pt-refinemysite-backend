/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model

import com.bosch.pt.csm.cloud.common.messages.AggregateReferenceAvro
import java.util.UUID

@Suppress("unused")
data class AggregateReference(val type: String, val identifier: UUID) {

  fun toAvro(): AggregateReferenceAvro =
      AggregateReferenceAvro.newBuilder().setType(type).setIdentifier(identifier.toString()).build()
}
