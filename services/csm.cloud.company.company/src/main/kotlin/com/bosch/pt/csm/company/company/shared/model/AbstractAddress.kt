/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@MappedSuperclass
abstract class AbstractAddress : Serializable {

  // City
  @field:Size(min = 1, max = MAX_CITY_LENGTH)
  @Column(nullable = false, length = MAX_CITY_LENGTH)
  lateinit var city: String

  // Zip Code
  @field:Size(min = 1, max = MAX_ZIPCODE_LENGTH)
  @Column(nullable = false, length = MAX_ZIPCODE_LENGTH)
  lateinit var zipCode: String

  // Area
  @field:Size(max = MAX_AREA_LENGTH) @Column(length = MAX_AREA_LENGTH) var area: String? = null

  // Country
  @field:Size(min = 1, max = MAX_COUNTRY_LENGTH)
  @Column(length = MAX_COUNTRY_LENGTH)
  lateinit var country: String

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("city", city)
          .append("zipCode", zipCode)
          .append("area", area)
          .append("country", country)
          .toString()

  companion object {
    private const val serialVersionUID: Long = -1771166100068372211L

    const val MAX_CITY_LENGTH = 100
    const val MAX_ZIPCODE_LENGTH = 10
    const val MAX_AREA_LENGTH = 100
    const val MAX_COUNTRY_LENGTH = 100
  }
}
