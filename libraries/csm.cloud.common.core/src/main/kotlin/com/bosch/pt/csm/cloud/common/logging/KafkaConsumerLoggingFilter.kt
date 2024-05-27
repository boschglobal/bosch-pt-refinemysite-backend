/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.logging

import ch.qos.logback.classic.Level.WARN
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.spi.FilterReply.DENY
import ch.qos.logback.core.spi.FilterReply.NEUTRAL

class KafkaConsumerLoggingFilter : Filter<ILoggingEvent>() {

  override fun decide(event: ILoggingEvent): FilterReply =
      if (event.level.isGreaterOrEqual(WARN) &&
          event.formattedMessage.matches(
              "These configurations '\\[.*\\]' were supplied but are not used yet\\.".toRegex()))
          DENY
      else NEUTRAL
}
