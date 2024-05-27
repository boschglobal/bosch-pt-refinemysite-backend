/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.model

import com.bosch.pt.iot.smartsite.user.model.PhoneNumber.Companion.PATTERN_COUNTRY_CODE
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber.Companion.PATTERN_NUMBER
import com.bosch.pt.iot.smartsite.user.model.PhoneNumberType.MOBILE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Unit test to verify correctness of regex for validation the phone number and country code. */
class PhoneNumberValidationTest {

  /** Verifies that phone validation regex validates a valid german code with a valid number. */
  @Test
  fun verifyWithGermanCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(MOBILE, "+49", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code with invalid characters and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCharsCountryCode() {
    val cut = PhoneNumber(MOBILE, "+abc", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number with
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidCharsPhoneNumber() {
    val cut = PhoneNumber(MOBILE, "+49", "7111abc")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code with invalid double zero prefix
   * and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithDoubleZeroPrefix() {
    val cut = PhoneNumber(MOBILE, "001234", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates an invalid country code with plus prefix and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(MOBILE, "+12345", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code with single zero prefix and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithSingleZeroPrefix() {
    val cut = PhoneNumber(MOBILE, "01234", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code without prefix and with a valid
   * number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithoutPrefix() {
    val cut = PhoneNumber(MOBILE, "1234", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /** Verifies that phone validation regex invalidates an empty country code with a valid number. */
  @Test
  fun verifyWithInvalidEmptyCountryCode() {
    val cut = PhoneNumber(MOBILE, "", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /** Verifies that phone validation regex invalidates a valid country code with an empty number. */
  @Test
  fun verifyWithInvalidEmptyPhoneNumber() {
    val cut = PhoneNumber(MOBILE, "+49", "")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code exceeding the maximum length
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidMaxLengthCountryCode() {
    val cut = PhoneNumber(MOBILE, "+1234", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number exceeding
   * the maximum length.
   */
  @Test
  fun verifyWithInvalidMaxLengthPhoneNumber() {
    val cut = PhoneNumber(MOBILE, "+49", "71112345671234567890123456")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code without the minimum length and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidMinLengthCountryCode() {
    val cut = PhoneNumber(MOBILE, "+", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number without the
   * minimum length.
   */
  @Test
  fun verifyWithInvalidMinLengthPhoneNumber() {
    val cut = PhoneNumber(MOBILE, "+49", "7111")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number with
   * invalid zero prefix.
   */
  @Test
  fun verifyWithInvalidZeroPrefixPhoneNumber() {
    val cut = PhoneNumber(MOBILE, "+49", "071112345671234")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex validates a portuguese country code with a valid number.
   */
  @Test
  fun verifyWithPortugalCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(MOBILE, "+351", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex validates United States country code with a valid number.
   */
  @Test
  fun verifyWithUnitedStatesCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(MOBILE, "+1", "7111234567")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex validates a valid country code with zeros and with a valid
   * number.
   */
  @Test
  fun verifyWithValidCountryCodeWithZeros() {
    val cut = PhoneNumber(MOBILE, "+502", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number starting
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidPhoneNumberStartingInvalidChars() {
    val cut = PhoneNumber(MOBILE, "+502", "\n\t91734978")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number ending with
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidPhoneNumberEndingInvalidChars() {
    val cut = PhoneNumber(MOBILE, "+502", "91734978\n")
    assertThat(cut.callNumber).doesNotMatch(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code starting with invalid
   * characters and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeStartingInvalidChars() {
    val cut = PhoneNumber(MOBILE, "\n+502", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates a country code ending with invalid characters
   * and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeEndingInvalidChars() {
    val cut = PhoneNumber(MOBILE, "+502\n", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex validates an american country code with a valid number.
   */
  @Test
  fun verifyWithValidAmericanCountryCode() {
    val cut = PhoneNumber(MOBILE, "+1803", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).matches(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates an american invalid country code with a valid
   * number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCode() {
    val cut = PhoneNumber(MOBILE, "+3 803", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates an american invalid country code containing
   * zeros and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithZeros() {
    val cut = PhoneNumber(MOBILE, "+3 003", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates an american country code exceeding the maximum
   * length and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithMaxLength() {
    val cut = PhoneNumber(MOBILE, "+3 8031", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }

  /**
   * Verifies that phone validation regex invalidates an american country code without the minimum
   * length and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithMinLength() {
    val cut = PhoneNumber(MOBILE, "+3 ", "91734978")
    assertThat(cut.callNumber).matches(PATTERN_NUMBER)
    assertThat(cut.countryCode).doesNotMatch(PATTERN_COUNTRY_CODE)
  }
}
