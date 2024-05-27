/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.message

import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.model.PostBoxAddress
import com.bosch.pt.csm.cloud.projectmanagement.company.model.StreetAddress

fun CompanyAggregateAvro.toEntity() =
    Company(
        identifier = buildAggregateIdentifier(),
        companyIdentifier = getIdentifier(),
        name = getName(),
        streetAddress = getStreetAddress()?.toEntity(),
        postBoxAddress = getPostBoxAddress()?.toEntity())

private fun StreetAddressAvro.toEntity() =
    StreetAddress(
        area = getArea(),
        city = getCity(),
        country = getCountry(),
        houseNumber = getHouseNumber(),
        street = getStreet(),
        zipCode = getZipCode())

private fun PostBoxAddressAvro.toEntity() =
    PostBoxAddress(
        area = getArea(),
        city = getCity(),
        country = getCountry(),
        postBox = getPostBox(),
        zipCode = getZipCode())
