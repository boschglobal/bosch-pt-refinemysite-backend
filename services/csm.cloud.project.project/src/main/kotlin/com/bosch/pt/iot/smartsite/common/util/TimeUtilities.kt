/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

object TimeUtilities {

  fun asLocalDateTime(epochMillis: Long): LocalDateTime =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), UTC)

  fun asLocalDate(epochMillis: Long): LocalDate =
      LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis), UTC)
}
