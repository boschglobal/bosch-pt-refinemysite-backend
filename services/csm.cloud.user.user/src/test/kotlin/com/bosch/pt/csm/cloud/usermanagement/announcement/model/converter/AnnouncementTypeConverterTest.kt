/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.model.converter

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum.SUCCESS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnnouncementTypeConverterTest {

  private val cut = AnnouncementTypeConverter()

  @DisplayName("When converting type to database column value")
  @Nested
  inner class ConvertToDatabaseColumn {

    @Test
    fun `given value is null`() {
      assertThat(cut.convertToDatabaseColumn(null)).isNull()
    }

    @Test
    fun `given value has been set`() {
      assertThat(cut.convertToDatabaseColumn(SUCCESS)).isEqualTo(30)
    }
  }

  @DisplayName("When converting from database column value to type")
  @Nested
  inner class ConvertFromDatabaseColumn {

    @Test
    fun `given database value is null`() {
      assertThrows(NullPointerException::class.java) { cut.convertToEntityAttribute(null) }
    }

    @Test
    fun `given database value has been set`() {
      assertThat(cut.convertToEntityAttribute(30)).isEqualTo(SUCCESS)
    }
  }
}
