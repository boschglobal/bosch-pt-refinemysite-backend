/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.utils

import java.util.Locale

fun Locale.toLanguage() =
    when (this.toString()) {
      "en_GB" -> "English"
      "de_DE" -> "Deutsch"
      "es_ES" -> "Español"
      "pt_PT" -> "Português"
      "fr_FR" -> "Français"
      else -> error("Unsupported locale")
    }
