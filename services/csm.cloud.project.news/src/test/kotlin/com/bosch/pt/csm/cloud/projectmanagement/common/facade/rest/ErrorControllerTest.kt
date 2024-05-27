/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INTERNAL_SERVER_ERROR
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.core.MethodParameter
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@SmartSiteMockKTest
internal class ErrorControllerTest {

  @MockK(relaxed = true) private lateinit var messageSource: MessageSource

  @Suppress("UnusedPrivateMember")
  @MockK(relaxed = true)
  private lateinit var environment: Environment

  @InjectMockKs private lateinit var cut: ErrorController

  @BeforeEach
  fun initMocks() {

    every { messageSource.getMessage(SERVER_ERROR_BAD_REQUEST, arrayOf(), any()) } returns
        MSG_BAD_REQUEST

    every {
      messageSource.getMessage(SERVER_ERROR_UNSUPPORTED_MEDIA_TYPE, arrayOf(), any())
    } returns MSG_UNSUPPORTED_MEDIATYPE

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

    val methodArgumentNotValidException = MethodArgumentNotValidException(mockk<MethodParameter>(), bindingResult)
    val responseEntity: ResponseEntity<*>
    responseEntity = cut.handleMethodArgumentValidationExceptions(methodArgumentNotValidException)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithHandleIllegalArgumentException() {
    val exception = IllegalArgumentException("IllegalArgumentException")
    val responseEntity = cut.handleAllBadRequestExceptions(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(responseEntity.body?.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithHttpMessageNotReadableException() {
    val exception =
        HttpMessageNotReadableException(
            "HttpMessageNotReadableExc.", MockHttpInputMessage(ByteArray(0)))
    val responseEntity = cut.handleAllBadRequestExceptions(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(responseEntity.body?.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithMethodArgumentNotValidException() {
    val exception: MethodArgumentNotValidException = mockk(relaxed = true)
    val responseEntity = cut.handleMethodArgumentValidationExceptions(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(responseEntity.body?.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyBadRequestWithMethodArgumentTypeMismatchException() {
    val exception: MethodArgumentTypeMismatchException = mockk(relaxed = true)
    // this is needed as a workaround to solve a StackOverflowError in logback
    every { exception.cause } returns null

    val responseEntity = cut.handleAllBadRequestExceptions(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(responseEntity.body?.message).isEqualTo(MSG_BAD_REQUEST)
  }

  @Test
  fun verifyInternalServerErrorForGeneralException() {
    val exception = NullPointerException()
    val responseEntity = cut.handleGeneralException(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    assertThat(responseEntity.body?.message).isNotNull
  }

  @Test
  fun verifyHandleAccessDeniedException() {
    val exception = AccessDeniedException("Denied")
    val responseEntity: ResponseEntity<*> = cut.handleAccessDeniedException(exception)

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
  }

  @Test
  fun verifyHttpMediaTypeNotSupportedException() {
    val responseEntity =
        cut.handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException("Media is unsupported"))

    assertThat(responseEntity).isNotNull
    assertThat(responseEntity.statusCode).isEqualTo(UNSUPPORTED_MEDIA_TYPE)
    assertThat(responseEntity.body).isNotNull
    assertThat(responseEntity.body?.message).isEqualTo(MSG_UNSUPPORTED_MEDIATYPE)
  }

  companion object {
    private const val MSG_BAD_REQUEST = "An invalid request was sent to the server."
    private const val MSG_UNSUPPORTED_MEDIATYPE = "Data have an unsupported media type."
    private const val MSG_INTERNAL_SERVER_ERROR = "An error occurred."
  }
}
