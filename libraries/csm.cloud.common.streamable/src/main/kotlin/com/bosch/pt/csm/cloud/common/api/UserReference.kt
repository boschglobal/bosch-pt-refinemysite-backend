/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.api

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro

data class UserReference(val identifier: UserId, val version: Long) {
  fun toAggregateIdentifier(): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setVersion(version)
          .setType("USER")
          .build()
}
