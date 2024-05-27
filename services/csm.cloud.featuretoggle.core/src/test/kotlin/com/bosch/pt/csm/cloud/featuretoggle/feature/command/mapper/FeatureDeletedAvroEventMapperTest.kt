/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class FeatureDeletedAvroEventMapperTest {
  private val mapper = FeatureDeletedEventAvroMapper()

  @Test
  fun `mapToKey throws IllegalArgumentException for unsupported event type`() {
    Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { mapper.mapToKey(UnsupportedEventType(), 1) }
        .withMessage("Failed requirement.")
  }

  @Test
  fun `mapToValue throws IllegalArgumentException for unsupported event type`() {
    Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { mapper.mapToValue(UnsupportedEventType(), 1) }
        .withMessage("Failed requirement.")
  }

  private class UnsupportedEventType
}
