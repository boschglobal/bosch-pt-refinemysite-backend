/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val WORK_AREA_PROJECTION = "WorkAreaProjection"

@Document(WORK_AREA_PROJECTION)
@TypeAlias(WORK_AREA_PROJECTION)
data class WorkArea(
    @Id val identifier: WorkAreaId,
    val project: ProjectId,
    val version: Long,
    val name: String,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<WorkAreaVersion>
)

data class WorkAreaVersion(
    val version: Long,
    val name: String,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
