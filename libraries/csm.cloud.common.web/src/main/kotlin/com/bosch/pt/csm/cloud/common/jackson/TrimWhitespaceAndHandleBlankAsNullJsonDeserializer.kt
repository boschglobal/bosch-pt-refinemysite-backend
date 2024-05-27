/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
@ConditionalOnProperty(
    "custom.jackson.convertBlankToNull.enabled", havingValue = "true", matchIfMissing = true)
class TrimWhitespaceAndHandleBlankAsNullJsonDeserializer : JsonDeserializer<String>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): String? =
      parser.valueAsString?.trim()?.ifBlank { null }
}
