/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.dto

import com.bosch.pt.iot.smartsite.common.i18n.Key.FILE
import java.time.LocalDateTime
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder.getLocale

class TopicAttachmentDescription(val fileName: String, date: LocalDateTime) : Description(date) {

  override fun toDisplayValue(messageSource: MessageSource): String =
      messageSource.getMessage(FILE, emptyArray(), getLocale()) + ": " + fileName
}
