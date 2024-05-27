/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.shared.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.builder.ToStringBuilder

@Embeddable
class StreetAddress : AbstractAddress() {

  // Street
  @field:Size(min = 1, max = MAX_STREET_LENGTH)
  @Column(length = MAX_STREET_LENGTH)
  lateinit var street: String

  // House number
  @field:Size(min = 1, max = MAX_HOUSENUMBER_LENGTH)
  @Column(length = MAX_HOUSENUMBER_LENGTH)
  lateinit var houseNumber: String

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("street", street)
          .append("houseNumber", houseNumber)
          .toString()

  companion object {
    const val MAX_STREET_LENGTH = 100
    const val MAX_HOUSENUMBER_LENGTH = 10
  }
}
