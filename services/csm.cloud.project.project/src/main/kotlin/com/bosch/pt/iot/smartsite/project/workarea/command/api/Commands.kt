/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.api

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId

data class CreateWorkAreaCommand(
    val identifier: WorkAreaId,
    val projectRef: ProjectId,
    val name: String,
    val position: Int?,
    var workAreaListVersion: Long,
    var parentRef: WorkAreaId? = null
)

data class UpdateWorkAreaCommand(
    val identifier: WorkAreaId,
    val version: Long,
    val name: String,
)

data class DeleteWorkAreaCommand(val identifier: WorkAreaId, val version: Long)
