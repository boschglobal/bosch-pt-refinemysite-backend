/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model.converter

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import java.util.UUID.randomUUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.jupiter.api.Test

class UuidConverterTest {

  private val cut: UuidConverter = UuidConverter()

  @Test
  fun testConvertToDatabaseColumn() {
    val uuid = randomUUID()
    assertEquals(uuid.toString(), cut.convertToDatabaseColumn(uuid))
    assertNull(cut.convertToDatabaseColumn(null))
  }

  @Test
  fun testConvertToEntityAttribute() {
    val uuidString = "83b1f193-b8cf-47ef-8e45-5e95d2f92f80"
    assertEquals(uuidString.toUUID(), cut.convertToEntityAttribute(uuidString))
    assertNull(cut.convertToEntityAttribute(null))
  }
}
