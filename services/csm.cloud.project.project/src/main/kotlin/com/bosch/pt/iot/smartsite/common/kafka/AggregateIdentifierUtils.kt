/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity

object AggregateIdentifierUtils {

  @JvmStatic
  fun getAggregateIdentifier(
      entity: AbstractEntity<*, *>,
      aggregateType: String
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro(entity.identifier.toString(), entity.version, aggregateType)

  @JvmStatic
  fun getAggregateIdentifier(
      entity: AbstractSnapshotEntity<*, *>,
      aggregateType: String
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro(entity.identifier.toString(), entity.version, aggregateType)

  @JvmStatic
  fun getAggregateIdentifier(
      entity: AbstractReplicatedEntity<*>,
      aggregateType: String
  ): AggregateIdentifierAvro =
      AggregateIdentifierAvro(entity.identifier.toString(), entity.version, aggregateType)
}
