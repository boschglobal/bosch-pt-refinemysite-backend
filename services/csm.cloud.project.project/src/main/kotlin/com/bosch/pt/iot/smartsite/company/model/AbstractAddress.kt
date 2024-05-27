/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@MappedSuperclass
abstract class AbstractAddress : Serializable {

  @field:NotNull
  @field:Size(min = 1, max = MAX_CITY_LENGTH)
  @Column(nullable = false, length = MAX_CITY_LENGTH)
  var city: String? = null

  @field:NotNull
  @field:Size(min = 1, max = MAX_ZIPCODE_LENGTH)
  @Column(nullable = false, length = MAX_ZIPCODE_LENGTH)
  var zipCode: String? = null

  @field:Size(max = MAX_AREA_LENGTH) @Column(length = MAX_AREA_LENGTH) var area: String? = null

  @field:NotNull
  @field:Size(min = 1, max = MAX_COUNTRY_LENGTH)
  @Column(length = MAX_COUNTRY_LENGTH)
  var country: String? = null

  constructor()

  protected constructor(city: String?, zipCode: String?, area: String?, country: String?) {
    this.city = city
    this.zipCode = zipCode
    this.area = area
    this.country = country
  }

  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("city", city)
          .append("zipCode", zipCode)
          .append("area", area)
          .append("country", country)
          .toString()

  companion object {
    private const val serialVersionUID: Long = -3688979747079812302

    const val MAX_CITY_LENGTH = 100
    const val MAX_ZIPCODE_LENGTH = 10
    const val MAX_AREA_LENGTH = 100
    const val MAX_COUNTRY_LENGTH = 100
  }
}
