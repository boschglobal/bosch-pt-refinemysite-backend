/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import java.time.LocalDateTime
import java.util.UUID

data class TaskPayloadV1(
    val id: UUID,
    val version: Long,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val status: String,
    val critical: Boolean? = null,
    val editDate: LocalDateTime? = null,
    val eventDate: LocalDateTime,

    // Additional attributes only for internal querying
    val projectId: ProjectId,
    val craftId: ProjectCraftId,
    val assigneeId: ParticipantId? = null,
    val workAreaId: WorkAreaId? = null,
)
