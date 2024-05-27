/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TaskStatusEnum
import java.util.UUID

class CreateTaskResource(
    val projectId: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val status: TaskStatusEnum? = null,
    val assigneeId: UUID? = null,
    val projectCraftId: UUID,
    val workAreaId: UUID? = null
)
