/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.exceptions.CommonErrorUtil.getTranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.exceptions.MdcConstants.RESPONSE_HTTP_STATUS_CODE
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.exceptions.TranslatedErrorMessage
import com.bosch.pt.csm.cloud.common.facade.rest.ErrorResource
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_ACCESS_DENIED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_METHOD_NOT_SUPPORTED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NOT_ACCEPTABLE
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_REQUEST_PROCESSING_FAILED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_SERVICE_NOT_AVAILABLE
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE
import com.bosch.pt.csm.common.exceptions.ReferencedEntityNotFoundException
import com.bosch.pt.csm.common.facade.rest.ErrorController.LogLevel.ERROR
import com.bosch.pt.csm.common.facade.rest.ErrorController.LogLevel.INFO
import com.bosch.pt.csm.common.facade.rest.ErrorController.LogLevel.WARN
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import datadog.trace.api.GlobalTracer
import jakarta.validation.ConstraintViolationException
import org.apache.catalina.connector.ClientAbortException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException
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
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_ACCEPTABLE
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MultipartException

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
  fun handleAllBadRequestExceptions(ex: Exception): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(INFO, SERVER_ERROR_BAD_REQUEST, BAD_REQUEST, ex)

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentValidationExceptions(
      ex: MethodArgumentNotValidException
  ): ResponseEntity<ErrorResource> {
    val illegalFields: List<String> = ex.bindingResult.fieldErrors.map { it.field }

    val translatedErrorMessage = getTranslatedErrorMessage(SERVER_ERROR_BAD_REQUEST, null, ex)
    MDC.put(RESPONSE_HTTP_STATUS_CODE, BAD_REQUEST.value().toString())
    // Don't log the exception to not log user input
    LOGGER.info("Request contained invalid arguments in following fields: {}", illegalFields)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(translatedErrorMessage.translatedMessage, BAD_REQUEST)
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
  fun handleHttpMediaTypeNotSupportedException(
      ex: HttpMediaTypeNotSupportedException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          INFO, SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE, ex)

  @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
  fun handleMediaTypeNotAcceptable(
      ex: HttpMediaTypeNotAcceptableException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(INFO, SERVER_ERROR_NOT_ACCEPTABLE, NOT_ACCEPTABLE, ex)

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(ex: Exception): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(INFO, SERVER_ERROR_ACCESS_DENIED, FORBIDDEN, ex)

  @ExceptionHandler(EntityOutdatedException::class, OptimisticLockingFailureException::class)
  fun handleOptimisticLockingFailureException(ex: Exception): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          INFO, COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING, CONFLICT, ex)

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleDataIntegrityViolationException(
      ex: DataIntegrityViolationException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          INFO, COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED, BAD_REQUEST, ex)

  @ExceptionHandler(ReferencedEntityNotFoundException::class)
  fun handleReferencedObjectNotFoundException(
      ex: ReferencedEntityNotFoundException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          INFO, COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED, CONFLICT, ex)

  @ExceptionHandler(MultipartException::class)
  fun handleFileSizeExceededException(ex: MultipartException): ResponseEntity<*> {
    if (ex.cause is IllegalStateException) {
      val ise = ex.cause as IllegalStateException?
      if (ise?.cause is SizeLimitExceededException) {
        logInfo("File size exceeded for API call", ex)
        return ResponseEntity.status(PAYLOAD_TOO_LARGE).build<Any>()
      }
    }
    MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.toString())
    logInfo("Multipart exception occurred", ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return ResponseEntity.status(INTERNAL_SERVER_ERROR).build<Any>()
  }

  @ExceptionHandler(PreconditionViolationException::class)
  fun handlePreconditionValidationException(
      ex: PreconditionViolationException
  ): ResponseEntity<ErrorResource> =
      getTranslatedErrorMessage(ex.messageKey, SERVER_ERROR_REQUEST_PROCESSING_FAILED, ex).let {
        logMessageAndBuildErrorResponse(
            INFO,
            "Precondition is not satisfied: ${it.englishMessage}",
            it.translatedMessage,
            BAD_REQUEST,
            ex)
      }

  @ExceptionHandler(BlockOperationsException::class)
  fun handleBlockModifyingOperationsException(
      ex: BlockOperationsException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(INFO, ex.messageKey, SERVICE_UNAVAILABLE, ex)

  @ExceptionHandler(AggregateNotFoundException::class)
  fun handleResourceNotFoundException(
      ex: AggregateNotFoundException
  ): ResponseEntity<ErrorResource> =
      getTranslatedErrorMessage(ex.messageKey, SERVER_ERROR_REQUEST_PROCESSING_FAILED, ex).let {
        logMessageAndBuildErrorResponse(
            INFO, "Resource not found: ${it.englishMessage}", it.translatedMessage, NOT_FOUND, ex)
      }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleMethodNotSupported(
      ex: HttpRequestMethodNotSupportedException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          INFO, SERVER_ERROR_METHOD_NOT_SUPPORTED, METHOD_NOT_ALLOWED, ex)

  @ExceptionHandler(Exception::class)
  fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResource> =
      if (ExceptionUtils.getRootCause(ex) is BlockOperationsException) {
        // Handle separately if transaction is rolled back throwing TransactionSystemException
        // (happens
        // for example if a task is started) and the root cause is the blocking of modifying
        // operations
        handleBlockModifyingOperationsException(
            ExceptionUtils.getRootCause(ex) as BlockOperationsException)
      } else {
        logMessageKeyAndBuildErrorResponse(
            ERROR, SERVER_ERROR_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, ex)
      }

  @ExceptionHandler(HttpClientErrorException.Unauthorized::class)
  fun handleExternalHttpClientUnauthorizedExceptions(
      ex: HttpClientErrorException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(INFO, SERVER_ERROR_INVALID_TOKEN, UNAUTHORIZED, ex)

  @ExceptionHandler(HttpServerErrorException.ServiceUnavailable::class)
  fun handleExternalHttpServerServiceUnavailableExceptions(
      ex: HttpClientErrorException
  ): ResponseEntity<ErrorResource> =
      logMessageKeyAndBuildErrorResponse(
          WARN, SERVER_ERROR_SERVICE_NOT_AVAILABLE, SERVICE_UNAVAILABLE, ex)

  @ExceptionHandler(ClientAbortException::class)
  @ExcludeFromCodeCoverage
  fun handleClientAbortException(ex: ClientAbortException) {
    MDC.put(RESPONSE_HTTP_STATUS_CODE, INTERNAL_SERVER_ERROR.value().toString())
    logWarning("ClientAbortException exception occurred", ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
  }

  private fun logMessageKeyAndBuildErrorResponse(
      level: LogLevel,
      messageKey: String,
      responseStatus: HttpStatus,
      ex: Exception
  ): ResponseEntity<ErrorResource> =
      getTranslatedErrorMessage(messageKey, null, ex).let {
        logMessageAndBuildErrorResponse(
            level, it.englishMessage, it.translatedMessage, responseStatus, ex)
      }

  private fun logMessageAndBuildErrorResponse(
      level: LogLevel,
      messageEnglish: String,
      messageUserLanguage: String,
      responseStatus: HttpStatus,
      ex: Exception
  ): ResponseEntity<ErrorResource> {
    MDC.put(RESPONSE_HTTP_STATUS_CODE, responseStatus.value().toString())
    log(level, messageEnglish, ex)
    MDC.remove(RESPONSE_HTTP_STATUS_CODE)
    return buildErrorResponse(messageUserLanguage, responseStatus)
  }

  private fun buildErrorResponse(
      message: String,
      status: HttpStatus
  ): ResponseEntity<ErrorResource> =
      ResponseEntity(ErrorResource(message, GlobalTracer.get().traceId), status)

  private fun getTranslatedErrorMessage(
      messageKey: String?,
      alternateMessageKey: String?,
      rootCause: Exception
  ): TranslatedErrorMessage =
      getTranslatedErrorMessage(
          messageSource,
          (messageKey ?: alternateMessageKey)!!,
          LocaleContextHolder.getLocale(),
          rootCause)

  private fun log(level: LogLevel, message: String, throwable: Throwable) =
      when (level) {
        INFO -> logInfo(message, throwable)
        WARN -> logWarning(message, throwable)
        ERROR -> logError(message, throwable)
      }

  @ExcludeFromCodeCoverage
  private fun logError(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(Profiles.of(LOG_JSON_PROFILE))) {
      LOGGER.error(String.format(ERROR_LOG_MESSAGE, message, throwable.message), throwable)
    } else {
      LOGGER.error(message, throwable)
    }
  }

  @ExcludeFromCodeCoverage
  private fun logWarning(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(Profiles.of(LOG_JSON_PROFILE))) {
      LOGGER.warn(String.format(ERROR_LOG_MESSAGE, message, throwable.message), throwable)
    } else {
      LOGGER.warn(message, throwable)
    }
  }

  @ExcludeFromCodeCoverage
  private fun logInfo(message: String, throwable: Throwable) {
    if (environment.acceptsProfiles(Profiles.of(LOG_JSON_PROFILE))) {
      LOGGER.info(String.format(ERROR_LOG_MESSAGE, message, throwable.message))
    } else {
      LOGGER.info(message, throwable)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ErrorController::class.java)
    private const val LOG_JSON_PROFILE = "log-json"
    private const val ERROR_LOG_MESSAGE = "%s : %s"
  }

  enum class LogLevel {
    INFO,
    WARN,
    ERROR
  }
}
