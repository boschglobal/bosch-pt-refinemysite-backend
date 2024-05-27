/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.command.api

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.company.company.PostBoxAddressVo
import com.bosch.pt.csm.company.company.StreetAddressVo

object ValueObjectTestData {

  fun postBoxAddress() =
      PostBoxAddressVo(
          city = "Leinfelden-Echterdingen",
          zipCode = "70745",
          area = "Baden Württemberg",
          country = IsoCountryCodeEnum.DE.alternativeCountryName,
          postBox = "10 01 56")

  fun streetAddress() =
      StreetAddressVo(
          city = "Leinfelden-Echterdingen",
          zipCode = "70745",
          area = "Baden Württemberg",
          country = IsoCountryCodeEnum.DE.alternativeCountryName,
          street = "Max-Lang-Straße",
          houseNumber = "40-46")
}
