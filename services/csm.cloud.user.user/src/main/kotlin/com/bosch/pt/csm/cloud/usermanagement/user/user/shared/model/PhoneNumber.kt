/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.io.Serializable
import org.apache.commons.lang3.builder.HashCodeBuilder

@Embeddable
class PhoneNumber() : Serializable {

  @Column(nullable = false, length = 5)
  var countryCode: @NotNull @Pattern(regexp = PATTERN_COUNTRY_CODE) String? = null

  @Column(nullable = false, columnDefinition = "varchar(255)")
  @Enumerated(STRING)
  var phoneNumberType: @NotNull PhoneNumberType? = null

  @Column(nullable = false, length = 25)
  var callNumber: @NotNull @Pattern(regexp = PATTERN_NUMBER) String? = null

  /**
   * Constructor.
   *
   * @param phoneNumberType the type of the phone number
   * @param countryCode phone country code
   * @param callNumber call number
   */
  constructor(phoneNumberType: PhoneNumberType, countryCode: String, callNumber: String) : this() {
    this.phoneNumberType = phoneNumberType
    this.countryCode = countryCode
    this.callNumber = callNumber
  }

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is PhoneNumber) {
      return false
    }
    val that = other
    return (countryCode == that.countryCode &&
        phoneNumberType == that.phoneNumberType &&
        callNumber == that.callNumber)
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    return HashCodeBuilder(17, 31)
        .append(countryCode)
        .append(phoneNumberType)
        .append(callNumber)
        .toHashCode()
  }

  fun getDisplayName(): String = "$phoneNumberType: $countryCode$callNumber"

  companion object {

    /**
     * Valid country codes according to ITU-T recommendation E.164 (International public
     * telecommunication numbering plan). Valid country codes are for example: '+49', '+351' or
     * '+1'. See [E.164](https://en.wikipedia.org/wiki/E.164) and
     * [List of country codes](https://en.wikipedia.org/wiki/List_of_country_calling_codes)
     */
    const val PATTERN_COUNTRY_CODE = "^\\+\\d{1,4}$"

    /** validation for phone number including area code. */
    const val PATTERN_NUMBER = "^[1-9][0-9]{4,24}$"

    private const val serialVersionUID = -42L
  }
}
