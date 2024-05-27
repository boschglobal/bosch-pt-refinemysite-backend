/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.config

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import datadog.trace.api.GlobalTracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Locale
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.lang.NonNull
import org.springframework.web.filter.OncePerRequestFilter

class ExceptionMessageHideFilter(private val messageSource: MessageSource) :
    OncePerRequestFilter() {

  public override fun doFilterInternal(
      @NonNull request: HttpServletRequest,
      @NonNull response: HttpServletResponse,
      @NonNull filterChain: FilterChain
  ) {
    try {
      filterChain.doFilter(request, response)
    } catch (ex: BlockOperationsException) {
      MDC.put(RESPONSE_HTTP_STATUS_CODE, SERVICE_UNAVAILABLE.toString())
      val logMessage = messageSource.getMessage(ex.messageKey, arrayOf(), Locale.ENGLISH)
      LOGGER.info(logMessage, ex)
      MDC.remove(RESPONSE_HTTP_STATUS_CODE)
      response.status = SERVICE_UNAVAILABLE.value()
      response.contentType = APPLICATION_JSON_VALUE
      messageSource.getMessage(ex.messageKey, arrayOf(), LocaleContextHolder.getLocale()).let {
        response.writer.write(
            "{\"message\":\" $it \",\"traceId\":\"${GlobalTracer.get().traceId}\"}")
      }
    } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
      MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.toString())
      LOGGER.warn(ex.message, ex)
      MDC.remove(RESPONSE_HTTP_STATUS_CODE)
      response.status = INTERNAL_SERVER_ERROR.value()
      response.contentType = APPLICATION_JSON_VALUE
      response.writer.write(
          "{\"message\":\"Internal error occurred.\",\"traceId\":\"${GlobalTracer.get().traceId}\"}")
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ExceptionMessageHideFilter::class.java)
  }
}
