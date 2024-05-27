/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.model

/** Builder for [Translation]s. */
class TranslationBuilder private constructor() {

  private var locale: String? = null
  private var value: String? = null

  /**
   * Set the locale of the translation.
   *
   * @param locale the locale of the translation
   * @return the builder
   */
  fun withLocale(locale: String?): TranslationBuilder {
    this.locale = locale
    return this
  }

  /**
   * Sets the actual translation.
   *
   * @param value the translation
   * @return the builder
   */
  fun withValue(value: String?): TranslationBuilder {
    this.value = value
    return this
  }

  /**
   * Build the translation.
   *
   * @return new translation
   */
  fun build(): Translation {
    val translation = Translation()
    translation.locale = locale
    translation.value = value
    return translation
  }

  companion object {

    /**
     * Initialise new translation builder with default data.
     *
     * @return craft builder
     */
    fun translation(): TranslationBuilder =
        TranslationBuilder().withLocale("de").withValue("Elektrizität")
  }
}
