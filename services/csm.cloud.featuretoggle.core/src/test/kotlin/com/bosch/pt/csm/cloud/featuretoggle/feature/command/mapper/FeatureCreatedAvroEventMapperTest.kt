/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.command.mapper

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class FeatureCreatedAvroEventMapperTest {
  private val mapper = FeatureCreatedAvroEventMapper()

  @Test
  fun `mapToKey throws IllegalArgumentException for unsupported event type`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { mapper.mapToKey(UnsupportedEventType(), 1) }
        .withMessage("Failed requirement.")
  }

  @Test
  fun `mapToValue throws IllegalArgumentException for unsupported event type`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { mapper.mapToValue(UnsupportedEventType(), 1) }
        .withMessage("Failed requirement.")
  }

  private class UnsupportedEventType
}
