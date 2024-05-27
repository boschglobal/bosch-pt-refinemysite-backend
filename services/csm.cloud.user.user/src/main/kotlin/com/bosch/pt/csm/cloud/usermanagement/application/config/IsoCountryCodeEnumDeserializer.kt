/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class IsoCountryCodeEnumDeserializer : JsonDeserializer<IsoCountryCodeEnum?>() {

  override fun deserialize(
      parser: JsonParser,
      context: DeserializationContext
  ): IsoCountryCodeEnum? {
    val value = parser.valueAsString?.uppercase() ?: return null
    return IsoCountryCodeEnum.valueOf(value)
  }
}
