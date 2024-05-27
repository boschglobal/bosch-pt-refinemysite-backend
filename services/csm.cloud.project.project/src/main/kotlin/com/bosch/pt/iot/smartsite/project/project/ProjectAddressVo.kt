/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project

data class ProjectAddressVo(
    val street: String,
    val houseNumber: String,
    val city: String,
    val zipCode: String
)