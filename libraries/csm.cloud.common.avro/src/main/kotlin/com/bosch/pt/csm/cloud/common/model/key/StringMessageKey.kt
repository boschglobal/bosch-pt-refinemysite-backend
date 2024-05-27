/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.key

import com.bosch.pt.csm.cloud.common.messages.StringMessageKeyAvro
import org.apache.avro.specific.SpecificRecord

data class StringMessageKey(val identifier: String) {

  fun toAvro(): SpecificRecord = StringMessageKeyAvro.newBuilder().setIdentifier(identifier).build()
}
