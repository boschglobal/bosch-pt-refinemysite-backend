/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import java.util.UUID

data class ParticipantPayloadV1(
    val id: UUID,
    val version: Long,
    val role: String,
    val status: String,
    val eventDate: LocalDateTime,

    // Additional attributes only for internal querying
    val userId: UserId,
    val companyId: CompanyId
)
