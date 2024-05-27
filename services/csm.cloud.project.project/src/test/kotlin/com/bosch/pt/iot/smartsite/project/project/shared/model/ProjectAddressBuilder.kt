/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.shared.model

class ProjectAddressBuilder private constructor() {

  private var city: String? = null
  private var houseNumber: String? = null
  private var street: String? = null
  private var zipCode: String? = null

  fun withCity(city: String?): ProjectAddressBuilder {
    this.city = city
    return this
  }

  fun withHouseNumber(houseNumber: String?): ProjectAddressBuilder {
    this.houseNumber = houseNumber
    return this
  }

  fun withStreet(street: String?): ProjectAddressBuilder {
    this.street = street
    return this
  }

  fun withZipCode(zipCode: String?): ProjectAddressBuilder {
    this.zipCode = zipCode
    return this
  }

  fun build(): ProjectAddress {
    val projectAddress = ProjectAddress()
    projectAddress.city = city!!
    projectAddress.houseNumber = houseNumber!!
    projectAddress.street = street!!
    projectAddress.zipCode = zipCode!!
    return projectAddress
  }

  companion object {

    fun projectAddress(): ProjectAddressBuilder =
        ProjectAddressBuilder()
            .withCity("City")
            .withHouseNumber("HN")
            .withStreet("Street")
            .withZipCode("ZC")
  }
}
