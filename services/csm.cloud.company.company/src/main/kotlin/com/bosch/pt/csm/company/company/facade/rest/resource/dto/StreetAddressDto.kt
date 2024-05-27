/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.dto

import com.bosch.pt.csm.company.company.StreetAddressVo
import com.bosch.pt.csm.company.company.shared.model.StreetAddress
import com.bosch.pt.csm.company.company.shared.model.StreetAddress.Companion.MAX_HOUSENUMBER_LENGTH
import com.bosch.pt.csm.company.company.shared.model.StreetAddress.Companion.MAX_STREET_LENGTH
import jakarta.validation.constraints.Size

class StreetAddressDto(
    city: String,
    zipCode: String,
    area: String?,
    country: String,
    @field:Size(min = 1, max = MAX_STREET_LENGTH) var street: String,
    @field:Size(min = 1, max = MAX_HOUSENUMBER_LENGTH) var houseNumber: String
) : AbstractAddressDto(city, zipCode, area, country) {

  constructor(
      streetAddress: StreetAddress
  ) : this(
      streetAddress.city,
      streetAddress.zipCode,
      streetAddress.area,
      streetAddress.country,
      streetAddress.street,
      streetAddress.houseNumber)

  fun toStreetAddressVo() =
      StreetAddressVo(
          city = this.city,
          zipCode = this.zipCode,
          area = this.area,
          country = this.country,
          street = this.street,
          houseNumber = this.houseNumber)
}
