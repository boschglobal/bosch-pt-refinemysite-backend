/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response.dto

import com.bosch.pt.csm.user.user.facade.resource.UserReference
import com.bosch.pt.csm.user.user.model.GenderEnum
import java.util.Date
import java.util.UUID

class UserDto(
    identifier: UUID,
    displayName: String?,
    email: String?,
    val gender: GenderEnum,
    val createdAt: Date,
    val admin: Boolean,
    val locked: Boolean
) : UserReference(identifier, displayName.orEmpty(), email)
