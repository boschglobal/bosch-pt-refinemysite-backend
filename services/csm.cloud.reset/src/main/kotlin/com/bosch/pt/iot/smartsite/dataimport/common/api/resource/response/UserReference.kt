/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response

import java.util.UUID

data class UserReference(
    val displayName: String? = null,
    val id: UUID? = null,
    val email: String? = null
)
