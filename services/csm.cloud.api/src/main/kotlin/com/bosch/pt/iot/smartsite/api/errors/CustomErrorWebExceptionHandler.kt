/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.errors

import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_ACCESS_DENIED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_API_VERSION_NOT_SUPPORTED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_METHOD_NOT_SUPPORTED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_REQUEST_PROCESSING_FAILED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_SERVICE_NOT_AVAILABLE
import com.bosch.pt.iot.smartsite.api.errors.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.iot.smartsite.api.logging.MdcExtractor
import com.bosch.pt.iot.smartsite.api.versioning.UnsupportedApiVersionException
import datadog.trace.api.GlobalTracer
import java.util.Locale
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.MessageSource
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation
import org.springframework.http.HttpStatus.BAD_GATEWAY
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.EXPECTATION_FAILED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.lang.NonNull
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartException
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClientResponseException
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
@Component
class CustomErrorWebExceptionHandler(
    private val messageSource: MessageSource,
    private val serverCodecConfigurer: ServerCodecConfigurer,
    private val logger: Logger,
) : ErrorWebExceptionHandler {

  override fun handle(exchange: ServerWebExchange, throwable: Throwable): Mono<Void> {
    val request = exchange.request
    val status = determineHttpStatus(throwable)
    val message = determineMessage(throwable, exchange.localeContext.locale, status)

    // Check if an invalid authorization is causing the issue. Can happen if e.g. the refresh token
    // is invalidated before a request uses it
    if (throwable is ClientAuthorizationException) {
      return handleAuthenticationError(exchange, throwable, status, message, request)
    }

    return response(status, message, request, exchange, throwable)
  }

  /**
   * Handle Authentication errors by
   * 1. Logging a warning with the reason the call failed (to investigate further)
   * 2. Invalidate session, as it is in an undefined state (no authorizedClient present)
   */
  private fun handleAuthenticationError(
      exchange: ServerWebExchange,
      throwable: ClientAuthorizationException,
      status: HttpStatusCode,
      message: String,
      request: ServerHttpRequest
  ): Mono<Void> {
    logger.warn(
        "Error in authentication flow - error: {}, uri: {}, invalidating session",
        throwable.error,
        throwable.error.uri)
    return exchange.session
        .flatMap { it.invalidate() }
        .then(response(status, message, request, exchange, throwable))
  }

  private fun response(
      status: HttpStatusCode,
      message: String,
      request: ServerHttpRequest,
      exchange: ServerWebExchange,
      throwable: Throwable
  ) =
      ServerResponse.status(status)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(ErrorResource(message, GlobalTracer.get().traceId)))
          .contextWrite { ctx: Context -> addToContext(ctx, request, status) }
          .doOnNext { logError(exchange, throwable, status, message) }
          .flatMap { response: ServerResponse -> write(exchange, response) }

  private fun logError(
      exchange: ServerWebExchange,
      throwable: Throwable,
      status: HttpStatusCode,
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
      (DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.javaClass.simpleName) ||
          isDisconnectedClientErrorMessage(NestedExceptionUtils.getMostSpecificCause(ex).message))

  private fun isDisconnectedClientErrorMessage(message: String?): Boolean =
      (message?.lowercase(Locale.getDefault()) ?: "").let {
        it.contains("broken pipe") || it.contains("connection reset by peer")
      }

  private fun addToContext(
      context: Context,
      request: ServerHttpRequest,
      status: HttpStatusCode
  ): Context {
    var requestContext = context
    requestContext = MdcExtractor.addFromRequest(requestContext, request)
    requestContext = requestContext.put(MdcConstants.RESPONSE_HTTP_STATUS_CODE, status.toString())
    return requestContext
  }

  private fun determineHttpStatus(error: Throwable): HttpStatusCode =
      when (error) {
        is ClientAuthorizationException -> UNAUTHORIZED
        is IllegalArgumentException -> BAD_REQUEST
        is MultipartException -> PAYLOAD_TOO_LARGE
        is ResponseStatusException -> error.statusCode
        is UnsupportedApiVersionException -> EXPECTATION_FAILED
        is WebClientResponseException.Unauthorized -> UNAUTHORIZED
        else ->
            findMergedAnnotation(error.javaClass, ResponseStatus::class.java)?.code
                ?: INTERNAL_SERVER_ERROR
      }

  private fun determineMessage(
      throwable: Throwable,
      locale: Locale?,
      status: HttpStatusCode
  ): String =
      when (status) {
        NOT_FOUND -> SERVER_ERROR_REQUEST_PROCESSING_FAILED
        BAD_GATEWAY -> SERVER_ERROR_SERVICE_NOT_AVAILABLE
        SERVICE_UNAVAILABLE -> SERVER_ERROR_SERVICE_NOT_AVAILABLE
        BAD_REQUEST -> SERVER_ERROR_BAD_REQUEST
        FORBIDDEN -> SERVER_ERROR_ACCESS_DENIED
        METHOD_NOT_ALLOWED -> SERVER_ERROR_METHOD_NOT_SUPPORTED
        UNAUTHORIZED -> SERVER_ERROR_INVALID_TOKEN
        EXPECTATION_FAILED -> SERVER_ERROR_API_VERSION_NOT_SUPPORTED
        else -> SERVER_ERROR_INTERNAL_SERVER_ERROR
      }.let { messageKey ->
        getTranslatedErrorMessage(messageSource, messageKey, locale, throwable.message)
            .translatedMessage
            .let { message ->
              // post-process message to replace placeholders
              if (status == EXPECTATION_FAILED) {
                val exception = throwable as UnsupportedApiVersionException
                return String.format(message, exception.minVersion, exception.maxVersion)
              }
              message
            }
      }

  private fun write(exchange: ServerWebExchange, response: ServerResponse): Mono<out Void> =
      exchange
          .apply {
            // force content-type since writeTo won't overwrite response header values
            this.response.headers.contentType = response.headers().contentType
          }
          .let { response.writeTo(exchange, ResponseContext()) }

  private inner class ResponseContext : ServerResponse.Context {

    @NonNull
    override fun messageWriters(): List<HttpMessageWriter<*>> = serverCodecConfigurer.writers

    @NonNull override fun viewResolvers(): List<ViewResolver> = emptyList()
  }

  companion object {
    /** Currently duplicated from Spring WebFlux HttpWebHandlerAdapter. */
    private val DISCONNECTED_CLIENT_EXCEPTIONS =
        setOf("AbortedException", "ClientAbortException", "EOFException", "EofException")
  }
}
