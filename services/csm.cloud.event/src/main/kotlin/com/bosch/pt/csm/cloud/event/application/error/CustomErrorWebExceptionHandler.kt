/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.error

import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import com.bosch.pt.csm.cloud.common.exceptions.TranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_METHOD_NOT_SUPPORTED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_REQUEST_PROCESSING_FAILED
import com.bosch.pt.csm.cloud.event.application.logging.MdcExtractor
import com.bosch.pt.csm.cloud.event.common.exception.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.csm.cloud.event.common.facade.rest.ErrorResource
import datadog.trace.api.GlobalTracer
import java.util.Locale
import java.util.function.Consumer
import org.slf4j.Logger
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.MessageSource
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.util.context.Context

/**
 * This class is based on the [AbstractErrorWebExceptionHandler]. It was not extended because of
 * private methods and attributes we need access to in the custom implementation
 */
@Component // -2 is important to overwrite DefaultErrorWebExceptionHandler
@Order(-2)
class CustomErrorWebExceptionHandler(
    private val messageSource: MessageSource,
    private val serverCodecConfigurer: ServerCodecConfigurer,
    private val logger: Logger,
) : ErrorWebExceptionHandler {

  override fun handle(exchange: ServerWebExchange, throwable: Throwable): Mono<Void> {
    val locale = checkNotNull(exchange.localeContext.locale)
    val status: HttpStatus
    val message: TranslatedErrorMessage
    var headersConsumer = Consumer { _: HttpHeaders -> }

    if (throwable is AuthenticationException) {
      // This part handles exceptions from spring security. They are forwarded here due to a
      // configuration in the WebSecurityConfiguration class
      status = determineHttpStatusFromAuthenticationException(throwable)
      message = determineMessageFromAuthenticationException(throwable, locale)
      val parameters = createParameters(throwable)
      headersConsumer = Consumer { headers: HttpHeaders ->
        headers.add(WWW_AUTHENTICATE, computeWwwAuthenticateHeaderValue(parameters))
      }
    } else {
      status = determineHttpStatus(throwable)
      message = determineMessage(status, throwable, locale)
    }

    return ServerResponse.status(status)
        .headers(headersConsumer)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            BodyInserters.fromValue(
                ErrorResource(message.translatedMessage, GlobalTracer.get().traceId)))
        .contextWrite { ctx: Context -> addToContext(ctx, exchange.request, status) }
        .doOnNext { logError(exchange, throwable, status, message.englishMessage) }
        .flatMap { response: ServerResponse -> write(exchange, response) }
  }

  private fun logError(
      exchange: ServerWebExchange,
      throwable: Throwable,
      status: HttpStatus,
      message: String
  ) {
    if (exchange.response.isCommitted || isDisconnectedClientError(throwable)) {
      logger.warn("Connection was prematurely closed.")
    } else if (status == INTERNAL_SERVER_ERROR) {
      logger.error(message, throwable)
    } else {
      logger.info(message, throwable)
    }
  }

  private fun isDisconnectedClientError(ex: Throwable): Boolean =
      DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.javaClass.getSimpleName()) ||
          isDisconnectedClientErrorMessage(NestedExceptionUtils.getMostSpecificCause(ex).message)

  private fun isDisconnectedClientErrorMessage(message: String?): Boolean =
      (message?.lowercase(Locale.getDefault()) ?: "").let {
        it.contains("broken pipe") || it.contains("connection reset by peer")
      }

  private fun addToContext(
      context: Context,
      request: ServerHttpRequest,
      status: HttpStatus
  ): Context =
      MdcExtractor.addFromRequest(context, request).apply {
        put(RESPONSE_HTTP_STATUS_CODE, status.value().toString())
      }

  private fun determineHttpStatus(error: Throwable): HttpStatus =
      when (error) {
        is ResponseStatusException -> error.statusCode as HttpStatus
        else -> findMergedAnnotation(error.javaClass, ResponseStatus::class.java)?.code
                ?: INTERNAL_SERVER_ERROR
      }

  private fun determineMessage(
      status: HttpStatus,
      throwable: Throwable,
      locale: Locale
  ): TranslatedErrorMessage =
      when (status) {
        NOT_FOUND -> SERVER_ERROR_REQUEST_PROCESSING_FAILED
        METHOD_NOT_ALLOWED -> SERVER_ERROR_METHOD_NOT_SUPPORTED
        else -> SERVER_ERROR_INTERNAL_SERVER_ERROR
      }.let { messageKey ->
        getTranslatedErrorMessage(messageSource, messageKey, locale, throwable.message)
      }

  /** This method is inspired by [BearerTokenServerAuthenticationEntryPoint] */
  private fun determineHttpStatusFromAuthenticationException(
      authException: AuthenticationException
  ): HttpStatus {
    if (authException is OAuth2AuthenticationException) {
      val error = authException.error
      if (error is BearerTokenError) {
        return error.httpStatus
      }
    }
    return UNAUTHORIZED
  }

  /** This method is inspired by [BearerTokenServerAuthenticationEntryPoint] */
  private fun determineMessageFromAuthenticationException(
      authException: AuthenticationException,
      locale: Locale
  ): TranslatedErrorMessage {
    val messageKey =
        if (authException is OAuth2AuthenticationException &&
            authException.error is BearerTokenError) {
          SERVER_ERROR_INVALID_TOKEN
        } else {
          SERVER_ERROR_NO_TOKEN_PROVIDED
        }

    return getTranslatedErrorMessage(messageSource, messageKey, locale, authException.message)
  }

  /** This method is inspired by [BearerTokenServerAuthenticationEntryPoint] */
  private fun createParameters(authException: AuthenticationException): Map<String, String> =
      mutableMapOf<String, String>().apply {
        if (authException is OAuth2AuthenticationException) {
          val error = authException.error
          this["error"] = error.errorCode

          if (error.description?.isNotBlank() == true) {
            this["error_description"] = error.description
          }
          if (error.uri?.isNotBlank() == true) {
            this["error_uri"] = error.uri
          }
          if (error is BearerTokenError && error.scope?.isNotBlank() == true) {
            this["scope"] = error.scope
          }
        }
      }

  private fun write(exchange: ServerWebExchange, response: ServerResponse): Mono<out Void> {
    // force content-type since writeTo won't overwrite response header values
    exchange.response.headers.contentType = response.headers().getContentType()
    return response.writeTo(exchange, ResponseContext())
  }

  private inner class ResponseContext : ServerResponse.Context {

    override fun messageWriters(): List<HttpMessageWriter<*>> = serverCodecConfigurer.writers

    override fun viewResolvers(): List<ViewResolver> = emptyList()
  }

  companion object {

    /** Currently duplicated from Spring WebFlux HttpWebHandlerAdapter. */
    private val DISCONNECTED_CLIENT_EXCEPTIONS: Set<String> =
        setOf("AbortedException", "ClientAbortException", "EOFException", "EofException")

    /** This method is copied from [BearerTokenServerAuthenticationEntryPoint] */
    private fun computeWwwAuthenticateHeaderValue(parameters: Map<String, String>): String =
        if (parameters.isEmpty()) {
          "Bearer"
        } else {
          "Bearer " +
              parameters.entries.joinToString(separator = ", ") { "${it.key}=\"${it.value}\"" }
        }
  }
}
