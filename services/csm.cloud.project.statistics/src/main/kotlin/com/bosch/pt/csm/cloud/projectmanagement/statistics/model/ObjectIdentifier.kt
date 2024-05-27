/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
class ObjectIdentifier(var type: String, var identifier: UUID) {

  constructor(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ) : this(aggregateIdentifierAvro.getType(), aggregateIdentifierAvro.toUUID())
}
