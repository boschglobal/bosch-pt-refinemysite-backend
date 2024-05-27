/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import java.util.regex.Pattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Unit test to verify correctness of regex for validation the phone number and country code. */
class PhoneNumberValidationTest {

  /** Verifies that phone validation regex validates a valid german code with a valid number. */
  @Test
  fun verifyWithGermanCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a country code with invalid characters and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCharsCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+abc", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number with
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidCharsPhoneNumber() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "7111abc")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a country code with invalid double zero prefix
   * and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithDoubleZeroPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "001234", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates an invalid country code with plus prefix and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+12345", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates a country code with single zero prefix and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithSingleZeroPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "01234", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates a country code without prefix and with a valid
   * number.
   */
  @Test
  fun verifyWithInvalidCountryCodeWithoutPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "1234", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /** Verifies that phone validation regex invalidates an empty country code with a valid number. */
  @Test
  fun verifyWithInvalidEmptyCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /** Verifies that phone validation regex invalidates a valid country code with an empty number. */
  @Test
  fun verifyWithInvalidEmptyPhoneNumber() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a country code exceeding the maximum length
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidMaxLengthCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+1234", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number exceeding
   * the maximum length.
   */
  @Test
  fun verifyWithInvalidMaxLengthPhoneNumber() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "71112345671234567890123456")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a country code without the minimum length and
   * with a valid number.
   */
  @Test
  fun verifyWithInvalidMinLengthCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number without the
   * minimum length.
   */
  @Test
  fun verifyWithInvalidMinLengthPhoneNumber() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "7111")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number with
   * invalid zero prefix.
   */
  @Test
  fun verifyWithInvalidZeroPrefixPhoneNumber() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+49", "071112345671234")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex validates a portuguese country code with a valid number.
   */
  @Test
  fun verifyWithPortugalCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+351", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex validates United States country code with a valid number.
   */
  @Test
  fun verifyWithUnitedStatesCountryCodeWithPlusPrefix() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+1", "7111234567")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex validates a valid country code with zeros and with a valid
   * number.
   */
  @Test
  fun verifyWithValidCountryCodeWithZeros() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+502", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number starting
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidPhoneNumberStartingInvalidChars() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+502", "\n\t91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a valid country code with a number ending with
   * invalid characters.
   */
  @Test
  fun verifyWithInvalidPhoneNumberEndingInvalidChars() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+502", "91734978\n")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isFalse
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates a country code starting with invalid
   * characters and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeStartingInvalidChars() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "\n+502", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates a country code ending with invalid characters
   * and with a valid number.
   */
  @Test
  fun verifyWithInvalidCountryCodeEndingInvalidChars() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+502\n", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex validates an american country code with a valid number.
   */
  @Test
  fun verifyWithValidAmericanCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+1803", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isTrue
  }

  /**
   * Verifies that phone validation regex invalidates an american invalid country code with a valid
   * number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCode() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+3 803", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates an american invalid country code containing
   * zeros and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithZeros() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+3 003", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates an american country code exceeding the maximum
   * length and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithMaxLength() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+3 8031", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  /**
   * Verifies that phone validation regex invalidates an american country code without the minimum
   * length and with a valid number.
   */
  @Test
  fun verifyWithInvalidAmericanCountryCodeWithMinLength() {
    val cut = PhoneNumber(PhoneNumberType.MOBILE, "+3 ", "91734978")
    assertThat(checkValueForRegex(cut.callNumber, PhoneNumber.PATTERN_NUMBER)).isTrue
    assertThat(checkValueForRegex(cut.countryCode, PhoneNumber.PATTERN_COUNTRY_CODE)).isFalse
  }

  private fun checkValueForRegex(value: String?, regex: String): Boolean {
    val pattern = Pattern.compile(regex)
    return pattern.matcher(value!!).matches()
  }
}
