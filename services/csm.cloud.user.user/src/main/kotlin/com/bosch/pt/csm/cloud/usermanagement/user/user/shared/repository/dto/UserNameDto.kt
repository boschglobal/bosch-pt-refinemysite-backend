/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.dto

import com.bosch.pt.csm.cloud.common.api.UserId

data class UserNameDto(val identifier: UserId, val firstName: String, val lastName: String)
