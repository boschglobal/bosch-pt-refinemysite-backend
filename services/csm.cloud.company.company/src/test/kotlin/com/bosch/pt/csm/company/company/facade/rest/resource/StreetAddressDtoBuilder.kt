/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource

import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto

class StreetAddressDtoBuilder private constructor() {

  private var city = "Leinfelden-Echterdingen"
  private var zipCode = "70745"
  private var area = "Baden Württemberg"
  private var country = "Germany"
  private var street = "Max-Lang-Straße"
  private var houseNumber = "40-46"

  fun withCity(city: String): StreetAddressDtoBuilder {
    this.city = city
    return this
  }

  fun withZipCode(zipCode: String): StreetAddressDtoBuilder {
    this.zipCode = zipCode
    return this
  }

  fun withArea(area: String): StreetAddressDtoBuilder {
    this.area = area
    return this
  }

  fun withCountry(country: String): StreetAddressDtoBuilder {
    this.country = country
    return this
  }

  fun withStreet(street: String): StreetAddressDtoBuilder {
    this.street = street
    return this
  }

  fun withHouseNumber(houseNumber: String): StreetAddressDtoBuilder {
    this.houseNumber = houseNumber
    return this
  }

  fun build(): StreetAddressDto =
      StreetAddressDto(city, zipCode, area, country, street, houseNumber)

  companion object {
    fun streetAddress(): StreetAddressDtoBuilder = StreetAddressDtoBuilder()
  }
}
