/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ErrorController(private val environment: Environment) {

  @ExceptionHandler(Exception::class)
  fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResource> {
    MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.toString())
    logError(ex.message, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)

    return ResponseEntity(
        ErrorResource(ExceptionUtils.getRootCause(ex).message ?: "", ""), INTERNAL_SERVER_ERROR)
  }

  @ExcludeFromCodeCoverage
  private fun logError(message: String?, throwable: Throwable) =
      if (environment.acceptsProfiles(Profiles.of(LOG_JSON_PROFILE))) {
        LOGGER.error(String.format(ERROR_LOG_MESSAGE, message, throwable.message), throwable)
      } else {
        LOGGER.error(message, throwable)
      }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ErrorController::class.java)

    private const val LOG_JSON_PROFILE = "log-json"
    private const val ERROR_LOG_MESSAGE = "%s : %s"
  }
}
