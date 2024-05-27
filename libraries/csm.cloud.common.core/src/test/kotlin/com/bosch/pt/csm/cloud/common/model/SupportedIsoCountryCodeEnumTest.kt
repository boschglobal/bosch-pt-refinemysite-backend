/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SupportedIsoCountryCodeEnumTest {

  @Test
  fun `verify exists by country name`() {
    assertThat(IsoCountryCodeEnum.fromCountryNameExists("United States of America")).isTrue
  }

  @Test
  fun `verify exists by alternative country name`() {
    assertThat(IsoCountryCodeEnum.fromAlternativeCountryNameExists("United States of America (the)"))
        .isTrue
  }

  @Test
  fun `verify finding country name by country code (lower case)`() {
    assertThat(IsoCountryCodeEnum.fromCountryCode("pt")).isEqualTo(IsoCountryCodeEnum.PT)
  }

  @Test
  fun `verify finding country name by country code (upper case)`() {
    assertThat(IsoCountryCodeEnum.fromCountryCode("PT")).isEqualTo(IsoCountryCodeEnum.PT)
  }

  @Test
  fun `verify finding country name by country code (mixed case)`() {
    assertThat(IsoCountryCodeEnum.fromCountryCode("Pt")).isEqualTo(IsoCountryCodeEnum.PT)
  }

  @Test
  fun `verify finding country code by country name`() {
    assertThat(IsoCountryCodeEnum.fromCountryName("United States of America")).isEqualTo(IsoCountryCodeEnum.US)
  }

  @Test
  fun `verify finding country code by alternative country name`() {
    assertThat(IsoCountryCodeEnum.fromAlternativeCountryName("United States of America (the)"))
        .isEqualTo(IsoCountryCodeEnum.US)
  }

  @Test
  fun `verify finding all country names`() {
    assertThat(IsoCountryCodeEnum.values()).hasSize(249)
  }
}
