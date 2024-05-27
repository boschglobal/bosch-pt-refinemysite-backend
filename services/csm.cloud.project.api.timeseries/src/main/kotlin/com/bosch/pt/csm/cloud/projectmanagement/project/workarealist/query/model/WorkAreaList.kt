/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.domain.WorkAreaListId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val WORK_AREA_LIST_PROJECTION = "WorkAreaListProjection"

@Document(WORK_AREA_LIST_PROJECTION)
@TypeAlias(WORK_AREA_LIST_PROJECTION)
data class WorkAreaList(
    @Id val identifier: WorkAreaListId,
    val project: ProjectId,
    val version: Long,
    val workAreas: List<WorkAreaId>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<WorkAreaListVersion>
)

data class WorkAreaListVersion(
    val version: Long,
    val workAreas: List<WorkAreaId>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
