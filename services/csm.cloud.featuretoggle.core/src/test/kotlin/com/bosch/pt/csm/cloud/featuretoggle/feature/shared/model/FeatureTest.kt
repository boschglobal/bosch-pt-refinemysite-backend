/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.shared.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class FeatureTest {

  @Test
  fun `has appropriate display name`() {
    val entity = Feature()
    entity.name = "featureName"

    assertThat(entity.getDisplayName()).isEqualTo("featureName")
    assertThat(entity.name).isEqualTo("featureName")
  }

  @Test
  fun `has error when mandatory properties are not set`() {
    val entity = Feature()
    assertThatExceptionOfType(UninitializedPropertyAccessException::class.java).isThrownBy {
      entity.name
    }
  }
}
