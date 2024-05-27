/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.importer.boundary.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.IOException
import org.springframework.core.io.Resource

object JsonResourceReader {

  /**
   * Read resource with JSON objects.
   *
   * @param resource the resource to read objects from
   * @param clazz type reference of expected type of data in resource
   * @param <T> expected type of data in resource
   * @return instance of expected type as defined in expected type reference </T>
   */
  fun <T> read(resource: Resource, clazz: TypeReference<T>): T {
    val objectMapper =
        ObjectMapper().apply {
          registerKotlinModule()
          registerModule(JavaTimeModule())
        }
    try {
      resource.inputStream.use {
        return objectMapper.readValue(it, clazz)
      }
    } catch (e: IOException) {
      throw IllegalArgumentException("Invalid resource provided", e)
    }
  }
}
