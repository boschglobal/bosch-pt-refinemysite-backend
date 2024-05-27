/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response

import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress.Companion.MAX_CITY_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress.Companion.MAX_HOUSENUMBER_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress.Companion.MAX_STREET_LENGTH
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectAddress.Companion.MAX_ZIPCODE_LENGTH
import jakarta.validation.constraints.Size

/** ProjectAddressResource for a [ProjectAddress]. */
class ProjectAddressDto(

    // City
    @field:Size(min = 1, max = MAX_CITY_LENGTH) val city: String,

    // House number
    @field:Size(min = 1, max = MAX_HOUSENUMBER_LENGTH) val houseNumber: String,

    // Street
    @field:Size(min = 1, max = MAX_STREET_LENGTH) val street: String,

    // Zip Code
    @field:Size(min = 1, max = MAX_ZIPCODE_LENGTH) val zipCode: String
) {

  /**
   * Creates new [ProjectAddressDto] from the entity [ProjectAddress]
   *
   * @param projectAddress the project address.
   */
  constructor(
      projectAddress: ProjectAddress
  ) : this(
      requireNotNull(projectAddress.city),
      requireNotNull(projectAddress.houseNumber),
      requireNotNull(projectAddress.street),
      requireNotNull(projectAddress.zipCode))

  fun toValueObject() =
      ProjectAddressVo(street = street, houseNumber = houseNumber, zipCode = zipCode, city = city)
}
