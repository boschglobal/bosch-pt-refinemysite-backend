/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.exceptions

import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException

object CommonErrorUtil {

  private val LOGGER = LoggerFactory.getLogger(CommonErrorUtil::class.java)

  fun getTranslatedErrorMessage(
      messageSource: MessageSource,
      messageKey: String,
      locale: Locale,
      rootCause: Exception
  ): TranslatedErrorMessage {
    val errorMessageEnglish = getMessage(messageSource, messageKey, Locale.US, rootCause)
    val errorMessageTranslated = getMessage(messageSource, messageKey, locale, rootCause)
    return TranslatedErrorMessage(errorMessageEnglish, errorMessageTranslated)
  }

  private fun getMessage(
      messageSource: MessageSource,
      messageKey: String,
      locale: Locale,
      rootCause: Exception
  ): String {
    return try {
      messageSource.getMessage(messageKey, arrayOf(), locale)
    } catch (ex: NoSuchMessageException) {
      // Log message because it's not logged
      LOGGER.error("Message key not found for error message", ex)
      LOGGER.error("Root cause", rootCause)
      throw ex
    }
  }
}
