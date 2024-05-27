/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.service

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Implementation of import service which can optionally be fault tolerant, meaning that data import
 * of elements is continued even if one object cannot be imported successfully. Invalid objects are
 * just skipped.
 */
@Service
class FaultTolerantImportService(
    @Value("\${skip-invalid}") private val skipInvalid: Boolean = false
) {

  fun <T : ImportObject> importData(data: T, function: Consumer<T>) =
      if (skipInvalid) {
        try {
          function.accept(data)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
          LOGGER.error("{} {} could not be imported", data.javaClass.simpleName, data.id)
          LOGGER.error("Error Message: {}", e.message)
        }
      } else {
        function.accept(data)
      }

  fun <T : ImportObject, U> importData(data: T, config: U, function: BiConsumer<T, U>) =
      if (skipInvalid) {
        try {
          function.accept(data, config)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
          LOGGER.error("{} {} could not be imported", data.javaClass.simpleName, data.id)
          LOGGER.error("Error Message: {}", e.message)
        }
      } else {
        function.accept(data, config)
      }

  fun <T> run(
      ignoreFailing: Boolean? = false,
      errorMessage: String? = "Data couldn't be loaded",
      function: Supplier<T>
  ): T? =
      if (skipInvalid || ignoreFailing == true) {
        try {
          function.get()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
          LOGGER.error(errorMessage)
          LOGGER.error("Error Message: {}", e.message)
          null
        }
      } else {
        function.get()
      }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(FaultTolerantImportService::class.java)
  }
}
