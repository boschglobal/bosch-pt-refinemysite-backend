/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.util

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Calendar
import java.util.Date

object DateUtils {

  @JvmStatic
  fun toDate(localDateTime: LocalDateTime?): Date? {
    return if (localDateTime == null) null else Date(localDateTime.toInstant(UTC).toEpochMilli())
  }

  @JvmStatic
  fun Date.getHour(): Long =
      Calendar.getInstance().apply { time = this@getHour }.get(Calendar.HOUR_OF_DAY).toLong()
}
