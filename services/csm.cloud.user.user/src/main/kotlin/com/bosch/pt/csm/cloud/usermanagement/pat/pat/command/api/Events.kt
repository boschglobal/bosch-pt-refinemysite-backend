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
import java.time.LocalDateTime

data class PatCreatedEvent(
    override val patId: PatId,
    val impersonatedUser: UserId,
    val scopes: List<PatScopeEnum>,
    val type: PatTypeEnum,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val description: String,
    val hash: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime,
) : PatEvent, AuditedEvent

data class PatUpdatedEvent(
    override val patId: PatId,
    val impersonatedUser: UserId,
    val scopes: List<PatScopeEnum>,
    val expiresAt: LocalDateTime,
    val description: String,
    override val userIdentifier: UserId,
    override val timestamp: LocalDateTime,
) : PatEvent, AuditedEvent
