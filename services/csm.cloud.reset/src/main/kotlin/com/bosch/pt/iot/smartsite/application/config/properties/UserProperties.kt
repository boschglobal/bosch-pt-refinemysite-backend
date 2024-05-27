/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config.properties

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import java.util.Locale

@Suppress("LongParameterList")
class UserProperties(
    val id: Long,
    val admin: Boolean = false,
    val announcement: Boolean = false,
    val identifier: String,
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val createdBy: String,
    val lastModifiedBy: String,
    val locale: Locale,
    val country: IsoCountryCodeEnum
)
