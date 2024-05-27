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
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class WorkAreaIdOrEmptyJsonSerializer : JsonSerializer<WorkAreaIdOrEmpty>() {

  override fun serialize(
      value: WorkAreaIdOrEmpty,
      generator: JsonGenerator,
      serializers: SerializerProvider
  ) {
    generator.writeString(if (value.isEmpty) EMPTY_REPRESENTATION else value.identifier.toString())
  }
}
