/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId

data class ParticipantResource(
    val id: ParticipantId,
    val version: Long,
    val project: ProjectId,
    val company: CompanyId,
    val user: UserId?,
    val role: String,
    val status: String,
    val eventTimestamp: Long
)
