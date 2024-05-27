/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.messaging.impl

import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import org.apache.avro.specific.SpecificRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("restore-db")
@Service
class RestoreCommandSendingServiceImpl : CommandSendingService {

  override fun send(key: CommandMessageKey, value: SpecificRecord, channel: String): Unit =
      throw IllegalStateException("Cannot send commands in restore mode")
}
