/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.dto

import java.time.LocalDateTime
import org.springframework.context.MessageSource

class MessageDescription(val text: String?, date: LocalDateTime) : Description(date) {

  override fun toDisplayValue(messageSource: MessageSource): String? = text
}
