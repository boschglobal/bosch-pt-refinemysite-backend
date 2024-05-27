/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.validation

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumerationValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** Test to verify expected behavior of [StringEnumerationValidator]. */
class StringEnumerationValidatorTest {

  private val cut = StringEnumerationValidator()

  /** Verifies that [StringEnumerationValidator.isValid] works as expected if value is null. */
  @Test
  fun verifyIsValidValueNull() {
    val valid = cut.isValid(null, null)
    assertThat(valid).isTrue
  }

  /**
   * Verifies that [StringEnumerationValidator.isValid] works as expected if availableEnumNames set
   * contains the specified value.
   */
  @Test
  fun verifyIsValidAvailableEnumNamesContainsValue() {
    ReflectionTestUtils.setField(cut, "availableEnumNames", setOf(FULL.name))

    val valid = cut.isValid(FULL, null)
    assertThat(valid).isTrue
  }

  /**
   * Verifies that [StringEnumerationValidator.isValid] works as expected if availableEnumNames set
   * contains not the specified value.
   */
  @Test
  fun verifyIsValidAvailableEnumNamesContainsNotValue() {
    ReflectionTestUtils.setField(cut, "availableEnumNames", setOf("A"))

    val valid = cut.isValid(FULL, null)
    assertThat(valid).isFalse
  }
}
