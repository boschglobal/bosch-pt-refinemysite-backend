/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val PROJECT_CRAFT_PROJECTION = "ProjectCraftProjection"

@Document(PROJECT_CRAFT_PROJECTION)
@TypeAlias(PROJECT_CRAFT_PROJECTION)
data class ProjectCraft(
    @Id val identifier: ProjectCraftId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val color: String,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<ProjectCraftVersion>
)

data class ProjectCraftVersion(
    val version: Long,
    val name: String,
    val color: String,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
