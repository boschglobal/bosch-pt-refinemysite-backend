/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import java.time.LocalDate
import java.time.LocalDateTime
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractEventProcessor {

  @Autowired private lateinit var processStateOnlyProperties: ProcessStateOnlyProperties

  abstract fun process(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      recordTimestamp: LocalDateTime
  )

  protected fun shouldProcessStateOnly(recordTimestamp: LocalDateTime): Boolean {
    if (!processStateOnlyProperties.isEnabled) {
      return false
    }

    val stateOnlyUntilDate = LocalDate.parse(processStateOnlyProperties.untilDate)
    val eventDate = recordTimestamp.toLocalDate()

    return eventDate.isBefore(stateOnlyUntilDate)
  }
}
