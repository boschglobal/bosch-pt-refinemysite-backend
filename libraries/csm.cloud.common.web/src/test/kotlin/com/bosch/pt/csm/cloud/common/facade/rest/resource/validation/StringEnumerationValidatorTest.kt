/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.validation

import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumerationValidatorTest.TestEnum.ONE
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** Test to verify expected behavior of [StringEnumerationValidator]. */
internal class StringEnumerationValidatorTest {

  private val cut = StringEnumerationValidator()

  /** Verifies that [StringEnumerationValidator.isValid] works as expected if value is null. */
  @Test
  fun verifyIsValidValueNull() {
    val valid = cut.isValid(null, null)
    Assertions.assertThat(valid).isTrue
  }

  /**
   * Verifies that [StringEnumerationValidator.isValid] works as expected if availableEnumNames set
   * contains the specified value.
   */
  @Test
  fun verifyIsValidAvailableEnumNamesContainsValue() {
    ReflectionTestUtils.setField(cut, "availableEnumNames", setOf(ONE.name))
    val valid = cut.isValid(ONE, null)
    Assertions.assertThat(valid).isTrue
  }

  /**
   * Verifies that [StringEnumerationValidator.isValid] works as expected if availableEnumNames set
   * contains not the specified value.
   */
  @Test
  fun verifyIsValidAvailableEnumNamesContainsNotValue() {
    ReflectionTestUtils.setField(cut, "availableEnumNames", setOf("A"))
    val valid = cut.isValid(ONE, null)
    Assertions.assertThat(valid).isFalse
  }

  enum class TestEnum {
    ONE,
    TWO
  }
}
