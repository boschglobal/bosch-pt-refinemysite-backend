/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@Embeddable
class StreetAddress : AbstractAddress {

  @field:NotNull
  @field:Size(min = 1, max = MAX_STREET_LENGTH)
  @Column(length = MAX_STREET_LENGTH)
  var street: String? = null

  @field:NotNull
  @field:Size(min = 1, max = MAX_HOUSENUMBER_LENGTH)
  @Column(length = MAX_HOUSENUMBER_LENGTH)
  var houseNumber: String? = null

  constructor()

  constructor(
      city: String?,
      zipCode: String?,
      area: String?,
      country: String?,
      street: String?,
      houseNumber: String?
  ) : super(city, zipCode, area, country) {
    this.street = street
    this.houseNumber = houseNumber
  }

  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("street", street)
          .append("houseNumber", houseNumber)
          .toString()

  companion object {
    private const val serialVersionUID: Long = 1398058672054730876

    const val MAX_STREET_LENGTH = 100
    const val MAX_HOUSENUMBER_LENGTH = 10
  }
}
