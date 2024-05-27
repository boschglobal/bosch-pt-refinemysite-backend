/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum

data class CreatePatCommand(
    val impersonatedUser: UserId,
    val description: String,
    val scopes: List<PatScopeEnum>,
    val validForMinutes: Long,
    val type: PatTypeEnum
)

data class PatCreatedCommandResult(
    val patId: PatId,
    val token: String,
)

data class UpdatePatCommand(
    val patId: PatId,
    val version: Long,
    val impersonatedUser: UserId,
    val description: String,
    val scopes: List<PatScopeEnum>,
    val validForMinutes: Long,
)

data class DeletePatCommand(
    val patId: PatId,
    val version: Long,
    val impersonatedUser: UserId,
)

const val MIN_PAT_VALIDITY_IN_MINUTES: Long = 1
const val MAX_PAT_VALIDITY_IN_MINUTES: Long = 365 * 24 * 60
