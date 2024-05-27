/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.exceptions.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.exceptions.TranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_ACCESS_DENIED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_METHOD_NOT_SUPPORTED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE
import com.bosch.pt.csm.cloud.common.translation.Key.COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED
import com.bosch.pt.csm.cloud.common.translation.Key.COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import datadog.trace.api.GlobalTracer
import jakarta.validation.ConstraintViolationException
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
class ErrorController(
    private val messageSource: MessageSource,
    private val environment: Environment
) {

  @ExceptionHandler(
      ConstraintViolationException::class,
      HttpMessageNotReadableException::class,
      IllegalArgumentException::class,
      MethodArgumentTypeMismatchException::class,
      MissingKotlinParameterException::class,
      MissingServletRequestParameterException::class)
  fun handleAllBadRequestExceptions(ex: Exception): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_BAD_REQUEST, ex)

    MDC.put(RESPONSE_HTTP_STATUS_CODE, BAD_REQUEST.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, BAD_REQUEST)
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentValidationExceptions(
      ex: MethodArgumentNotValidException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_BAD_REQUEST, ex)

    MDC.put(RESPONSE_HTTP_STATUS_CODE, BAD_REQUEST.value().toString())
    LOGGER.info(
        "Request contained invalid arguments in following fields: {}",
        ex.bindingResult.fieldErrors.map { it.field })
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, BAD_REQUEST)
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
  fun handleHttpMediaTypeNotSupportedException(
      ex: HttpMediaTypeNotSupportedException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE, ex)

    MDC.put(RESPONSE_HTTP_STATUS_CODE, UNSUPPORTED_MEDIA_TYPE.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, UNSUPPORTED_MEDIA_TYPE)
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(ex: Exception): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_ACCESS_DENIED, ex)

    MDC.put(RESPONSE_HTTP_STATUS_CODE, FORBIDDEN.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, FORBIDDEN)
  }

  @ExceptionHandler(EntityOutdatedException::class, OptimisticLockingFailureException::class)
  fun handleOptimisticLockingFailureException(ex: Exception): ResponseEntity<ErrorResource> {
    val translatedErrorMessage =
        getTranslatedErrorMessage(COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, HttpStatus.CONFLICT.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, HttpStatus.CONFLICT)
  }

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleDataIntegrityViolationException(
      ex: DataIntegrityViolationException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage =
        getTranslatedErrorMessage(COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, BAD_REQUEST.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, BAD_REQUEST)
  }

  @ExceptionHandler(PreconditionViolationException::class)
  fun handlePreconditionValidationException(
      ex: PreconditionViolationException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(ex.messageKey, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, BAD_REQUEST.value().toString())
    logInfo("Precondition is not satisfied: ${translatedErrorMessage.englishMessage}", ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, BAD_REQUEST)
  }

  @ExceptionHandler(BlockOperationsException::class)
  fun handleBlockModifyingOperationsException(
      ex: BlockOperationsException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(ex.messageKey, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, SERVICE_UNAVAILABLE.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, SERVICE_UNAVAILABLE)
  }

  @ExceptionHandler(AggregateNotFoundException::class)
  fun handleResourceNotFoundException(
      ex: AggregateNotFoundException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(ex.messageKey, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, NOT_FOUND.value().toString())
    logInfo("Resource not found: ${translatedErrorMessage.englishMessage}", ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, NOT_FOUND)
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleMethodNotSupported(
      ex: HttpRequestMethodNotSupportedException
  ): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_METHOD_NOT_SUPPORTED, ex)

    MDC.put(RESPONSE_HTTP_STATUS_CODE, METHOD_NOT_ALLOWED.value().toString())
    logInfo(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, METHOD_NOT_ALLOWED)
  }

  @ExceptionHandler(ClientAbortException::class)
  @ExcludeFromCodeCoverage
  fun handleClientAbortException(ex: ClientAbortException) {
    MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.value().toString())
    logWarning("ClientAbortException exception occurred", ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
  }

  @ExceptionHandler(Exception::class)
  fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResource> {
    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_INTERNAL_SERVER_ERROR, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.value().toString())
    logError(translatedErrorMessage.englishMessage, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage, INTERNAL_SERVER_ERROR)
  }

  private fun buildErrorResponse(message: TranslatedErrorMessage, status: HttpStatus) =
      ResponseEntity(ErrorResource(message.translatedMessage, GlobalTracer.get().traceId), status)

  private fun getTranslatedErrorMessage(messageKey: String, rootCause: Exception) =
      getTranslatedErrorMessage(
          messageSource, messageKey, LocaleContextHolder.getLocale(), rootCause)

  @ExcludeFromCodeCoverage
  private fun logError(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(PROFILE_LOG_JSON)) {
      LOGGER.error(ERROR_LOG_FORMAT.format(message, throwable.message), throwable)
    } else {
      LOGGER.error(message, throwable)
    }
  }

  @ExcludeFromCodeCoverage
  private fun logWarning(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(PROFILE_LOG_JSON)) {
      LOGGER.warn(ERROR_LOG_FORMAT.format(message, throwable.message), throwable)
    } else {
      LOGGER.warn(message, throwable)
    }
  }

  @ExcludeFromCodeCoverage
  private fun logInfo(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(PROFILE_LOG_JSON)) {
      LOGGER.info(ERROR_LOG_FORMAT.format(message, throwable.message), throwable)
    } else {
      LOGGER.info(message, throwable)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ErrorController::class.java)
    private val PROFILE_LOG_JSON = Profiles.of("log-json")
    private const val ERROR_LOG_FORMAT = "%s : %s"
  }
}
