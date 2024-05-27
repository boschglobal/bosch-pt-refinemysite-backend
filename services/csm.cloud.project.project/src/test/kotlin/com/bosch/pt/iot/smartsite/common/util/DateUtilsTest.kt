/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.util

import com.bosch.pt.iot.smartsite.common.util.DateUtils.toDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verifying DateUtils properly converts ")
class DateUtilsTest {

  @Test
  @DisplayName("a LocalDateTime to a Date object")
  fun verifyConvert() {
    val now = Instant.now()
    val date = toDate(LocalDateTime.ofInstant(now, UTC))

    assertThat(now.toEpochMilli() == date!!.toInstant().toEpochMilli()).isTrue
  }

  @Test
  @DisplayName("a Null LocalDateTime to a Null object")
  fun verifyConvertWithNullValue() {
    val date = toDate(null)
    assertThat(date).isNull()
  }
}
