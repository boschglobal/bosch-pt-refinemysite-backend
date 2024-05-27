/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import java.util.UUID

data class AggregateIdentifier(val type: String, val identifier: UUID, val version: Long) {

  fun toAvro(): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setType(type)
          .setIdentifier(identifier.toString())
          .setVersion(version)
          .build()
}
