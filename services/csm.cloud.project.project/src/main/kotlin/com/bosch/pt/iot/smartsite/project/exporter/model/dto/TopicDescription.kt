/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.model.dto

import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.time.LocalDateTime
import org.springframework.context.MessageSource

class TopicDescription(
    val identifier: TopicId,
    val text: String?,
    date: LocalDateTime,
    val children: MutableList<Description>
) : Description(date) {

  override fun toDisplayValue(messageSource: MessageSource): String {
    val str = StringBuffer()
    if (text != null) {
      str.append("\n").append(text)
    }

    for (child in children.sortedBy { it.date }) {
      if (child is TopicAttachmentDescription) {
        str.append("\n").append(child.toDisplayValue(messageSource))
      } else {
        child.toDisplayValue(messageSource)?.let { str.append("\n- ").append(it) }
      }
    }

    return str.toString()
  }
}
