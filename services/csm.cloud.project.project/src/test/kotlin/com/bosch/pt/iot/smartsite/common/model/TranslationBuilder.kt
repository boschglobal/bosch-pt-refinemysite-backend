/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

class TranslationBuilder private constructor() {

  private var locale: String? = null
  private var value: String? = null

  fun withLocale(locale: String?): TranslationBuilder = apply { this.locale = locale }

  fun withValue(value: String?): TranslationBuilder = apply { this.value = value }

  fun build(): Translation {
    val translation = Translation()
    translation.locale = locale
    translation.value = value
    return translation
  }

  companion object {

    fun translation(): TranslationBuilder =
        TranslationBuilder().withLocale("de").withValue("Elektrizität")
  }
}
