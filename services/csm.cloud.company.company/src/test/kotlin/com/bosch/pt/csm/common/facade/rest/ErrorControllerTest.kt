/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.bosch.pt.csm.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NOT_ACCEPTABLE
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.csm.common.i18n.Key.COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_MISSING_ADDRESS
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import java.util.Locale.ENGLISH
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.core.MethodParameter
import org.springframework.core.env.Environment
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import org.springframework.http.HttpStatus.NOT_ACCEPTABLE
import org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MultipartException

@SmartSiteMockKTest
class ErrorControllerTest {

  @MockK(relaxed = true) private lateinit var messageSource: MessageSource

  @Suppress("UNUSED", "UnusedPrivateMember")
  @MockK(relaxed = true)
  private lateinit var environment: Environment

  @InjectMockKs private lateinit var cut: ErrorController

  @BeforeEach
  fun initMocks() {
    every { messageSource.getMessage(BLOCK_WRITING_OPERATIONS, arrayOf(), any()) } returns
        MSG_BLOCK_WRITING_OPERATIONS
    every {
      messageSource.getMessage(COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING, arrayOf(), any())
    } returns MSG_OPTIMISTIC_LOCKING_FAILURE
    every {
      messageSource.getMessage(COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED, arrayOf(), any())
    } returns MSG_DATA_INTEGRITY_VIOLATED
    every { messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, arrayOf(), any()) } returns
        MSG_BAD_REQUEST
    every {
      messageSource.getMessage(SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE, arrayOf(), any())
    } returns MSG_UNSUPPORTED_MEDIA_TYPE
    every { messageSource.getMessage(SERVER_ERROR_INTERNAL_SERVER_ERROR, arrayOf(), any()) } returns
        MSG_INTERNAL_SERVER_ERROR
  }

  @Test
  fun verifyHandleMethodArgumentValidationExceptions() {
    val bindingResult: BindingResult = mockk()
    every { bindingResult.fieldErrors } returns
        listOf(
            FieldError("obj1", "field1", "Constraint violation"),
            FieldError("obj2", "field2", "Constraint violation"))

    val method: MethodParameter = mockk()
    val methodArgumentNotValidException = MethodArgumentNotValidException(method, bindingResult)
    val responseEntity: ResponseEntity<*>
    responseEntity = cut.handleMethodArgumentValidationExceptions(methodArgumentNotValidException)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun verifyHttpMediaTypeNotSupportedException() {
    val responseEntity =
        cut.handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException("Media is unsupported"))
    assertThat(responseEntity.statusCode).isEqualTo(UNSUPPORTED_MEDIA_TYPE)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_UNSUPPORTED_MEDIA_TYPE)
  }

  @Test
  fun verifyHttpMediaTypeNotAcceptableException() {
    val responseEntity =
        cut.handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException("Media type not acceptable"))
    assertThat(responseEntity.statusCode).isEqualTo(NOT_ACCEPTABLE)
    assertThat(responseEntity.body!!.message)
        .isEqualTo(messageSource.getMessage(SERVER_ERROR_NOT_ACCEPTABLE, arrayOf(), ENGLISH))
  }

  @Test
  fun verifyPreconditionValidationException() {
    val responseEntity =
        cut.handlePreconditionValidationException(
            PreconditionViolationException(COMPANY_VALIDATION_ERROR_MISSING_ADDRESS))
    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
  }

  @Test
  fun verifyBlockModifyingOperationsException() {
    val responseEntity =
        cut.handleBlockModifyingOperationsException(
            BlockOperationsException(BLOCK_WRITING_OPERATIONS))
    assertThat(responseEntity.statusCode).isEqualTo(SERVICE_UNAVAILABLE)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BLOCK_WRITING_OPERATIONS)
  }

  @Test
  fun verifyGeneralExceptionHandlerBlockModifyingOperationsException() {
    val responseEntity =
        cut.handleGeneralException(BlockOperationsException(BLOCK_WRITING_OPERATIONS))
    assertThat(responseEntity.statusCode).isEqualTo(SERVICE_UNAVAILABLE)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BLOCK_WRITING_OPERATIONS)
  }

  @Test
  fun verifyBadRequestWithHandleIllegalArgumentException() {
    val exception = IllegalArgumentException("IllegalArgumentException")
    val responseEntity = cut.handleAllBadRequestExceptions(exception)
    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithHttpMessageNotReadableException() {
    val exception =
        HttpMessageNotReadableException(
            "HttpMessageNotReadableExc.", MockHttpInputMessage(ByteArray(0)))
    val responseEntity = cut.handleAllBadRequestExceptions(exception)
    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithMethodArgumentNotValidException() {
    val exception: MethodArgumentNotValidException = mockk(relaxed = true)
    val responseEntity = cut.handleMethodArgumentValidationExceptions(exception)
    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithMethodArgumentTypeMismatchException() {
    val exception: MethodArgumentTypeMismatchException = mockk(relaxed = true)
    // this is needed as a workaround to solve a StackOverflowError in logback
    every { exception.cause } returns null

    val responseEntity = cut.handleAllBadRequestExceptions(exception)

    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyInternalServerErrorForGeneralException() {
    val responseEntity = cut.handleGeneralException(NullPointerException())
    assertThat(responseEntity.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
    assertThat(responseEntity.body!!.message).isNotNull
  }

  @Test
  fun verifyHandleFileSizeExceededException() {
    val nested2 =
        SizeLimitExceededException(
            "the request was rejected because its size (100) exceeds the configured maximum (1000)",
            100,
            1000)
    val nested1 = IllegalStateException(nested2)
    val exception = MultipartException("Test", nested1)
    val responseEntity = cut.handleFileSizeExceededException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(PAYLOAD_TOO_LARGE)
  }

  @Test
  fun verifyHandleFileSizeExceededExceptionCauseNotIllegalStateException() {
    val exception = MultipartException("Test")
    val responseEntity = cut.handleFileSizeExceededException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
  }

  @Test
  fun verifyHandleFileSizeExceededExceptionCauseNotSizeLimitExceeded() {
    val cause = IllegalStateException("Cause")
    val exception = MultipartException("Test", cause)
    val responseEntity = cut.handleFileSizeExceededException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
  }

  @Test
  fun verifyHandleAccessDeniedException() {
    val exception = AccessDeniedException("Denied")
    val responseEntity: ResponseEntity<*> = cut.handleAccessDeniedException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(FORBIDDEN)
  }

  @Test
  fun verifyHandleOptimisticLockingFailureException() {
    val exception = OptimisticLockingFailureException(COMMON_VALIDATION_ERROR_OPTIMISTIC_LOCKING)
    val responseEntity = cut.handleOptimisticLockingFailureException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(CONFLICT)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_OPTIMISTIC_LOCKING_FAILURE)
  }

  @Test
  fun verifyHandleEntityOutdatedException() {
    val exception = EntityOutdatedException(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
    val responseEntity = cut.handleOptimisticLockingFailureException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(CONFLICT)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_OPTIMISTIC_LOCKING_FAILURE)
  }

  @Test
  fun verifyHandleDataIntegrityViolationException() {
    val exception = DataIntegrityViolationException(COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED)
    val responseEntity = cut.handleDataIntegrityViolationException(exception)
    assertThat(responseEntity.statusCode).isEqualTo(BAD_REQUEST)
    assertThat(responseEntity.body!!.message).isEqualTo(MSG_DATA_INTEGRITY_VIOLATED)
  }

  @Test
  fun verifyHandleMethodNotAllowedException() {
    val exception = HttpRequestMethodNotSupportedException("Blah")
    val responseEntity = cut.handleMethodNotSupported(exception)
    assertThat(responseEntity.statusCode).isEqualTo(METHOD_NOT_ALLOWED)
  }

  @Test
  fun verifyHandleExternalHttpClientUnauthorizedExceptions() {
    val exception = HttpClientErrorException(UNAUTHORIZED)
    val responseEntity = cut.handleExternalHttpClientUnauthorizedExceptions(exception)
    assertThat(responseEntity.statusCode).isEqualTo(UNAUTHORIZED)
  }

  @Test
  fun verifyHandleExternalHttpServerServiceUnavailableExceptions() {
    val exception = HttpClientErrorException(SERVICE_UNAVAILABLE)
    val responseEntity = cut.handleExternalHttpServerServiceUnavailableExceptions(exception)
    assertThat(responseEntity.statusCode).isEqualTo(SERVICE_UNAVAILABLE)
  }

  companion object {
    private const val MSG_BLOCK_WRITING_OPERATIONS =
        "Due to maintenance work, changes to the data are not possible at the moment."
    private const val MSG_OPTIMISTIC_LOCKING_FAILURE = "Optimistic locking failure."
    private const val MSG_DATA_INTEGRITY_VIOLATED = "Integrity violated."
    private const val MSG_BAD_REQUEST = "An invalid request was sent to the server."
    private const val MSG_UNSUPPORTED_MEDIA_TYPE = "Data have an unsupported media type."
    private const val MSG_INTERNAL_SERVER_ERROR = "An error occurred."
  }
}
