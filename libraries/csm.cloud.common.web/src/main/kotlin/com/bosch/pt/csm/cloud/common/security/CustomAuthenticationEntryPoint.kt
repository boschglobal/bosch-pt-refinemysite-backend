/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.security

import com.bosch.pt.csm.cloud.common.exceptions.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import com.bosch.pt.csm.cloud.common.facade.rest.ErrorResource
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_LOCKED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_NOT_REGISTERED
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.GlobalTracer
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.lang.NonNull
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.util.StringUtils

class CustomAuthenticationEntryPoint(
    private val messageSource: MessageSource,
    private val environment: Environment
) : AuthenticationEntryPoint {

  private var realmName: String? = null

  override fun commence(
      request: HttpServletRequest,
      response: HttpServletResponse,
      authException: AuthenticationException
  ) {
    var status = UNAUTHORIZED
    var messageKey: String = SERVER_ERROR_NO_TOKEN_PROVIDED
    val parameters: MutableMap<String, String?> = LinkedHashMap()
    when (authException) {
      is UsernameNotFoundException -> {
        status = FORBIDDEN
        messageKey = SERVER_ERROR_USER_NOT_REGISTERED
      }
      is LockedException -> {
        status = FORBIDDEN
        messageKey = SERVER_ERROR_USER_LOCKED
      }
      else -> {
        if (realmName != null) {
          parameters["realm"] = realmName
        }
        if (authException is OAuth2AuthenticationException) {
          val error = authException.error
          parameters["error"] = error.errorCode
          if (StringUtils.hasText(error.description)) {
            parameters["error_description"] = error.description
          }
          if (StringUtils.hasText(error.uri)) {
            parameters["error_uri"] = error.uri
          }
          if (error is BearerTokenError) {
            if (StringUtils.hasText(error.scope)) {
              parameters["scope"] = error.scope
            }
            status = error.httpStatus
            messageKey = SERVER_ERROR_INVALID_TOKEN
          }
        }
        val wwwAuthenticate = computeAuthenticateHeaderValue(parameters)
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate)
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
      val objectMapper = ObjectMapper()
      val errorResource = ErrorResource(message.translatedMessage, GlobalTracer.get().traceId)
      objectMapper.writeValue(it, errorResource)
    }
  }

  /**
   * Set the default realm name to use in the bearer token error response
   *
   * @param realmName name of realm
   */
  fun setRealmName(realmName: String?) {
    this.realmName = realmName
  }

  private fun logInfo(@NonNull message: String, @NonNull throwable: Throwable) {
    if (environment.acceptsProfiles(Profiles.of("log-json"))) {
      LOGGER.info("{} {}", message, throwable.message)
    } else {
      LOGGER.info(message, throwable)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CustomAuthenticationEntryPoint::class.java)
    private fun computeAuthenticateHeaderValue(parameters: Map<String, String?>): String {
      var wwwAuthenticate = "Bearer"
      if (parameters.isNotEmpty()) {
        wwwAuthenticate +=
            parameters.entries
                .stream()
                .map { (key, value) -> "$key=\"$value\"" }
                .collect(Collectors.joining(", ", " ", ""))
      }
      return wwwAuthenticate
    }
  }
}
