/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import java.io.IOException
import org.springframework.stereotype.Component

/*
 * Component to implement and add the string json deserializer with trim.
 */
@Component
open class StringTrimModule :
    SimpleModule("String Deserializer", Version(1, 0, 0, null, null, null)) {

  init {
    addDeserializer(String::class.java, StringJsonDeserializer())
  }

  // Inner string json deserializer class that applies the trim function
  class StringJsonDeserializer : JsonDeserializer<String?>() {

    @Throws(IOException::class)
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): String? =
        jsonParser.valueAsString?.trim { it <= ' ' }
  }
}
