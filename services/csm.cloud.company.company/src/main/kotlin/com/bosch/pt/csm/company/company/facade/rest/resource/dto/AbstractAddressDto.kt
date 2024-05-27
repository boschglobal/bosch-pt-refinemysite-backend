/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.dto

import com.bosch.pt.csm.company.company.shared.model.AbstractAddress
import com.bosch.pt.csm.company.company.shared.model.AbstractAddress.Companion.MAX_AREA_LENGTH
import com.bosch.pt.csm.company.company.shared.model.AbstractAddress.Companion.MAX_CITY_LENGTH
import com.bosch.pt.csm.company.company.shared.model.AbstractAddress.Companion.MAX_COUNTRY_LENGTH
import com.bosch.pt.csm.company.company.shared.model.AbstractAddress.Companion.MAX_ZIPCODE_LENGTH
import jakarta.validation.constraints.Size

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractAddressDto(
    @field:Size(min = 1, max = MAX_CITY_LENGTH) var city: String,
    @field:Size(min = 1, max = MAX_ZIPCODE_LENGTH) var zipCode: String,
    @field:Size(max = MAX_AREA_LENGTH) var area: String?,
    @field:Size(min = 1, max = MAX_COUNTRY_LENGTH) var country: String
) {

  constructor(
      address: AbstractAddress
  ) : this(
      city = address.city,
      zipCode = address.zipCode,
      area = address.area,
      country = address.country)
}
