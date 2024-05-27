/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.model

open class Address(
    val city: String? = null,
    val zipCode: String? = null,
    val area: String? = null,
    val country: String? = null
)
