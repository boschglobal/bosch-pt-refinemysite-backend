/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.util

import com.bosch.pt.csm.cloud.common.i18n.LocalDateFormatter
import java.time.LocalDate
import org.springframework.context.i18n.LocaleContextHolder

fun LocalDate.formatForCurrentLocale(): String =
    format(LocalDateFormatter.forLocale(LocaleContextHolder.getLocale()))
