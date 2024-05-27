/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.messaging

import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import org.apache.avro.specific.SpecificRecord

interface CommandSendingService {

  fun send(key: CommandMessageKey, value: SpecificRecord, channel: String)
}
