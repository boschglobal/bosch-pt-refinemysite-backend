/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.request

import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MAX_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.MIN_PAT_VALIDITY_IN_MINUTES
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

class CreateOrUpdatePatResource(
    val description: String,
    @get:Size(min = 1, message = "At least one scope is required for a valid PAT")
    val scopes: List<PatScopeEnum>,
    @get:Min(
        value = MIN_PAT_VALIDITY_IN_MINUTES, message = "PAT must be at least " +
          "$MIN_PAT_VALIDITY_IN_MINUTES minute(s) valid")
    @get:Max(
        value = MAX_PAT_VALIDITY_IN_MINUTES,
        message = "PAT must be at most 365 days valid ($MAX_PAT_VALIDITY_IN_MINUTES minutes)")
    val validForMinutes: Long
)
