/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import java.util.UUID
import org.apache.avro.specific.SpecificRecord

sealed interface EventMessageKey {

  val rootContextIdentifier: UUID

  fun toAvro(): SpecificRecord
}
