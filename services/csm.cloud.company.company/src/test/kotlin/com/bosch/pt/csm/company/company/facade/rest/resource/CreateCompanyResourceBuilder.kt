/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource

import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.request.SaveCompanyResource

/** Builder for a [SaveCompanyResource]. */
class CreateCompanyResourceBuilder
/** Private constructor. */
private constructor() {

  private var companyName: String? = null
  private var streetAddress: StreetAddressDto? = null
  private var postBoxAddress: PostBoxAddressDto? = null

  /**
   * Sets name.
   *
   * @param name the name
   * @return the builder
   */
  fun withName(name: String): CreateCompanyResourceBuilder {
    this.companyName = name
    return this
  }

  /**
   * Sets street address.
   *
   * @param streetAddress the street address
   * @return the builder
   */
  fun withStreetAddress(streetAddress: StreetAddressDto?): CreateCompanyResourceBuilder {
    this.streetAddress = streetAddress
    return this
  }

  /**
   * Sets post box address.
   *
   * @param postBoxAddress the post box address
   * @return the builder
   */
  fun withPostBoxAddress(postBoxAddress: PostBoxAddressDto?): CreateCompanyResourceBuilder {
    this.postBoxAddress = postBoxAddress
    return this
  }

  /**
   * Builds the [StreetAddress] object.
   *
   * @return the StreetAddress
   */
  fun build(): SaveCompanyResource =
      SaveCompanyResource(companyName!!, streetAddress, postBoxAddress)

  companion object {

    /**
     * Create builder object.
     *
     * @return builder object
     */
    fun createCompanyResource(): CreateCompanyResourceBuilder = CreateCompanyResourceBuilder()
  }
}
