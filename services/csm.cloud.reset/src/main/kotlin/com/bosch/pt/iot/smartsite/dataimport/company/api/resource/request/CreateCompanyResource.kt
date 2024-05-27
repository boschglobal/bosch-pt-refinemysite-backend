/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.api.resource.request

class CreateCompanyResource(
    val name: String? = null,
    val streetAddress: CreateStreetAddressResource? = null,
    val postBoxAddress: CreatePostBoxAddressResource? = null
)
