/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model

import java.io.Serializable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.Size

@Embeddable
class ProjectAddress : Serializable {

  /** The city. */
  @field:Size(min = 1, max = MAX_CITY_LENGTH)
  @Column(nullable = false, length = MAX_CITY_LENGTH)
  lateinit var city: String

  /** The house number. */
  @field:Size(min = 1, max = MAX_HOUSENUMBER_LENGTH)
  @Column(nullable = false, length = MAX_HOUSENUMBER_LENGTH)
  lateinit var houseNumber: String

  /** The street. */
  @field:Size(min = 1, max = MAX_STREET_LENGTH)
  @Column(nullable = false, length = MAX_STREET_LENGTH)
  lateinit var street: String

  /** The zip code. */
  @field:Size(min = 1, max = MAX_ZIPCODE_LENGTH)
  @Column(nullable = false, length = MAX_ZIPCODE_LENGTH)
  lateinit var zipCode: String

  fun getDisplayName(): String = "$street, $houseNumber, $zipCode, $city"

  companion object {
    private const val serialVersionUID: Long = 8955587101494085086

    const val MAX_CITY_LENGTH = 100
    const val MAX_HOUSENUMBER_LENGTH = 10
    const val MAX_STREET_LENGTH = 100
    const val MAX_ZIPCODE_LENGTH = 10
  }
}
