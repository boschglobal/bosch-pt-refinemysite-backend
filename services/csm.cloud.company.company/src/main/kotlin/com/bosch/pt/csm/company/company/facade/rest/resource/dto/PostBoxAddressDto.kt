/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.dto

import com.bosch.pt.csm.company.company.PostBoxAddressVo
import com.bosch.pt.csm.company.company.shared.model.PostBoxAddress
import com.bosch.pt.csm.company.company.shared.model.PostBoxAddress.Companion.MAX_POSTBOX_LENGTH
import jakarta.validation.constraints.Size

class PostBoxAddressDto(
    city: String,
    zipCode: String,
    area: String?,
    country: String,
    @field:Size(min = 1, max = MAX_POSTBOX_LENGTH) var postBox: String
) : AbstractAddressDto(city, zipCode, area, country) {

  constructor(
      postBoxAddress: PostBoxAddress
  ) : this(
      postBoxAddress.city,
      postBoxAddress.zipCode,
      postBoxAddress.area,
      postBoxAddress.country,
      postBoxAddress.postBox)

  fun toPostBoxAdressVo() =
      PostBoxAddressVo(
          city = this.city,
          zipCode = this.zipCode,
          area = this.area,
          country = this.country,
          postBox = this.postBox)
}
