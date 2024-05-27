/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

class StreetAddressBuilder private constructor() {

  private var city: String? = null
  private var zipCode: String? = null
  private var area: String? = null
  private var country: String? = null
  private var street: String? = null
  private var houseNumber: String? = null

  fun withCity(city: String?): StreetAddressBuilder = apply { this.city = city }

  fun withZipCode(zipCode: String?): StreetAddressBuilder = apply { this.zipCode = zipCode }

  fun withArea(area: String?): StreetAddressBuilder = apply { this.area = area }

  fun withCountry(country: String?): StreetAddressBuilder = apply { this.country = country }

  fun withStreet(street: String?): StreetAddressBuilder = apply { this.street = street }

  fun withHouseNumber(houseNumber: String?): StreetAddressBuilder = apply {
    this.houseNumber = houseNumber
  }

  fun build(): StreetAddress = StreetAddress(city, zipCode, area, country, street, houseNumber)

  companion object {

    @JvmStatic fun streetAddress(): StreetAddressBuilder = StreetAddressBuilder()
  }
}
