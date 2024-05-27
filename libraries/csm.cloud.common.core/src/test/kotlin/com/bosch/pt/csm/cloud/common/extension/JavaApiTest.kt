/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.extension

import com.bosch.pt.csm.cloud.common.extensions.firstKey
import com.bosch.pt.csm.cloud.common.extensions.firstValue
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toList
import com.bosch.pt.csm.cloud.common.extensions.toLocalDate
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JavaApiTest {

  @Test
  fun `verify Map#firstKey`() {
    val map = mapOf("firstKey" to "firstValue", "secondKey" to "secondValue")

    assertThat(map.firstKey()).isEqualTo("firstKey")
  }

  @Test
  fun `verify Map#firstValue`() {
    val map = mapOf("firstKey" to "firstValue", "secondKey" to "secondValue")

    assertThat(map.firstValue()).isEqualTo("firstValue")
  }

  @Test
  fun `verify String#toUUID`() {
    val uuid = UUID.randomUUID()
    val uuidString = uuid.toString()

    assertThat(uuidString.toUUID()).isEqualTo(uuid)
  }

  @Test
  fun `verify Long#toInstantByMillis`() {
    assertThat(0L.toInstantByMillis()).isEqualTo("1970-01-01T00:00:00Z")
    assertThat(1611740430852L.toInstantByMillis()).isEqualTo("2021-01-27T09:40:30.852Z")
  }

  @Test
  fun `verify Long#toLocalDateByMillis`() {
    assertThat(0L.toLocalDateByMillis()).isEqualTo("1970-01-01")

    //  27. January 2021 23:59:59 GMT+0
    assertThat(1611791999000L.toLocalDateByMillis()).isEqualTo("2021-01-27")

    // 28. January 2021 00:00:00 GMT+0
    assertThat(1611792000000L.toLocalDateByMillis()).isEqualTo("2021-01-28")

    // 27. January 2021 09:40:30.852 GMT+0
    assertThat(1611740430852L.toLocalDateByMillis()).isEqualTo("2021-01-27")
  }

  @Test
  fun `verify Long#toLocalDateTimeByMillis`() {
    assertThat(0L.toLocalDateTimeByMillis()).isEqualTo("1970-01-01T00:00:00")
    assertThat(1611791999000L.toLocalDateTimeByMillis()).isEqualTo("2021-01-27T23:59:59")
    assertThat(1611792000000L.toLocalDateTimeByMillis()).isEqualTo("2021-01-28T00:00:00")
    assertThat(1611740430852L.toLocalDateTimeByMillis()).isEqualTo("2021-01-27T09:40:30.852")
  }

  @Test
  fun `verify Date#toLocalDate`() {
    // changing the system time zone to UTC is required because Date#toLocalDate internally
    // uses ZoneId#systemDefault()
    doWithSystemDefaultTimeZoneUtc {
      assertThat(Date(0L).toLocalDate()).isEqualTo("1970-01-01")

      //  27. January 2021 23:59:59 GMT+0
      assertThat(Date(1611791999000L).toLocalDate()).isEqualTo("2021-01-27")

      // 28. January 2021 00:00:00 GMT+0
      assertThat(Date(1611792000000L).toLocalDate()).isEqualTo("2021-01-28")

      // 27. January 2021 09:40:30.852 GMT+0
      assertThat(Date(1611740430852L).toLocalDate()).isEqualTo("2021-01-27")
    }
  }

  @Test
  fun `verify LocalDate#toEpochMilli`() {
    assertThat(LocalDate.of(1970, 1, 1).toEpochMilli()).isEqualTo(0L)
    assertThat(LocalDate.of(2021, 1, 28).toEpochMilli()).isEqualTo(1611792000000L)
  }

  @Test
  fun `verify LocalDateTime#toDate`() {
    assertThat(LocalDateTime.of(1970, 1, 1, 0, 0, 0).toDate().toInstant())
        .isEqualTo("1970-01-01T00:00:00Z")
    assertThat(LocalDateTime.of(2021, 1, 27, 23, 59, 59).toDate().toInstant())
        .isEqualTo("2021-01-27T23:59:59Z")
    assertThat(LocalDateTime.of(2021, 1, 28, 0, 0, 0).toDate().toInstant())
        .isEqualTo("2021-01-28T00:00:00Z")
    assertThat(LocalDateTime.of(2021, 1, 27, 9, 40, 30, 852_000_000).toDate().toInstant())
        .isEqualTo("2021-01-27T09:40:30.852Z")
  }

  @Test
  fun `verify Instant#toDate`() {
    assertThat(Instant.ofEpochMilli(0L).toDate().toInstant()).isEqualTo("1970-01-01T00:00:00Z")
    assertThat(Instant.ofEpochMilli(1611791999000L).toDate().toInstant())
        .isEqualTo("2021-01-27T23:59:59Z")
    assertThat(Instant.ofEpochMilli(1611792000000L).toDate().toInstant())
        .isEqualTo("2021-01-28T00:00:00Z")
    assertThat(Instant.ofEpochMilli(1611740430852L).toDate().toInstant())
        .isEqualTo("2021-01-27T09:40:30.852Z")
  }

  @Test
  fun `verify T#toList`() {
    assertThat(0L.toList()).isEqualTo(listOf(0L))
  }

  private fun doWithSystemDefaultTimeZoneUtc(procedure: () -> Unit) {
    val oldTimeZone = TimeZone.getDefault()
    val newTimeZone = TimeZone.getTimeZone("UTC")
    TimeZone.setDefault(newTimeZone)
    procedure.invoke()
    TimeZone.setDefault(oldTimeZone)
  }
}
