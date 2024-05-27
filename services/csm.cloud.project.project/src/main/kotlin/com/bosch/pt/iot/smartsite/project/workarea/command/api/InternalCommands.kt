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
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId

data class AddWorkAreaToListCommand(
    val projectRef: ProjectId,
    val identifier: WorkAreaListId,
    val version: Long,
    val workAreaRef: WorkAreaId,
    val position: Int
)

data class CreateWorkAreaListCommand(val identifier: WorkAreaListId, val projectRef: ProjectId)

data class ReorderWorkAreaListCommand(
    val workAreaRef: WorkAreaId,
    val version: Long,
    val position: Int = 0
)

data class RemoveWorkAreaFromListCommand(
    val identifier: WorkAreaId,
    val workAreaListRef: WorkAreaListId,
    var version: Long
)
