/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.i18n

import java.time.format.DateTimeFormatter
import java.util.Locale

object LocalDateFormatter {
  fun forLocale(locale: Locale): DateTimeFormatter {
    return when (locale.language) {
      "de" -> DateTimeFormatter.ofPattern("dd.MM.yyyy")
      "en" -> DateTimeFormatter.ofPattern("MM/dd/yyyy")
      "es" -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
      "fr" -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
      "pt" -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
      else -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
  }
}
