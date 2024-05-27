/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.model

class PostBoxAddressBuilder private constructor() {

  private var city: String? = null
  private var zipCode: String? = null
  private var area: String? = null
  private var country: String? = null
  private var postBox: String? = null

  fun withCity(city: String?): PostBoxAddressBuilder = apply { this.city = city }

  fun withZipCode(zipCode: String?): PostBoxAddressBuilder = apply { this.zipCode = zipCode }

  fun withArea(area: String?): PostBoxAddressBuilder = apply { this.area = area }

  fun withCountry(country: String?): PostBoxAddressBuilder = apply { this.country = country }

  fun withPostBox(postBox: String?): PostBoxAddressBuilder = apply { this.postBox = postBox }

  fun build(): PostBoxAddress = PostBoxAddress(city, zipCode, area, country, postBox)

  companion object {

    @JvmStatic fun postBoxAddress(): PostBoxAddressBuilder = PostBoxAddressBuilder()
  }
}
