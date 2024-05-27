/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.config

import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty.Companion.EMPTY_REPRESENTATION
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class WorkAreaIdOrEmptyJsonDeserializer : JsonDeserializer<WorkAreaIdOrEmpty?>() {

  override fun deserialize(
      parser: JsonParser,
      context: DeserializationContext
  ): WorkAreaIdOrEmpty? {
    val value = parser.valueAsString ?: return null
    return if (value.equals(EMPTY_REPRESENTATION, ignoreCase = true)) {
      WorkAreaIdOrEmpty()
    } else {
      WorkAreaIdOrEmpty(value.asWorkAreaId())
    }
  }
}
