/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request

import jakarta.validation.constraints.NotNull

data class SetUserLockedResource(@field:NotNull var locked: Boolean)
