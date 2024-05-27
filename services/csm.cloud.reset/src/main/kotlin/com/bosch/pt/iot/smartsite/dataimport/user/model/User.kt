/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.model

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.GenderEnum
import java.util.Locale
import java.util.UUID

class User(
    val identifier: UUID? = null,
    override val id: String,
    val version: Long? = null,
    val email: String,
    val password: String,
    val gender: GenderEnum? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val position: String? = null,
    val roles: Set<String>? = null,
    val phoneNumbers: Set<PhoneNumber>? = null,
    val craftIds: Set<String>? = null,
    val locale: Locale? = null,
    val country: IsoCountryCodeEnum? = null
) : ImportObject
