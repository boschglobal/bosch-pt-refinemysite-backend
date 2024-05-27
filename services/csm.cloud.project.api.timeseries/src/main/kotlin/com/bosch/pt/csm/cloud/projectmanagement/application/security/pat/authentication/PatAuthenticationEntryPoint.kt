/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.common.exceptions.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import com.bosch.pt.csm.cloud.common.facade.rest.ErrorResource
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_LOCKED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_NOT_REGISTERED
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatTokenError
import com.bosch.pt.csm.cloud.projectmanagement.common.translation.Key.PAT_EXPIRED
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.GlobalTracer
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.AuthenticationEntryPoint

class PatAuthenticationEntryPoint(
    private val messageSource: MessageSource,
    private val environment: Environment
) : AuthenticationEntryPoint {

  override fun commence(
      request: HttpServletRequest,
      response: HttpServletResponse,
      authException: AuthenticationException
  ) {
    var status: HttpStatus = UNAUTHORIZED
    var messageKey: String = SERVER_ERROR_NO_TOKEN_PROVIDED

    when (authException) {
      is UsernameNotFoundException -> {
        status = FORBIDDEN
        messageKey = SERVER_ERROR_USER_NOT_REGISTERED
      }
      is LockedException -> {
        status = FORBIDDEN
        messageKey = SERVER_ERROR_USER_LOCKED
      }
      is CredentialsExpiredException -> {
        status = FORBIDDEN
        messageKey = PAT_EXPIRED
      }
      is PatAuthenticationException -> {
        val error = authException.error
        val parameters =
            mutableMapOf<String, String?>().apply {
              this["error"] = error.errorCode
              error.description?.ifBlank { null }?.let { this["error_description"] = it }
              error.uri?.ifBlank { null }?.let { this["error_uri"] = it }
            }

        if (error is PatTokenError) {
          status = error.httpStatus
          messageKey = SERVER_ERROR_INVALID_TOKEN
        }

        val wwwAuthenticate = computeAuthenticateHeaderValue(parameters)
        response.addHeader(WWW_AUTHENTICATE, wwwAuthenticate)
      }
      else -> {
        val wwwAuthenticate = computeAuthenticateHeaderValue(emptyMap())
        response.addHeader(WWW_AUTHENTICATE, wwwAuthenticate)
      }
    }
    response.status = status.value()
    buildErrorBody(response, messageKey, authException)
  }

  private fun buildErrorBody(
      response: HttpServletResponse,
      messageKey: String,
      authException: AuthenticationException
  ) {
    response.contentType = APPLICATION_JSON_VALUE
    val locale = LocaleContextHolder.getLocale()
    val message = getTranslatedErrorMessage(messageSource, messageKey, locale, authException)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, response.status.toString())
    logInfo(message.englishMessage, authException)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    response.writer.use {
      val errorResource = ErrorResource(message.translatedMessage, GlobalTracer.get().traceId)
      ObjectMapper().apply { writeValue(it, errorResource) }
    }
  }

  private fun logInfo(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(Profiles.of("log-json"))) {
      LOGGER.info("{} {}", message, throwable.message)
    } else {
      LOGGER.info(message, throwable)
    }
  }

  companion object {

    private val LOGGER = LoggerFactory.getLogger(PatAuthenticationEntryPoint::class.java)

    private fun computeAuthenticateHeaderValue(parameters: Map<String, String?>): String =
        if (parameters.isNotEmpty()) {
          "PAT " +
              parameters.entries.joinToString(separator = ", ") { (key, value) ->
                "$key=\"$value\""
              }
        } else {
          "Unauthorized"
        }
  }
}
