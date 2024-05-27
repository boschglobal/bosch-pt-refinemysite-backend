/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Unit test to verify correct validations of [StringTrimModule]. */
class StringTrimModuleTest {

  private lateinit var objectMapper: ObjectMapper

  @BeforeEach
  fun setup() {
    objectMapper = ObjectMapper()
    val deserializer: SimpleModule = StringTrimModule()
    objectMapper.registerModule(deserializer)
  }

  @Test
  fun verifyStringJsonDeserializer() {
    val json = "{\"key\" : \"   My String with spaces   \"}"
    val type =
        objectMapper.typeFactory.constructParametricType(
            HashMap::class.java, String::class.java, String::class.java)

    val result = objectMapper.readValue<HashMap<String, String>>(json, type)
    assertThat(result["key"]).isEqualTo(result["key"]!!.trim { it <= ' ' })
  }

  @Test
  fun verifyStringJsonDeserializerWithNull() {
    val json = "{\"key\" : null}"
    val result = objectMapper.readValue(json, String::class.java)
    assertThat(result).isNull()
  }
}
