/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource

import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto

class PostBoxAddressDtoBuilder private constructor() {

  private var city = "Leinfelden-Echterdingen"
  private var zipCode = "70745"
  private var area = "Baden Württemberg"
  private var country = "Germany"
  private var postBox = "10 01 56"

  fun withCity(city: String): PostBoxAddressDtoBuilder {
    this.city = city
    return this
  }

  fun withZipCode(zipCode: String): PostBoxAddressDtoBuilder {
    this.zipCode = zipCode
    return this
  }

  fun withArea(area: String): PostBoxAddressDtoBuilder {
    this.area = area
    return this
  }

  fun withCountry(country: String): PostBoxAddressDtoBuilder {
    this.country = country
    return this
  }

  fun withPostBox(postBox: String): PostBoxAddressDtoBuilder {
    this.postBox = postBox
    return this
  }

  fun build(): PostBoxAddressDto = PostBoxAddressDto(city, zipCode, area, country, postBox)

  companion object {
    fun postBoxAddress(): PostBoxAddressDtoBuilder = PostBoxAddressDtoBuilder()
  }
}
