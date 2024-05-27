/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.stereotype.Component

/*
 * Component to implement and add the string json deserializer with trim.
 */
@Component
class StringTrimModule : SimpleModule("String Deserializer", Version(1, 0, 0, null, null, null)) {

  init {
    addDeserializer(String::class.java, StringJsonDeserializer())
  }

  // Inner string json deserializer class that applies the trim function
  internal class StringJsonDeserializer : JsonDeserializer<String?>() {

    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): String? =
        jsonParser.valueAsString?.trim { it <= ' ' }
  }
}
