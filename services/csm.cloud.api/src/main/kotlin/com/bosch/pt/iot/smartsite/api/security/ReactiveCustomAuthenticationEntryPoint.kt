/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_LOCKED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_NOT_REGISTERED
import com.bosch.pt.iot.smartsite.api.errors.CommonErrorUtil
import com.bosch.pt.iot.smartsite.api.errors.ErrorResource
import datadog.trace.api.GlobalTracer
import org.springframework.context.MessageSource
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Hints
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Reactive implementation of CustomAuthenticationEntryPoint, which will return a message and the
 * correct Status Code for errors in the authentication flow
 */
class ReactiveCustomAuthenticationEntryPoint(
    private val messageSource: MessageSource,
) : ServerAuthenticationEntryPoint {

  override fun commence(
      exchange: ServerWebExchange,
      authException: AuthenticationException
  ): Mono<Void> {

    var status = HttpStatus.UNAUTHORIZED
    var messageKey: String = SERVER_ERROR_NO_TOKEN_PROVIDED

    val parameters: MutableMap<String, String> = mutableMapOf()

    when (authException) {
      is UsernameNotFoundException -> {
        status = HttpStatus.FORBIDDEN
        messageKey = SERVER_ERROR_USER_NOT_REGISTERED
      }
      is LockedException -> {
        status = HttpStatus.FORBIDDEN
        messageKey = SERVER_ERROR_USER_LOCKED
      }
      is OAuth2AuthenticationException -> {
        val error = authException.error
        parameters["error"] = error.errorCode
        if (!error.description.isNullOrBlank()) parameters["description"] = error.description
        if (!error.uri.isNullOrBlank()) parameters["error_uri"] = error.uri
        if (error is BearerTokenError && !error.scope.isNullOrBlank()) {
          parameters["scope"] = error.scope
          status = error.httpStatus
          messageKey = SERVER_ERROR_INVALID_TOKEN
        }
      }
    }
    val wwwAuthenticate: String = computeAuthenticateHeaderValue(parameters)
    exchange.response.headers[WWW_AUTHENTICATE] = wwwAuthenticate

    exchange.response.statusCode = status
    return buildErrorBody(exchange, messageKey, authException)
  }

  private fun computeAuthenticateHeaderValue(parameters: Map<String, String>): String {
    var wwwAuthenticate = "Bearer"
    if (parameters.isNotEmpty()) wwwAuthenticate += parameters.entries.joinToString(", ", " ", "")
    return wwwAuthenticate
  }

  private fun buildErrorBody(
      exchange: ServerWebExchange,
      messageKey: String,
      authException: AuthenticationException
  ): Mono<Void> {
    val locale = exchange.localeContext.locale
    exchange.response.headers[CONTENT_TYPE] = APPLICATION_JSON_VALUE
    val message =
        CommonErrorUtil.getTranslatedErrorMessage(
            messageSource, messageKey, locale, authException.message)
    val errorResource = ErrorResource(message.translatedMessage, GlobalTracer.get().traceId)

    return exchange.response.writeWith(
        Jackson2JsonEncoder()
            .encode(
                Mono.just(errorResource),
                exchange.response.bufferFactory(),
                ResolvableType.forInstance(errorResource),
                APPLICATION_JSON,
                Hints.from(Hints.LOG_PREFIX_HINT, exchange.logPrefix)))
  }
}
