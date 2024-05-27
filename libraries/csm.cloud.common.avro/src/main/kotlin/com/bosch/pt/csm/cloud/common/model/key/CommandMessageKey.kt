/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import com.bosch.pt.csm.cloud.common.messages.CommandMessageKeyAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

data class CommandMessageKey(val partitioningIdentifier: UUID) {

  fun toAvro(): SpecificRecord =
      CommandMessageKeyAvro.newBuilder()
          .setPartitioningIdentifier(partitioningIdentifier.toString())
          .build()
}
