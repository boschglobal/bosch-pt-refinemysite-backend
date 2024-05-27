/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization

import java.util.UUID

/** @see AuthorizationDelegation */
class AuthorizationDelegationDto(
    /** identifier of the entity to be authorized */
    val sourceIdentifier: UUID,
    /** identifier of the entity the authorization is delegated to */
    val targetIdentifier: UUID
)
